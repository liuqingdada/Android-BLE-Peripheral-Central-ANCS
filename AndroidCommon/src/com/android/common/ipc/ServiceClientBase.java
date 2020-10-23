package com.android.common.ipc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.android.common.utils.LogUtil;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class ServiceClientBase<IService> implements ConnectStateListener, Handler.Callback {
    private static final String TAG = "ServiceClientBase";

    private static final int MSG_NEW_OPERATION = 1;
    private static final int MSG_PENDING_OPERATIONS = 2;
    private static final int MSG_AUTO_DISCONNECT = 3;

    protected Context mAppContext;
    protected ServiceConnector<IService> mServiceConnector;

    private String mServiceName;
    private Handler mOperationHandler;
    private Queue<IpcOperation<IService>> mPendingOperations = new LinkedList<>();
    private boolean mAutoDisconnect;
    private long mDelayToDisconnect;

    protected ServiceClientBase(
            @NonNull Context context,
            @NonNull String serviceName,
            @NonNull Looper workLooper,
            @NonNull ServiceConnector<IService> serviceConnector
    ) {
        mAppContext = context.getApplicationContext();
        mServiceName = serviceName;
        mOperationHandler = new Handler(workLooper, this);

        mServiceConnector = serviceConnector;
        mServiceConnector.addListener(this);
    }

    /**
     * Enable/disable "auto disconnect" feature
     *
     * @param autoDisconnect    Disconnect automatically if true
     * @param delayToDisconnect The delay time to disconnect if no operations, in milliseconds.
     */
    public void setAutoDisconnect(boolean autoDisconnect, long delayToDisconnect) {
        mAutoDisconnect = autoDisconnect;
        if (autoDisconnect) {
            mDelayToDisconnect = Math.max(delayToDisconnect, 0L);
        }
    }

    @NonNull
    public ServiceConnector<IService> getServiceConnector() {
        return mServiceConnector;
    }

    public void connect() {
        mServiceConnector.connect();
    }

    /**
     * Disconnect the Service connection. This may cause the pending operations to lost!
     */
    public void disconnect() {
        mServiceConnector.disconnect();
    }

    public void addOperation(IpcOperation<IService> operation) {
        if (mServiceConnector.getConnectState() == ServiceConnector.STATE_DISCONNECTED) {
            // try to connect if not connected or connecting
            // (such as the Service APK was installed after the previous connecting)
            // (such as autoDisconnect enabled)
            mServiceConnector.connect();
        }
        mOperationHandler.removeMessages(MSG_AUTO_DISCONNECT);
        Message.obtain(mOperationHandler, MSG_NEW_OPERATION, operation).sendToTarget();
    }

    @Override
    public void onStateChanged(int newState) {
        LogUtil.d(TAG, "[%s] Service connect state changed: %s",
                mServiceName, ServiceConnector.stateToStr(newState)
        );
        if (newState == ServiceConnector.STATE_CONNECTED) {
            mOperationHandler.removeMessages(MSG_AUTO_DISCONNECT);
            Message.obtain(mOperationHandler, MSG_PENDING_OPERATIONS).sendToTarget();
        }
    }

    @WorkerThread
    private void handleOperation(@NonNull IpcOperation<IService> operation) {
        IService service = mServiceConnector.getService();
        if (service != null) {
            try {
                operation.execute(service);
                LogUtil.d(TAG, "[%s] Succeeded to handle incoming operation: %s",
                        mServiceName, operation
                );
                return; // Success
            } catch (RemoteException e) {
                String errMsg = String.format(
                        Locale.US,
                        "[%s] Failed to handle incoming operation: %s", mServiceName, operation
                );
                LogUtil.w(TAG, errMsg, e);
                // add it into the queue again
            } catch (Exception e) {
                String errMsg = String.format(
                        Locale.US,
                        "[%s] Cannot execute incoming operation: %s. Discard it.",
                        mServiceName,
                        operation
                );
                LogUtil.e(TAG, errMsg, e);
                return; // discard the operation
            }
        }

        // Service not connected or failed to IPC
        LogUtil.d(TAG, "[%s] Added into pending queue: %s", mServiceName, operation);
        mPendingOperations.add(operation);
    }

    @WorkerThread
    private void handlePendingOperations() {
        LogUtil.d(TAG, "[%s] handlePendingOperations: %d", mServiceName, mPendingOperations.size());
        while (mServiceConnector.getService() != null) {
            IpcOperation<IService> operation = mPendingOperations.poll();
            if (operation == null) {
                break;
            }
            handleOperation(operation);
        }
        LogUtil.d(
                TAG,
                "[%s] handlePendingOperations done: %d",
                mServiceName,
                mPendingOperations.size()
        );
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_NEW_OPERATION: {
                @SuppressWarnings("unchecked")
                IpcOperation<IService> operation = (IpcOperation<IService>) msg.obj;
                handleOperation(operation);
                break;
            }

            case MSG_PENDING_OPERATIONS: {
                handlePendingOperations();
                break;
            }

            case MSG_AUTO_DISCONNECT: {
                LogUtil.d(TAG, "auto disconnect");
                mServiceConnector.disconnect();
                break;
            }
        }

        if (mAutoDisconnect && msg.what != MSG_AUTO_DISCONNECT) {
            mOperationHandler.removeMessages(MSG_AUTO_DISCONNECT);
            mOperationHandler.sendEmptyMessageDelayed(MSG_AUTO_DISCONNECT, mDelayToDisconnect);
        }

        return true;
    }
}

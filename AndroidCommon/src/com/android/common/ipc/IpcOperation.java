package com.android.common.ipc;

import android.os.RemoteException;

import androidx.annotation.NonNull;

public interface IpcOperation<IService> {
    void execute(@NonNull IService service) throws RemoteException;
}
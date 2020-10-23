package com.android.common.ipc;

public interface ConnectStateListener {
    void onStateChanged(@ServiceConnector.ConnectState int newState);
}

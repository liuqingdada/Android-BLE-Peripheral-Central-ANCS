package com.suhen.android.libble.nrfscan;

/**
 * Created by andy
 * 2019-06-12.
 * Email: 1239604859@qq.com
 */
public interface FastPairConstant {

    interface Action {
    }

    interface Extra {
        String SP_NAME = "xiamitv_service_fastpair";
        String NRF_SCAN_STATUS = "extra_NRF_SCAN_STATUS";
        String KEY_SCANNER_ID = "key_ble_central_scanner_id";
    }

    interface Source {
        String DEFAULT_TICPOD_ADDRESS = "00:00:00:00:00:00";
        long BLE_SCAN_REPORT_DELAY = 500;
    }
}

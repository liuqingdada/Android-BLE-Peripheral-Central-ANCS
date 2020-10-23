package com.suhen.android.libble.nrfscan;

/**
 * Created by andy
 * 2019-06-12.
 * Email: 1239604859@qq.com
 */
public interface FastPairConstant {

    interface Action {
        String ACTION_NEED_SHOW_POPUP = "com.mobvoi.ticpod.service.ACTION_NEED_SHOW_POPUP";
        String ACTION_GAIA_LIFE = "com.mobvoi.ticpod.service.ACTION_GAIA_LIFE";
        int STATE_RECREATE = 1024;
        String SERVICE_ACTION = "com.mobvoi.ticpod.ACTION.FAST_PAIR.SERVICE";
    }

    interface Extra {
        String SP_NAME = "ticpod_service_fastpair";
        String KEY_SCANNER_ID = "_key_scanner_id";
        String DEVICE_ID = "_DEVICE_ID";
        String NAME = "_NAME";
        String COLOR = "_COLOR";
        String OTA = "_OTA";
        String PAIR_ADDRESS = "_PAIR_ADDRESS";
        String FAST_PAIR_DEVICE = "fast_pair_device";
        String OTA_SINGLE = "_OTA_SINGLE";
        String TICPOD_API_1050 = "_TICPOD_API_1050";
        String SWITCH_IS_SHOW_DOT = "_SWITCH_IS_SHOW_DOT";
        String SWITCH_WENWEN_ASSISTANT = "com.mobvoi.ticpod.SWITCH_WENWEN_ASSISTANT";
        String NRF_SCAN_STATUS = "extra_NRF_SCAN_STATUS";

        String SP_OTA_HELPER = "vpa_sp_ticpod_ota_helper";
    }

    interface SOURCE {
        String DEFAULT_TICPOD_ADDRESS = "00:00:00:00:00:00";
        String UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb";
        /**
         * TICPOD 服务UUID
         */
        String UUID_MOBVOI_CONFIG_SERVICE = "00008868" + UUID_SUFFIX;

        long BLE_SCAN_REPORT_DELAY = 500;
    }
}

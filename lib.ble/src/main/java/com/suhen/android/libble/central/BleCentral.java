package com.suhen.android.libble.central;

import android.content.Context;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 */
public class BleCentral {
    private static final String TAG = BleCentral.class.getSimpleName();

    private static BleCentral sBleCentral;

    protected Context mContext;

    protected BleCentral(Context context){
        mContext = context;
    }

    public static synchronized BleCentral getInstance(Context context) {
        if (sBleCentral == null) {
            sBleCentral = new BleCentral(context);
        }
        return sBleCentral;
    }

}

package com.android.common.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.Locale;

public class LocaleUtils {
    /**
     * It can be used to set locale for Activity
     */
    public static Context createLocaleContext(@NonNull Context base, @NonNull Locale locale) {
        Configuration config = base.getResources().getConfiguration();
        config.setLocale(locale);
        return base.createConfigurationContext(config);
    }

    /**
     * Set locale for specified context.
     */
    public static void setLocaleForContext(@NonNull Context context, @NonNull Locale locale) {
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, res.getDisplayMetrics());
    }
}

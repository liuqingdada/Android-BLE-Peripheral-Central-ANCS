package com.android.common.utils;

import android.content.res.AssetManager;
import android.graphics.Typeface;

import androidx.collection.ArrayMap;

/**
 * avoid typeFace.createFromAsset() method memory leak
 *
 * @see <a href="https://issuetracker.google.com/issues/36919609">
 * https://issuetracker.google.com/issues/36919609</a>
 */
public class TypefaceUtils {
    private static final ArrayMap<String, Typeface> sTypefaceCache = new ArrayMap<>();

    public static Typeface createFromAsset(AssetManager mgr, String path) {
        Typeface typeface = sTypefaceCache.get(path);

        if (typeface != null) {
            return typeface;
        }
        synchronized (sTypefaceCache) {
            typeface = sTypefaceCache.get(path);
            if (typeface != null) {
                return typeface;
            }
            typeface = Typeface.createFromAsset(mgr, path);
            sTypefaceCache.put(path, typeface);
            return typeface;
        }
    }
}

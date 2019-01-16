package com.suhen.android.libble.message.utils;

/**
 * Created by liuqing
 * 2017/6/28.
 * Email: 1239604859@qq.com
 */

public class FilterStr {
    public static String filterNumber(String number) {
        number = number.replaceAll("[^(0-9)]", "");
        return number;
    }

    public static String filterAlphabet(String alph) {
        alph = alph.replaceAll("[^(A-Za-z)]", "");
        return alph;
    }

    public static String filter(String character) {
        character = character.replaceAll("[^(a-zA-Z0-9\\u4e00-\\u9fa5)]", "");
        return character;
    }

    // *************************************************************

    public static boolean filterSymbol(String str) {
        String regex = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~]";
        return str.matches(regex);
    }

    public static boolean filterNum(String str) {
        String regex = "[^(0-9)]";
        return str.matches(regex);
    }

    public static boolean filterAlpha(String str) {
        String regex = "[^(A-Za-z)]";
        return str.matches(regex);
    }
}

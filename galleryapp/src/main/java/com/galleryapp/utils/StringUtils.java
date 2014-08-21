package com.galleryapp.utils;

import com.galleryapp.Logger;

import java.util.Arrays;

/**
 * Created by pvg on 21.08.14.
 */
public class StringUtils {

    public static final String TAG = StringUtils.class.getSimpleName();

    private StringUtils() {
    }

    public static String[] parseIndexSchema(String indexString) {
        return indexString.split("&");
    }

    public static String[][] parseIndexElements(String indexString) {
        String[] indexes = parseIndexSchema(indexString);
        Logger.d(TAG, "indexes = " + Arrays.toString(indexes));
        int indexSize = indexes.length;
        String[][] elements = new String[indexSize][2];
        for (int i = 0; i < indexSize; i++) {
            String[] element = indexes[i].split("=");
            String name = "";
            String value = "";
            if (element.length > 0) {
                name = element[0];
            }
            if (element.length > 1) {
                value = element[1];
            }
            Logger.d(TAG, "[" + i + "]" + " name = " + name + " / value = " + value);
            elements[i][0] = name;
            elements[i][1] = value;
        }
        return elements;
    }
}

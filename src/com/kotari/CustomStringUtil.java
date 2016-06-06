package com.kotari;

import java.util.Vector;

/**
 * Created by fuad on 5/31/16.
 */
public class CustomStringUtil {
    public static String join(Vector<String> vec, String delim) {
        StringBuffer result = new StringBuffer();

        for (int i = 0; i < vec.size(); i++) {
            if (i != 0)
                result.append(delim);
            result.append(vec.get(i));
        }

        return result.toString();
    }
}

package com.kotari;

import javax.swing.*;

/**
 * Created by fuad on 6/6/16.
 */
public class CommonUtils {
    public static double try_double(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
        }
        return 0;
    }

    public static int try_int(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return 0;
    }
}

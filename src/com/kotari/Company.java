package com.kotari;

/**
 * Created by fuad on 6/6/16.
 */
public class Company {
    private static Company sCompany;

    static {
        sCompany = new Company();
    }

    public static Company getSingleton() {
        return sCompany;
    }

    public String name;
}

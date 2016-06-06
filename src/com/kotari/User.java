package com.kotari;

/**
 * Created by fuad on 6/6/16.
 */
public class User {

    public static final int ROLE_ROOT = 0;
    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_SUPERVISOR = 2;
    public static final int ROLE_READER = 3;

    public static String GetRoleName(int role) {
        switch (role) {
            case ROLE_ROOT: return "ROOT";
            case ROLE_ADMIN: return "Administrator";
            case ROLE_SUPERVISOR: return "Supervisor";
            case ROLE_READER: default: return "Reader";
        }
    }

    private static User sUser;

    static {
        sUser = new User();
    }

    public static User getSingleton() {
        return sUser;
    }

    public int id;
    public String username;
    public String hash;
    public int role;

    public static String hashPassword(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if       (c >= 'a' && c <= 'm') c += 13;
            else if  (c >= 'A' && c <= 'M') c += 13;
            else if  (c >= 'n' && c <= 'z') c -= 13;
            else if  (c >= 'N' && c <= 'Z') c -= 13;
            sb.append(c);
        }
        return sb.toString();
    }

    public static boolean doesHashMatch(String new_pass, String stored_hash) {
        return hashPassword(new_pass).equals(stored_hash);
    }
}

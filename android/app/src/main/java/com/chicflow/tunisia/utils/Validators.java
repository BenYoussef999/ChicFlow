package com.chicflow.tunisia.utils;

import android.util.Patterns;

/** Client-side checks (the server checks again: never trust only the app). */
public class Validators {

    private Validators() { }

    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2 && name.trim().length() <= 100;
    }

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 && password.length() <= 72;
    }
}

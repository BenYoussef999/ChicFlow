package com.chicflow.tunisia.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Remembers who is logged in (token, id, name, role) using SharedPreferences.
 * Used from stage 2 onward.
 */
public class SessionManager {

    private static final String PREFS = "chicflow_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, int userId, String name, String role) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_NAME, name)
                .putString(KEY_ROLE, role)
                .apply();
    }

    /** Saves the "data" part of a login/register answer: { token, user: {id, name, role...} } */
    public void saveFromAuthResponse(JSONObject data) {
        JSONObject user = data.optJSONObject("user");
        if (user == null) {
            user = new JSONObject();
        }
        saveSession(data.optString("token"), user.optInt("id", -1),
                user.optString("name"), user.optString("role", "user"));
    }

    public String getToken()  { return prefs.getString(KEY_TOKEN, null); }
    public int getUserId()    { return prefs.getInt(KEY_USER_ID, -1); }
    public String getName()   { return prefs.getString(KEY_NAME, ""); }
    public String getRole()   { return prefs.getString(KEY_ROLE, "user"); }

    public boolean isLoggedIn() { return getToken() != null; }
    public boolean isAdmin()    { return "admin".equals(getRole()); }

    public void logout() {
        prefs.edit().clear().apply();
    }
}

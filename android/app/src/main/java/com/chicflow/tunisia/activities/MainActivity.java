package com.chicflow.tunisia.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.network.ApiClient;
import com.chicflow.tunisia.network.ApiConfig;
import com.chicflow.tunisia.utils.SessionManager;

import org.json.JSONObject;

/**
 * STAGE 2 PLACEHOLDER dashboard: proves that login works and that the token is accepted
 * by the server (it loads the profile). Real navigation (wardrobe, budget...) comes later.
 */
public class MainActivity extends AppCompatActivity {

    private SessionManager session;
    private TextView tvWelcome, tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvInfo = findViewById(R.id.tvInfo);

        tvWelcome.setText(getString(R.string.welcome_user, session.getName()));
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        loadProfile();
    }

    private void loadProfile() {
        tvInfo.setText(R.string.loading);
        ApiClient.getInstance(this).get(ApiConfig.PROFILE, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                JSONObject user = data.optJSONObject("user");
                if (user == null) return;
                String style = user.optString("style_preference", "");
                if (style.isEmpty() || "null".equals(style)) {
                    style = getString(R.string.style_none);
                }
                tvInfo.setText(getString(R.string.profile_info,
                        user.optString("email"), user.optString("role"), style));
            }

            @Override
            public void onError(String message, int httpStatus) {
                if (httpStatus == 401) {          // token expired or revoked
                    session.logout();
                    goToLogin();
                } else {
                    tvInfo.setText(message);
                }
            }
        });
    }

    private void logout() {
        // Tell the server to delete the token, then clear it on the phone.
        // We clear in both cases (success or failure) so the user is never stuck.
        ApiClient.getInstance(this).post(ApiConfig.LOGOUT, new JSONObject(), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                finishLogout();
            }

            @Override
            public void onError(String message, int httpStatus) {
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        session.logout();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

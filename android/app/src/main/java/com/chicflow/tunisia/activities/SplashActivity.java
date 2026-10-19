package com.chicflow.tunisia.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.utils.SessionManager;

/** Shows the logo for 1.5 seconds, then opens Main (if logged in) or Login. */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean loggedIn = new SessionManager(this).isLoggedIn();
            Class<?> next = loggedIn ? MainActivity.class : LoginActivity.class;
            startActivity(new Intent(SplashActivity.this, next));
            finish();
        }, SPLASH_DELAY_MS);
    }
}

package com.chicflow.tunisia.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.chicflow.tunisia.R;

/** Shows the logo for 1.5 seconds, then opens the next screen. */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Stage 2: if SessionManager.isLoggedIn() -> open MainActivity instead.
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, SPLASH_DELAY_MS);
    }
}

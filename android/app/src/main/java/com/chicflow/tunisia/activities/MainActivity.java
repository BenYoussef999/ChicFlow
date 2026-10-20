package com.chicflow.tunisia.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.fragments.HomeFragment;
import com.chicflow.tunisia.fragments.PlaceholderFragment;
import com.chicflow.tunisia.fragments.WardrobeFragment;
import com.chicflow.tunisia.network.ApiClient;
import com.chicflow.tunisia.network.ApiConfig;
import com.chicflow.tunisia.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

/** Main screen: a bottom navigation bar that swaps fragments (one per tab). */
public class MainActivity extends AppCompatActivity {

    private SessionManager session;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);   // triggers the listener -> shows Home
        }

        checkSessionStillValid();
    }

    /** Called by fragments (for example the shortcut cards on Home). */
    public void selectTab(int tabId) {
        bottomNav.setSelectedItemId(tabId);
    }

    private void showTab(int tabId) {
        Fragment fragment;
        // NOTE: if/else instead of switch, because R.id values are not constants in recent Android Gradle versions
        if (tabId == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (tabId == R.id.nav_wardrobe) {
            fragment = new WardrobeFragment();
        } else if (tabId == R.id.nav_outfits) {
            fragment = PlaceholderFragment.newInstance(
                    getString(R.string.home_card_outfit), getString(R.string.placeholder_text));
        } else if (tabId == R.id.nav_budget) {
            fragment = PlaceholderFragment.newInstance(
                    getString(R.string.home_card_budget), getString(R.string.placeholder_text));
        } else {
            fragment = PlaceholderFragment.newInstance(
                    getString(R.string.home_card_week), getString(R.string.placeholder_text));
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /** Asks the server if our token is still valid. Only a 401 logs the user out. */
    private void checkSessionStillValid() {
        ApiClient.getInstance(this).get(ApiConfig.PROFILE, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                // token is fine
            }

            @Override
            public void onError(String message, int httpStatus) {
                if (httpStatus == 401) {
                    session.logout();
                    goToLogin();
                }
                // other errors (no network...) are ignored: the app stays usable
            }
        });
    }

    public void logout() {
        // Tell the server to delete the token, then clear it on the phone.
        // We clear in both cases so the user is never stuck.
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

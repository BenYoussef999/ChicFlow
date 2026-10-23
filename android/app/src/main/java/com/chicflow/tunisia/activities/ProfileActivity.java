package com.chicflow.tunisia.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.network.ApiClient;
import com.chicflow.tunisia.network.ApiConfig;
import com.chicflow.tunisia.utils.SessionManager;
import com.chicflow.tunisia.utils.Validators;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/** Shows the profile from the server and lets the user change name and style preference. */
public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private String noPreferenceLabel;

    private TextInputLayout tilName;
    private TextInputEditText etName;
    private MaterialAutoCompleteTextView actStyle;
    private TextView tvEmail, tvRole, tvError;
    private MaterialButton btnSave;
    private CircularProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);
        noPreferenceLabel = getString(R.string.option_no_preference);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilName = findViewById(R.id.tilName);
        etName = findViewById(R.id.etName);
        actStyle = findViewById(R.id.actStyle);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);
        tvError = findViewById(R.id.tvError);
        btnSave = findViewById(R.id.btnSave);
        progress = findViewById(R.id.progress);

        // Dropdown: "No preference" + the five styles
        String[] styles = getResources().getStringArray(R.array.style_options);
        String[] options = new String[styles.length + 1];
        options[0] = noPreferenceLabel;
        System.arraycopy(styles, 0, options, 1, styles.length);
        actStyle.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, options));
        actStyle.setText(noPreferenceLabel, false);

        btnSave.setOnClickListener(v -> save());
        loadProfile();
    }

    private void loadProfile() {
        setLoading(true);
        ApiClient.getInstance(this).get(ApiConfig.PROFILE, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                setLoading(false);
                JSONObject user = data.optJSONObject("user");
                if (user != null) showUser(user);
            }

            @Override
            public void onError(String message, int httpStatus) {
                setLoading(false);
                if (httpStatus == 401) {          // token expired: back to login
                    session.logout();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }
                showError(message);
            }
        });
    }

    private void showUser(JSONObject user) {
        etName.setText(user.optString("name"));
        tvEmail.setText(user.optString("email"));
        tvRole.setText("admin".equals(user.optString("role"))
                ? R.string.role_admin : R.string.role_member);

        // JSON null must be tested with isNull(): optString would return the text "null"
        String style = user.isNull("style_preference") ? "" : user.optString("style_preference", "");
        actStyle.setText(style.isEmpty() ? noPreferenceLabel : capitalize(style), false);
    }

    private void save() {
        tilName.setError(null);
        tvError.setVisibility(View.GONE);

        String name = String.valueOf(etName.getText()).trim();
        if (!Validators.isValidName(name)) {
            tilName.setError(getString(R.string.err_name));
            return;
        }

        String chosen = String.valueOf(actStyle.getText()).trim();
        String style = noPreferenceLabel.equals(chosen) ? "" : chosen.toLowerCase(Locale.ROOT);

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("style_preference", style);
        } catch (JSONException e) {
            return;
        }

        setLoading(true);
        ApiClient.getInstance(this).post(ApiConfig.PROFILE, body, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                setLoading(false);
                JSONObject user = data.optJSONObject("user");
                if (user != null) {
                    showUser(user);
                    session.updateName(user.optString("name", name));
                }
                Toast.makeText(ProfileActivity.this, R.string.toast_profile_saved,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message, int httpStatus) {
                setLoading(false);
                showError(message);
            }
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    private static String capitalize(String text) {
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}

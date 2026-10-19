package com.chicflow.tunisia.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.network.ApiClient;
import com.chicflow.tunisia.network.ApiConfig;
import com.chicflow.tunisia.utils.SessionManager;
import com.chicflow.tunisia.utils.Validators;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private MaterialAutoCompleteTextView actStyle;
    private MaterialButton btnRegister;
    private CircularProgressIndicator progress;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirm = findViewById(R.id.tilConfirm);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        actStyle = findViewById(R.id.actStyle);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);

        // Style dropdown (optional field)
        String[] styles = getResources().getStringArray(R.array.style_options);
        actStyle.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, styles));

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tvGoLogin).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);
        tvError.setVisibility(View.GONE);

        String name = String.valueOf(etName.getText()).trim();
        String email = String.valueOf(etEmail.getText()).trim();
        String password = String.valueOf(etPassword.getText());
        String confirm = String.valueOf(etConfirm.getText());
        // Display labels are English words; the server expects them in lowercase
        String style = String.valueOf(actStyle.getText()).trim().toLowerCase(Locale.ROOT);

        boolean valid = true;
        if (!Validators.isValidName(name)) {
            tilName.setError(getString(R.string.err_name));
            valid = false;
        }
        if (!Validators.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.err_email));
            valid = false;
        }
        if (!Validators.isValidPassword(password)) {
            tilPassword.setError(getString(R.string.err_password));
            valid = false;
        }
        if (!password.equals(confirm)) {
            tilConfirm.setError(getString(R.string.err_password_match));
            valid = false;
        }
        if (!valid) return;

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("email", email);
            body.put("password", password);
            body.put("style_preference", style);   // "" means "no preference"
        } catch (JSONException e) {
            return;
        }

        setLoading(true);
        ApiClient.getInstance(this).post(ApiConfig.REGISTER, body, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                // The server logs us in right away
                new SessionManager(RegisterActivity.this).saveFromAuthResponse(data);
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(String message, int httpStatus) {
                setLoading(false);
                tvError.setText(message);
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}

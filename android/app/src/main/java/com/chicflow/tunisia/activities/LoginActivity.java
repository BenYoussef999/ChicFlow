package com.chicflow.tunisia.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.network.ApiClient;
import com.chicflow.tunisia.network.ApiConfig;

import org.json.JSONObject;

import com.google.android.material.button.MaterialButton;

/**
 * STAGE 1 PLACEHOLDER: only a "Test connection" button that calls ping.php.
 * In stage 2 this screen becomes the real login form.
 */
public class LoginActivity extends AppCompatActivity {

    private TextView tvStatus;
    private MaterialButton btnTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tvStatus = findViewById(R.id.tvStatus);
        btnTest = findViewById(R.id.btnTestConnection);

        btnTest.setOnClickListener(v -> testConnection());
    }

    private void testConnection() {
        btnTest.setEnabled(false);
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.cf_brown));
        tvStatus.setText(R.string.status_testing);

        ApiClient.getInstance(this).get(ApiConfig.PING, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject data) {
                btnTest.setEnabled(true);
                boolean schemaOk = data.optBoolean("schema_ok", false);
                tvStatus.setTextColor(ContextCompat.getColor(LoginActivity.this,
                        schemaOk ? R.color.cf_success : R.color.cf_error));
                tvStatus.setText("Server reachable\n"
                        + "PHP " + data.optString("php_version") + "\n"
                        + "Database: " + data.optString("database") + "\n"
                        + "Tables found: " + data.optInt("table_count") + " / 9\n"
                        + (schemaOk ? "Schema OK" : "Schema incomplete: import schema.sql"));
            }

            @Override
            public void onError(String message, int httpStatus) {
                btnTest.setEnabled(true);
                tvStatus.setTextColor(ContextCompat.getColor(LoginActivity.this, R.color.cf_error));
                tvStatus.setText("Failed: " + message
                        + (httpStatus > 0 ? "\n(HTTP " + httpStatus + ")" : ""));
            }
        });
    }
}

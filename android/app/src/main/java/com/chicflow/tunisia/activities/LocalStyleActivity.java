package com.chicflow.tunisia.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.adapters.BrandAdapter;
import com.chicflow.tunisia.models.Brand;
import com.chicflow.tunisia.repository.BrandRepository;
import com.chicflow.tunisia.utils.SessionManager;
import com.chicflow.tunisia.utils.UrlUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/**
 * Local Style: catalog of Tunisian brands. Everybody can read it and open the websites
 * (implicit intent). Only an administrator sees the buttons to add or remove a brand.
 */
public class LocalStyleActivity extends AppCompatActivity {

    private final BrandRepository repo = BrandRepository.getInstance();

    private boolean admin;
    private BrandAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_style);

        admin = new SessionManager(this).isAdmin();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText(admin ? R.string.empty_brands_admin : R.string.empty_brands_user);

        RecyclerView rv = findViewById(R.id.rvBrands);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BrandAdapter(admin, new BrandAdapter.OnActionListener() {
            @Override
            public void onVisit(Brand brand) {
                openWebsite(brand);
            }

            @Override
            public void onDelete(Brand brand) {
                confirmDelete(brand);
            }
        });
        rv.setAdapter(adapter);

        View fab = findViewById(R.id.fabAddBrand);
        fab.setVisibility(admin ? View.VISIBLE : View.GONE);
        fab.setOnClickListener(v -> showBrandDialog());

        refresh();
    }

    private void refresh() {
        List<Brand> brands = repo.getAll();
        adapter.setItems(brands);
        tvEmpty.setVisibility(brands.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Implicit intent: Android finds a browser to open the address. */
    private void openWebsite(Brand brand) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(brand.getWebsiteUrl())));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    private void showBrandDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_brand, null);
        TextInputLayout tilName = view.findViewById(R.id.tilBrandName);
        TextInputLayout tilCategory = view.findViewById(R.id.tilBrandCategory);
        TextInputLayout tilUrl = view.findViewById(R.id.tilBrandUrl);
        TextInputEditText etName = view.findViewById(R.id.etBrandName);
        TextInputEditText etCategory = view.findViewById(R.id.etBrandCategory);
        TextInputEditText etPrice = view.findViewById(R.id.etBrandPrice);
        TextInputEditText etUrl = view.findViewById(R.id.etBrandUrl);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_brand_title)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null)     // set below to keep the dialog open on errors
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            tilName.setError(null);
            tilCategory.setError(null);
            tilUrl.setError(null);

            String name = String.valueOf(etName.getText()).trim();
            String category = String.valueOf(etCategory.getText()).trim();
            String price = String.valueOf(etPrice.getText()).trim();
            String url = UrlUtils.normalize(String.valueOf(etUrl.getText()));

            boolean valid = true;
            if (name.length() < 2) {
                tilName.setError(getString(R.string.err_brand_name));
                valid = false;
            }
            if (category.length() < 2) {
                tilCategory.setError(getString(R.string.err_brand_category));
                valid = false;
            }
            if (url == null) {
                tilUrl.setError(getString(R.string.err_brand_url));
                valid = false;
            }
            if (!valid) return;

            repo.add(new Brand(0, name, category, price, url));
            dialog.dismiss();
            Toast.makeText(this, R.string.toast_brand_added, Toast.LENGTH_SHORT).show();
            refresh();
        });
    }

    private void confirmDelete(Brand brand) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_brand_title)
                .setMessage(brand.getName())
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    repo.delete(brand.getId());
                    Toast.makeText(this, R.string.toast_brand_deleted, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .show();
    }
}

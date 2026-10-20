package com.chicflow.tunisia.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.repository.WardrobeRepository;
import com.chicflow.tunisia.utils.ClothingOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Add a new clothing item, or edit/delete an existing one (when EXTRA_ITEM_ID is given). */
public class ItemFormActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "item_id";

    private WardrobeRepository repo;
    private ClothingItem editing;        // null = we are adding a new item
    private String dateValue;            // yyyy-MM-dd

    private TextInputLayout tilName, tilCategory, tilColor, tilPrice;
    private TextInputEditText etName, etPrice, etDate;
    private MaterialAutoCompleteTextView actCategory, actColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_form);

        repo = WardrobeRepository.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilName = findViewById(R.id.tilName);
        tilCategory = findViewById(R.id.tilCategory);
        tilColor = findViewById(R.id.tilColor);
        tilPrice = findViewById(R.id.tilPrice);
        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etDate = findViewById(R.id.etDate);
        actCategory = findViewById(R.id.actCategory);
        actColor = findViewById(R.id.actColor);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        MaterialButton btnDelete = findViewById(R.id.btnDelete);

        actCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ClothingOptions.CATEGORY_LABELS));
        actColor.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ClothingOptions.COLOR_NAMES));

        int id = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);
        editing = id == -1 ? null : repo.getById(id);

        if (editing != null) {
            toolbar.setTitle(R.string.title_edit_item);
            etName.setText(editing.getName());
            actCategory.setText(ClothingOptions.categoryLabel(editing.getCategory()), false);
            actColor.setText(editing.getColor(), false);
            etPrice.setText(String.valueOf(editing.getPrice()));
            dateValue = editing.getDateAdded();
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            toolbar.setTitle(R.string.title_add_item);
            dateValue = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            btnDelete.setVisibility(View.GONE);
        }
        etDate.setText(dateValue);

        etDate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> save());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void showDatePicker() {
        // The picker works in UTC milliseconds, so we format/parse in UTC too
        SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));

        long selection = MaterialDatePicker.todayInUtcMilliseconds();
        try {
            Date current = utc.parse(dateValue);
            if (current != null) selection = current.getTime();
        } catch (ParseException ignored) {
            // keep today
        }

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.date_picker_title)
                .setSelection(selection)
                .build();
        picker.addOnPositiveButtonClickListener(millis -> {
            dateValue = utc.format(new Date(millis));
            etDate.setText(dateValue);
        });
        picker.show(getSupportFragmentManager(), "datePicker");
    }

    private void save() {
        tilName.setError(null);
        tilCategory.setError(null);
        tilColor.setError(null);
        tilPrice.setError(null);

        String name = String.valueOf(etName.getText()).trim();
        String categoryKey = ClothingOptions.categoryKeyForLabel(
                String.valueOf(actCategory.getText()).trim());
        String color = String.valueOf(actColor.getText()).trim();
        String priceText = String.valueOf(etPrice.getText()).trim().replace(',', '.');

        boolean valid = true;
        if (name.length() < 2) {
            tilName.setError(getString(R.string.err_item_name));
            valid = false;
        }
        if (categoryKey == null) {
            tilCategory.setError(getString(R.string.err_category));
            valid = false;
        }
        if (!ClothingOptions.isValidColor(color)) {
            tilColor.setError(getString(R.string.err_color));
            valid = false;
        }
        double price = 0;
        try {
            price = Double.parseDouble(priceText);
            if (price < 0 || price > 999999) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            tilPrice.setError(getString(R.string.err_price));
            valid = false;
        }
        if (!valid) return;

        if (editing != null) {
            editing.setName(name);
            editing.setCategory(categoryKey);
            editing.setColor(color);
            editing.setPrice(price);
            editing.setDateAdded(dateValue);
            repo.update(editing);
        } else {
            repo.add(new ClothingItem(0, name, categoryKey, color, price, null, dateValue));
        }

        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    repo.delete(editing.getId());
                    Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }
}

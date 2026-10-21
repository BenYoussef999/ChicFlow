package com.chicflow.tunisia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.adapters.PurchaseAdapter;
import com.chicflow.tunisia.models.Purchase;
import com.chicflow.tunisia.repository.BudgetRepository;
import com.chicflow.tunisia.utils.Format;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fashion Budget tab: month switcher, monthly budget, ProgressBar of the percentage used,
 * and the list of purchases. Spent / remaining / percentage are CALCULATED, never typed in.
 */
public class BudgetFragment extends Fragment {

    private interface DateCallback {
        void onDate(String date);
    }

    private final BudgetRepository repo = BudgetRepository.getInstance();
    private final Calendar cal = Calendar.getInstance();     // always the 1st of the shown month

    private TextView tvMonth, tvBudgetLine, tvPercent, tvSpent, tvRemaining, tvRemainingLabel, tvEmpty;
    private MaterialButton btnBudget;
    private LinearProgressIndicator progress;
    private PurchaseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        tvMonth = view.findViewById(R.id.tvMonth);
        tvBudgetLine = view.findViewById(R.id.tvBudgetLine);
        tvPercent = view.findViewById(R.id.tvPercent);
        tvSpent = view.findViewById(R.id.tvSpent);
        tvRemaining = view.findViewById(R.id.tvRemaining);
        tvRemainingLabel = view.findViewById(R.id.tvRemainingLabel);
        tvEmpty = view.findViewById(R.id.tvEmptyPurchases);
        btnBudget = view.findViewById(R.id.btnBudget);
        progress = view.findViewById(R.id.progressBudget);

        RecyclerView rv = view.findViewById(R.id.rvPurchases);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PurchaseAdapter(purchase -> confirmDelete(purchase));
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> changeMonth(-1));
        view.findViewById(R.id.btnNextMonth).setOnClickListener(v -> changeMonth(1));
        btnBudget.setOnClickListener(v -> showBudgetDialog());
        view.findViewById(R.id.fabAddPurchase).setOnClickListener(v -> showPurchaseDialog());

        render();
    }

    private String monthKey() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(cal.getTime());
    }

    private void changeMonth(int delta) {
        cal.add(Calendar.MONTH, delta);
        render();
    }

    /** Recalculates and shows everything for the selected month. */
    private void render() {
        String month = monthKey();
        tvMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.getTime()));

        Double budget = repo.getBudget(month);
        double spent = repo.totalSpent(month);
        tvSpent.setText(Format.tnd(spent));

        if (budget == null) {
            tvBudgetLine.setText(R.string.budget_not_set);
            btnBudget.setText(R.string.btn_set_budget);
            tvPercent.setText("\u2014");
            progress.setProgressCompat(0, false);
            tvRemainingLabel.setText(R.string.label_remaining);
            tvRemaining.setText("\u2014");
            tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.cf_brown));
        } else {
            tvBudgetLine.setText(getString(R.string.budget_monthly, Format.tnd(budget)));
            btnBudget.setText(R.string.btn_edit_budget);

            double percent = spent / budget * 100.0;           // 180 / 300 -> 60
            int shown = (int) Math.round(percent);
            tvPercent.setText(shown + "%");
            progress.setProgressCompat(Math.min(100, shown), false);

            double remaining = budget - spent;                  // 300 - 180 -> 120
            boolean over = remaining < 0;
            tvRemainingLabel.setText(over ? R.string.label_over_budget : R.string.label_remaining);
            tvRemaining.setText(Format.tnd(Math.abs(remaining)));

            int color = ContextCompat.getColor(requireContext(),
                    over ? R.color.cf_error : R.color.cf_burgundy);
            progress.setIndicatorColor(color);
            tvRemaining.setTextColor(ContextCompat.getColor(requireContext(),
                    over ? R.color.cf_error : R.color.cf_brown));
        }

        List<Purchase> purchases = repo.purchasesForMonth(month);
        adapter.setItems(purchases);
        tvEmpty.setVisibility(purchases.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------- Budget dialog ----------

    private void showBudgetDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_set_budget, null);
        TextInputLayout til = view.findViewById(R.id.tilBudgetAmount);
        TextInputEditText et = view.findViewById(R.id.etBudgetAmount);

        Double current = repo.getBudget(monthKey());
        if (current != null) {
            et.setText(String.valueOf(current));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_budget_title)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null)     // set below so we can keep the dialog open on errors
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Double amount = parseAmount(String.valueOf(et.getText()));
            if (amount == null) {
                til.setError(getString(R.string.err_budget));
                return;
            }
            repo.setBudget(monthKey(), amount);
            dialog.dismiss();
            Toast.makeText(requireContext(), R.string.toast_budget_saved, Toast.LENGTH_SHORT).show();
            render();
        });
    }

    // ---------- Purchase dialog ----------

    private void showPurchaseDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_purchase, null);
        TextInputLayout tilDesc = view.findViewById(R.id.tilPurchaseDesc);
        TextInputLayout tilAmount = view.findViewById(R.id.tilPurchaseAmount);
        TextInputEditText etDesc = view.findViewById(R.id.etPurchaseDesc);
        TextInputEditText etAmount = view.findViewById(R.id.etPurchaseAmount);
        TextInputEditText etDate = view.findViewById(R.id.etPurchaseDate);

        final String[] date = {defaultPurchaseDate()};
        etDate.setText(date[0]);
        etDate.setOnClickListener(v -> pickDate(date[0], picked -> {
            date[0] = picked;
            etDate.setText(picked);
        }));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_purchase_title)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            tilDesc.setError(null);
            tilAmount.setError(null);

            String description = String.valueOf(etDesc.getText()).trim();
            Double amount = parseAmount(String.valueOf(etAmount.getText()));

            boolean valid = true;
            if (description.length() < 2) {
                tilDesc.setError(getString(R.string.err_purchase_desc));
                valid = false;
            }
            if (amount == null) {
                tilAmount.setError(getString(R.string.err_budget));
                valid = false;
            }
            if (!valid) return;

            repo.addPurchase(new Purchase(0, description, amount, date[0]));
            dialog.dismiss();
            Toast.makeText(requireContext(), R.string.toast_purchase_added, Toast.LENGTH_SHORT).show();

            // Show the month of the new purchase (the date picker may have chosen another month)
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.YEAR, Integer.parseInt(date[0].substring(0, 4)));
            cal.set(Calendar.MONTH, Integer.parseInt(date[0].substring(5, 7)) - 1);
            render();
        });
    }

    /** Today if we are looking at the current month, otherwise the 1st of the shown month. */
    private String defaultPurchaseDate() {
        SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = day.format(new Date());
        return today.startsWith(monthKey()) ? today : day.format(cal.getTime());
    }

    private void pickDate(String current, DateCallback callback) {
        SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));

        long selection = MaterialDatePicker.todayInUtcMilliseconds();
        try {
            Date parsed = utc.parse(current);
            if (parsed != null) selection = parsed.getTime();
        } catch (ParseException ignored) {
            // keep today
        }

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.hint_purchase_date)
                .setSelection(selection)
                .build();
        picker.addOnPositiveButtonClickListener(millis -> callback.onDate(utc.format(new Date(millis))));
        picker.show(getChildFragmentManager(), "purchaseDate");
    }

    private void confirmDelete(Purchase purchase) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_purchase_title)
                .setMessage(purchase.getDescription())
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    repo.deletePurchase(purchase.getId());
                    Toast.makeText(requireContext(), R.string.toast_purchase_deleted,
                            Toast.LENGTH_SHORT).show();
                    render();
                })
                .show();
    }

    /** Returns the amount if it is a number between 0 (exclusive) and 999999, otherwise null. */
    private Double parseAmount(String text) {
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.'));
            return (value > 0 && value <= 999999) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

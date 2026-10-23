package com.chicflow.tunisia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.activities.LocalStyleActivity;
import com.chicflow.tunisia.activities.MainActivity;
import com.chicflow.tunisia.activities.ProfileActivity;
import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.models.Outfit;
import com.chicflow.tunisia.repository.BudgetRepository;
import com.chicflow.tunisia.repository.OutfitRepository;
import com.chicflow.tunisia.repository.WardrobeRepository;
import com.chicflow.tunisia.repository.WeekRepository;
import com.chicflow.tunisia.utils.BudgetMath;
import com.chicflow.tunisia.utils.ClothingOptions;
import com.chicflow.tunisia.utils.Format;
import com.chicflow.tunisia.utils.SessionManager;
import com.chicflow.tunisia.utils.WeekDays;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Dashboard: greeting, today's outfit, budget summary and shortcuts to the other screens. */
public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvItemCount, tvTodayOutfit, tvTodayPieces;
    private TextView tvBudgetLine, tvBudgetPercent;
    private LinearProgressIndicator progressBudget;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvItemCount = view.findViewById(R.id.tvItemCount);
        tvTodayOutfit = view.findViewById(R.id.tvTodayOutfit);
        tvTodayPieces = view.findViewById(R.id.tvTodayPieces);
        tvBudgetLine = view.findViewById(R.id.tvHomeBudgetLine);
        tvBudgetPercent = view.findViewById(R.id.tvHomeBudgetPercent);
        progressBudget = view.findViewById(R.id.progressHomeBudget);

        view.findViewById(R.id.cardToday).setOnClickListener(v -> goTo(R.id.nav_week));
        view.findViewById(R.id.cardHomeBudget).setOnClickListener(v -> goTo(R.id.nav_budget));
        view.findViewById(R.id.cardWardrobe).setOnClickListener(v -> goTo(R.id.nav_wardrobe));
        view.findViewById(R.id.cardOutfit).setOnClickListener(v -> goTo(R.id.nav_outfits));
        view.findViewById(R.id.cardBudget).setOnClickListener(v -> goTo(R.id.nav_budget));
        view.findViewById(R.id.cardWeek).setOnClickListener(v -> goTo(R.id.nav_week));
        view.findViewById(R.id.cardLocal).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LocalStyleActivity.class)));
        view.findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));
        view.findViewById(R.id.btnLogout).setOnClickListener(v ->
                ((MainActivity) requireActivity()).logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();      // also runs when coming back from Profile, Week or Budget
    }

    private void refresh() {
        tvGreeting.setText(getString(R.string.welcome_user,
                new SessionManager(requireContext()).getName()));

        int count = WardrobeRepository.getInstance().count();
        tvItemCount.setText(getResources().getQuantityString(R.plurals.items_count, count, count));

        // Today's planned outfit
        Outfit today = WeekRepository.getInstance().getPlannedOutfit(WeekDays.todayIso());
        if (today == null) {
            tvTodayOutfit.setText(R.string.home_today_none);
            tvTodayPieces.setText(R.string.home_today_tap);
        } else {
            tvTodayOutfit.setText(today.getName());
            StringBuilder emojis = new StringBuilder();
            List<ClothingItem> items = OutfitRepository.getInstance().itemsOf(today);
            for (ClothingItem item : items) {
                emojis.append(ClothingOptions.categoryEmoji(item.getCategory())).append("  ");
            }
            tvTodayPieces.setText(emojis.toString().trim());
        }

        // Budget of the current month
        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        BudgetRepository budgets = BudgetRepository.getInstance();
        Double budget = budgets.getBudget(month);
        double spent = budgets.totalSpent(month);

        if (budget == null) {
            tvBudgetLine.setText(R.string.home_budget_none);
            tvBudgetPercent.setText("");
            progressBudget.setProgressCompat(0, false);
        } else {
            int percent = BudgetMath.percent(spent, budget);
            double remaining = budget - spent;
            boolean over = remaining < 0;

            tvBudgetLine.setText(over
                    ? getString(R.string.home_budget_over, Format.tnd(-remaining), Format.tnd(budget))
                    : getString(R.string.home_budget_left, Format.tnd(remaining), Format.tnd(budget)));
            tvBudgetPercent.setText(getString(R.string.home_budget_percent, percent));
            progressBudget.setProgressCompat(Math.min(100, percent), false);
            progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(),
                    over ? R.color.cf_error : R.color.cf_burgundy));
        }
    }

    private void goTo(int tabId) {
        ((MainActivity) requireActivity()).selectTab(tabId);
    }
}

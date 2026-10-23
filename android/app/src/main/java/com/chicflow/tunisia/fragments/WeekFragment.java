package com.chicflow.tunisia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.activities.MainActivity;
import com.chicflow.tunisia.adapters.WeekDayAdapter;
import com.chicflow.tunisia.models.DayPlan;
import com.chicflow.tunisia.models.Outfit;
import com.chicflow.tunisia.repository.OutfitRepository;
import com.chicflow.tunisia.repository.WeekRepository;
import com.chicflow.tunisia.utils.Occasions;
import com.chicflow.tunisia.utils.WeekDays;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** My Week tab: assign a saved outfit to each day, change it or remove it. */
public class WeekFragment extends Fragment {

    private final WeekRepository weekRepo = WeekRepository.getInstance();
    private final OutfitRepository outfitRepo = OutfitRepository.getInstance();

    private WeekDayAdapter adapter;
    private TextView tvSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_week, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSummary = view.findViewById(R.id.tvWeekSummary);

        RecyclerView rv = view.findViewById(R.id.rvWeek);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new WeekDayAdapter(WeekDays.todayIso(), plan -> onDayClicked(plan));
        rv.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<DayPlan> days = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            Outfit outfit = weekRepo.getPlannedOutfit(day);
            days.add(new DayPlan(day, outfit,
                    outfit == null ? new ArrayList<>() : outfitRepo.itemsOf(outfit)));
        }
        adapter.setItems(days);
        tvSummary.setText(getString(R.string.week_summary, weekRepo.plannedCount()));
    }

    private void onDayClicked(DayPlan plan) {
        if (plan.isPlanned()) {
            showDayOptions(plan.getDay());
        } else {
            showOutfitChooser(plan.getDay());
        }
    }

    private void showDayOptions(int day) {
        String[] options = {getString(R.string.week_change), getString(R.string.week_remove)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(WeekDays.name(day))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showOutfitChooser(day);
                    } else {
                        weekRepo.removePlan(day);
                        Toast.makeText(requireContext(), R.string.toast_week_removed,
                                Toast.LENGTH_SHORT).show();
                        refresh();
                    }
                })
                .show();
    }

    private void showOutfitChooser(int day) {
        List<Outfit> saved = outfitRepo.getAll(false);

        if (saved.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(WeekDays.name(day))
                    .setMessage(R.string.dialog_no_saved_outfits)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_go_outfits, (dialog, which) ->
                            ((MainActivity) requireActivity()).selectTab(R.id.nav_outfits))
                    .show();
            return;
        }

        String[] names = new String[saved.size()];
        for (int i = 0; i < saved.size(); i++) {
            Outfit o = saved.get(i);
            names[i] = o.getName() + " \u00B7 " + Occasions.label(o.getOccasion());
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_choose_outfit)
                .setItems(names, (dialog, which) -> {
                    weekRepo.setPlan(day, saved.get(which).getId());
                    Toast.makeText(requireContext(),
                            getString(R.string.toast_week_set, WeekDays.name(day)),
                            Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .show();
    }
}

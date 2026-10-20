package com.chicflow.tunisia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.activities.MainActivity;
import com.chicflow.tunisia.repository.WardrobeRepository;
import com.chicflow.tunisia.utils.SessionManager;

/** Dashboard: greeting, wardrobe size and shortcuts to the other tabs. */
public class HomeFragment extends Fragment {

    private TextView tvItemCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String name = new SessionManager(requireContext()).getName();
        ((TextView) view.findViewById(R.id.tvGreeting))
                .setText(getString(R.string.welcome_user, name));
        tvItemCount = view.findViewById(R.id.tvItemCount);

        view.findViewById(R.id.cardWardrobe).setOnClickListener(v -> goTo(R.id.nav_wardrobe));
        view.findViewById(R.id.cardOutfit).setOnClickListener(v -> goTo(R.id.nav_outfits));
        view.findViewById(R.id.cardBudget).setOnClickListener(v -> goTo(R.id.nav_budget));
        view.findViewById(R.id.cardWeek).setOnClickListener(v -> goTo(R.id.nav_week));
        view.findViewById(R.id.btnLogout).setOnClickListener(v ->
                ((MainActivity) requireActivity()).logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        int count = WardrobeRepository.getInstance().count();
        tvItemCount.setText(getResources().getQuantityString(R.plurals.items_count, count, count));
    }

    private void goTo(int tabId) {
        ((MainActivity) requireActivity()).selectTab(tabId);
    }
}

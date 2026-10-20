package com.chicflow.tunisia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.activities.ItemFormActivity;
import com.chicflow.tunisia.adapters.ClothingAdapter;
import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.repository.WardrobeRepository;
import com.chicflow.tunisia.utils.ClothingOptions;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The wardrobe: grid of items with search, category chips, color filter and sorting. */
public class WardrobeFragment extends Fragment {

    private final WardrobeRepository repo = WardrobeRepository.getInstance();

    private ClothingAdapter adapter;
    private TextView tvCount, tvEmpty;
    private TextInputEditText etSearch;
    private ChipGroup chipGroup;

    // Current filter state (null = no filter)
    private String selectedColor = null;
    private int selectedSort = WardrobeRepository.SORT_NEWEST;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wardrobe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCount = view.findViewById(R.id.tvCount);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        etSearch = view.findViewById(R.id.etSearch);
        chipGroup = view.findViewById(R.id.chipGroup);

        // Grid
        RecyclerView rv = view.findViewById(R.id.rvItems);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new ClothingAdapter(item -> openForm(item));
        rv.setAdapter(adapter);

        // Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });

        // Category chips
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> refresh());

        // Color dropdown
        MaterialAutoCompleteTextView actColor = view.findViewById(R.id.actColor);
        List<String> colorOptions = new ArrayList<>();
        colorOptions.add(getString(R.string.option_all_colors));
        colorOptions.addAll(Arrays.asList(ClothingOptions.COLOR_NAMES));
        actColor.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, colorOptions));
        actColor.setText(colorOptions.get(0), false);
        actColor.setOnItemClickListener((parent, v, position, id) -> {
            selectedColor = position == 0 ? null : ClothingOptions.COLOR_NAMES[position - 1];
            refresh();
        });

        // Sort dropdown
        MaterialAutoCompleteTextView actSort = view.findViewById(R.id.actSort);
        String[] sortOptions = getResources().getStringArray(R.array.sort_options);
        actSort.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sortOptions));
        actSort.setText(sortOptions[0], false);
        actSort.setOnItemClickListener((parent, v, position, id) -> {
            selectedSort = position;      // position order = SORT_* constants
            refresh();
        });

        // Add button
        view.findViewById(R.id.fabAdd).setOnClickListener(v -> openForm(null));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();      // also reloads after coming back from the add/edit screen
    }

    private String selectedCategory() {
        int id = chipGroup.getCheckedChipId();
        if (id == R.id.chipShirt) return "shirt";
        if (id == R.id.chipTrousers) return "trousers";
        if (id == R.id.chipJacket) return "jacket";
        if (id == R.id.chipShoes) return "shoes";
        if (id == R.id.chipAccessory) return "accessory";
        if (id == R.id.chipOther) return "other";
        return null;                      // "All"
    }

    private void refresh() {
        String search = String.valueOf(etSearch.getText());
        List<ClothingItem> result = repo.query(search, selectedCategory(), selectedColor, selectedSort);

        adapter.setItems(result);
        int n = result.size();
        tvCount.setText(getResources().getQuantityString(R.plurals.items_count, n, n));
        tvEmpty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
    }

    private void openForm(@Nullable ClothingItem item) {
        Intent intent = new Intent(requireContext(), ItemFormActivity.class);
        if (item != null) {
            intent.putExtra(ItemFormActivity.EXTRA_ITEM_ID, item.getId());
        }
        startActivity(intent);
    }
}

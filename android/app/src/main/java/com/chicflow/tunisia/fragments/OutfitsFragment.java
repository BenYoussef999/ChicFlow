package com.chicflow.tunisia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.activities.ChallengeActivity;
import com.chicflow.tunisia.activities.SavedOutfitsActivity;
import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.models.Outfit;
import com.chicflow.tunisia.repository.OutfitRepository;
import com.chicflow.tunisia.repository.WardrobeRepository;
import com.chicflow.tunisia.utils.Occasions;
import com.chicflow.tunisia.utils.OutfitGenerator;
import com.chicflow.tunisia.utils.OutfitViews;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Smart Outfit tab: choose an occasion, get an outfit, save it as a favorite. */
public class OutfitsFragment extends Fragment {

    private ChipGroup chipOccasions;
    private View cardResult;
    private TextView tvName, tvSource, tvExplanation, tvNotEnough;
    private LinearLayout containerPieces;
    private MaterialButton btnSave;

    private Outfit current;            // the outfit being shown (not stored until saved)
    private boolean currentSaved;
    private Set<Integer> lastIds = null;   // pieces of the previous suggestion

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_outfits, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chipOccasions = view.findViewById(R.id.chipOccasions);
        cardResult = view.findViewById(R.id.cardResult);
        tvName = view.findViewById(R.id.tvResultName);
        tvSource = view.findViewById(R.id.tvResultSource);
        tvExplanation = view.findViewById(R.id.tvResultExplanation);
        tvNotEnough = view.findViewById(R.id.tvNotEnough);
        containerPieces = view.findViewById(R.id.containerPieces);
        btnSave = view.findViewById(R.id.btnSaveFavorite);

        view.findViewById(R.id.btnSuggest).setOnClickListener(v -> generate());
        view.findViewById(R.id.btnTryAnother).setOnClickListener(v -> generate());
        btnSave.setOnClickListener(v -> saveCurrent());

        view.findViewById(R.id.btnChallenge).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChallengeActivity.class)));
        view.findViewById(R.id.btnSavedOutfits).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SavedOutfitsActivity.class)));
    }

    private String selectedOccasion() {
        int id = chipOccasions.getCheckedChipId();
        if (id == R.id.chipWork) return "work";
        if (id == R.id.chipCasual) return "casual";
        if (id == R.id.chipEvent) return "event";
        return "university";
    }

    private void generate() {
        String occasion = selectedOccasion();
        List<ClothingItem> wardrobe = WardrobeRepository.getInstance().getAll();

        // Ask again (up to 100 times, it is instant) if we got exactly the same pieces as last time
        OutfitGenerator.Suggestion suggestion = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            suggestion = OutfitGenerator.suggest(wardrobe, occasion);
            if (suggestion == null) break;
            Set<Integer> pieceIds = new HashSet<>();
            for (ClothingItem item : suggestion.items) {
                pieceIds.add(item.getId());
            }
            if (!pieceIds.equals(lastIds)) {
                lastIds = pieceIds;
                break;
            }
        }

        if (suggestion == null) {
            current = null;
            cardResult.setVisibility(View.GONE);
            tvNotEnough.setVisibility(View.VISIBLE);
            return;
        }
        tvNotEnough.setVisibility(View.GONE);

        List<Integer> ids = new ArrayList<>();
        for (ClothingItem item : suggestion.items) {
            ids.add(item.getId());
        }
        current = new Outfit(0, Occasions.label(occasion) + " look", occasion,
                suggestion.explanation, Outfit.SOURCE_RULES, false, ids);
        currentSaved = false;

        tvName.setText(current.getName());
        tvSource.setText(Occasions.sourceLabel(current.getSource()));
        tvExplanation.setText(current.getExplanation());
        OutfitViews.bindPieces(getLayoutInflater(), containerPieces, suggestion.items);
        btnSave.setText(R.string.btn_save_favorite);
        btnSave.setEnabled(true);
        cardResult.setVisibility(View.VISIBLE);
    }

    private void saveCurrent() {
        if (current == null || currentSaved) return;
        current.setFavorite(true);
        OutfitRepository.getInstance().add(current);
        currentSaved = true;
        btnSave.setText(R.string.btn_saved);
        btnSave.setEnabled(false);
        Toast.makeText(requireContext(), R.string.toast_outfit_saved, Toast.LENGTH_SHORT).show();
    }
}

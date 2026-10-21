package com.chicflow.tunisia.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.adapters.OutfitAdapter;
import com.chicflow.tunisia.models.Outfit;
import com.chicflow.tunisia.repository.OutfitRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Lists saved outfits (all, or favorites only). Heart = favorite, bin = delete. */
public class SavedOutfitsActivity extends AppCompatActivity {

    private final OutfitRepository repo = OutfitRepository.getInstance();

    private OutfitAdapter adapter;
    private ChipGroup chipGroup;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_outfits);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        chipGroup = findViewById(R.id.chipGroup);
        tvEmpty = findViewById(R.id.tvEmpty);

        RecyclerView rv = findViewById(R.id.rvOutfits);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OutfitAdapter(new OutfitAdapter.OnActionListener() {
            @Override
            public void onToggleFavorite(Outfit outfit) {
                repo.setFavorite(outfit.getId(), !outfit.isFavorite());
                refresh();
            }

            @Override
            public void onDelete(Outfit outfit) {
                confirmDelete(outfit);
            }
        });
        rv.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> refresh());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean favoritesOnly = chipGroup.getCheckedChipId() == R.id.chipFavorites;
        List<Outfit> list = repo.getAll(favoritesOnly);
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(Outfit outfit) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_outfit_title)
                .setMessage(R.string.dialog_delete_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    repo.delete(outfit.getId());
                    Toast.makeText(this, R.string.toast_outfit_deleted, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .show();
    }
}

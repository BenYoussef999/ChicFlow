package com.chicflow.tunisia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.models.Outfit;
import com.chicflow.tunisia.repository.OutfitRepository;
import com.chicflow.tunisia.utils.Occasions;
import com.chicflow.tunisia.utils.OutfitViews;

import java.util.ArrayList;
import java.util.List;

/** Cards for the saved outfits list. */
public class OutfitAdapter extends RecyclerView.Adapter<OutfitAdapter.ViewHolder> {

    public interface OnActionListener {
        void onToggleFavorite(Outfit outfit);
        void onDelete(Outfit outfit);
    }

    private final List<Outfit> items = new ArrayList<>();
    private final OnActionListener listener;

    public OutfitAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Outfit> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_outfit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Outfit outfit = items.get(position);

        holder.tvName.setText(outfit.getName());
        holder.tvMeta.setText(Occasions.label(outfit.getOccasion()));
        holder.tvSource.setText(Occasions.sourceLabel(outfit.getSource()));
        holder.tvExplanation.setText(outfit.getExplanation());
        holder.tvExplanation.setVisibility(
                outfit.getExplanation() == null || outfit.getExplanation().isEmpty()
                        ? View.GONE : View.VISIBLE);

        OutfitViews.bindPieces(LayoutInflater.from(holder.itemView.getContext()),
                holder.containerPieces, OutfitRepository.getInstance().itemsOf(outfit));

        holder.btnFavorite.setImageResource(
                outfit.isFavorite() ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
        holder.btnFavorite.setOnClickListener(v -> listener.onToggleFavorite(outfit));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(outfit));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvMeta, tvSource, tvExplanation;
        final LinearLayout containerPieces;
        final ImageButton btnFavorite, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvOutfitName);
            tvMeta = itemView.findViewById(R.id.tvOutfitMeta);
            tvSource = itemView.findViewById(R.id.tvOutfitSource);
            tvExplanation = itemView.findViewById(R.id.tvOutfitExplanation);
            containerPieces = itemView.findViewById(R.id.containerPieces);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnDelete = itemView.findViewById(R.id.btnOutfitDelete);
        }
    }
}

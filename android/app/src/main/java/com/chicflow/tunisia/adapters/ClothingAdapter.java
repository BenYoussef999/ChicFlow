package com.chicflow.tunisia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.utils.ClothingOptions;
import com.chicflow.tunisia.utils.Format;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shows clothing items as cards in a grid. Can highlight selected items (challenge screen). */
public class ClothingAdapter extends RecyclerView.Adapter<ClothingAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ClothingItem item);
    }

    private final List<ClothingItem> items = new ArrayList<>();
    private final Set<Integer> selectedIds = new HashSet<>();
    private final OnItemClickListener listener;

    public ClothingAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ClothingItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();   // lists are small, so this simple call is fine
    }

    /** Ids of the items to highlight with a check mark. */
    public void setSelectedIds(Set<Integer> ids) {
        selectedIds.clear();
        selectedIds.addAll(ids);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clothing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClothingItem item = items.get(position);
        boolean selected = selectedIds.contains(item.getId());
        float density = holder.itemView.getResources().getDisplayMetrics().density;

        holder.swatch.setBackgroundColor(ClothingOptions.colorInt(item.getColor()));
        holder.tvEmoji.setText(ClothingOptions.categoryEmoji(item.getCategory()));
        holder.tvName.setText(item.getName());
        holder.tvMeta.setText(ClothingOptions.categoryLabel(item.getCategory())
                + " \u00B7 " + item.getColor());
        holder.tvPrice.setText(Format.tnd(item.getPrice()));

        holder.tvCheck.setVisibility(selected ? View.VISIBLE : View.GONE);
        holder.card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(),
                selected ? R.color.cf_burgundy : R.color.cf_beige_dark));
        holder.card.setStrokeWidth(Math.round((selected ? 3 : 1) * density));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View swatch;
        final TextView tvEmoji, tvName, tvMeta, tvPrice, tvCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            swatch = itemView.findViewById(R.id.swatch);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvName = itemView.findViewById(R.id.tvName);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCheck = itemView.findViewById(R.id.tvCheck);
        }
    }
}

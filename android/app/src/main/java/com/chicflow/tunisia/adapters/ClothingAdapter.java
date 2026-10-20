package com.chicflow.tunisia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.utils.ClothingOptions;
import com.chicflow.tunisia.utils.Format;

import java.util.ArrayList;
import java.util.List;

/** Shows clothing items as cards in the wardrobe grid. */
public class ClothingAdapter extends RecyclerView.Adapter<ClothingAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ClothingItem item);
    }

    private final List<ClothingItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public ClothingAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ClothingItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();   // lists are small, so this simple call is fine
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

        holder.swatch.setBackgroundColor(ClothingOptions.colorInt(item.getColor()));
        holder.tvEmoji.setText(ClothingOptions.categoryEmoji(item.getCategory()));
        holder.tvName.setText(item.getName());
        holder.tvMeta.setText(ClothingOptions.categoryLabel(item.getCategory())
                + " \u00B7 " + item.getColor());
        holder.tvPrice.setText(Format.tnd(item.getPrice()));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View swatch;
        final TextView tvEmoji, tvName, tvMeta, tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            swatch = itemView.findViewById(R.id.swatch);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvName = itemView.findViewById(R.id.tvName);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
}

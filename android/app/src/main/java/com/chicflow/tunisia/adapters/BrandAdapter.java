package com.chicflow.tunisia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.models.Brand;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/** Cards for the Local Style catalog. Only administrators see the delete button. */
public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.ViewHolder> {

    public interface OnActionListener {
        void onVisit(Brand brand);
        void onDelete(Brand brand);
    }

    private final List<Brand> items = new ArrayList<>();
    private final boolean admin;
    private final OnActionListener listener;

    public BrandAdapter(boolean admin, OnActionListener listener) {
        this.admin = admin;
        this.listener = listener;
    }

    public void setItems(List<Brand> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_brand, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Brand brand = items.get(position);

        holder.tvName.setText(brand.getName());

        String meta = brand.getCategory();
        if (brand.getPriceRange() != null && !brand.getPriceRange().isEmpty()) {
            meta += " \u00B7 " + brand.getPriceRange();
        }
        holder.tvMeta.setText(meta);

        holder.btnVisit.setVisibility(brand.hasWebsite() ? View.VISIBLE : View.GONE);
        holder.btnVisit.setOnClickListener(v -> listener.onVisit(brand));

        holder.btnDelete.setVisibility(admin ? View.VISIBLE : View.GONE);
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(brand));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvMeta;
        final MaterialButton btnVisit;
        final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvBrandName);
            tvMeta = itemView.findViewById(R.id.tvBrandMeta);
            btnVisit = itemView.findViewById(R.id.btnVisit);
            btnDelete = itemView.findViewById(R.id.btnBrandDelete);
        }
    }
}

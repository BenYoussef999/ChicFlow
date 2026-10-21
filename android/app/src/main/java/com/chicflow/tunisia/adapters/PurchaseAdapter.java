package com.chicflow.tunisia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.models.Purchase;
import com.chicflow.tunisia.utils.Format;

import java.util.ArrayList;
import java.util.List;

/** One row per purchase, with a delete button. */
public class PurchaseAdapter extends RecyclerView.Adapter<PurchaseAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(Purchase purchase);
    }

    private final List<Purchase> items = new ArrayList<>();
    private final OnDeleteListener listener;

    public PurchaseAdapter(OnDeleteListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Purchase> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_purchase, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Purchase p = items.get(position);
        holder.tvDescription.setText(p.getDescription());
        holder.tvDate.setText(p.getDate());
        holder.tvAmount.setText(Format.tnd(p.getAmount()));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDescription, tvDate, tvAmount;
        final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvPurchaseDescription);
            tvDate = itemView.findViewById(R.id.tvPurchaseDate);
            tvAmount = itemView.findViewById(R.id.tvPurchaseAmount);
            btnDelete = itemView.findViewById(R.id.btnPurchaseDelete);
        }
    }
}

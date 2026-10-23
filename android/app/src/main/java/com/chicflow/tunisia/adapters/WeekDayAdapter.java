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
import com.chicflow.tunisia.models.DayPlan;
import com.chicflow.tunisia.utils.ClothingOptions;
import com.chicflow.tunisia.utils.WeekDays;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** Seven rows (Monday to Sunday) showing the planned outfit of each day. */
public class WeekDayAdapter extends RecyclerView.Adapter<WeekDayAdapter.ViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(DayPlan plan);
    }

    private final List<DayPlan> items = new ArrayList<>();
    private final int today;
    private final OnDayClickListener listener;

    public WeekDayAdapter(int today, OnDayClickListener listener) {
        this.today = today;
        this.listener = listener;
    }

    public void setItems(List<DayPlan> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayPlan plan = items.get(position);
        boolean isToday = plan.getDay() == today;
        float density = holder.itemView.getResources().getDisplayMetrics().density;

        holder.tvDay.setText(WeekDays.name(plan.getDay()));
        holder.tvToday.setVisibility(isToday ? View.VISIBLE : View.GONE);
        holder.card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(),
                isToday ? R.color.cf_burgundy : R.color.cf_beige_dark));
        holder.card.setStrokeWidth(Math.round((isToday ? 2 : 1) * density));

        if (plan.isPlanned()) {
            holder.tvOutfit.setText(plan.getOutfit().getName());
            holder.tvOutfit.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.cf_brown));
            StringBuilder emojis = new StringBuilder();
            for (ClothingItem item : plan.getItems()) {
                emojis.append(ClothingOptions.categoryEmoji(item.getCategory())).append("  ");
            }
            holder.tvPieces.setText(emojis.toString().trim());
            holder.tvPieces.setVisibility(View.VISIBLE);
        } else {
            holder.tvOutfit.setText(R.string.week_nothing);
            holder.tvOutfit.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.cf_brown_light));
            holder.tvPieces.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onDayClick(plan));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvDay, tvToday, tvOutfit, tvPieces;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvDay = itemView.findViewById(R.id.tvDayName);
            tvToday = itemView.findViewById(R.id.tvTodayBadge);
            tvOutfit = itemView.findViewById(R.id.tvDayOutfit);
            tvPieces = itemView.findViewById(R.id.tvDayPieces);
        }
    }
}

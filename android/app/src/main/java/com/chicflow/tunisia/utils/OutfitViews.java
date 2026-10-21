package com.chicflow.tunisia.utils;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.models.ClothingItem;

import java.util.List;

/** Fills a LinearLayout with one row per clothing item (used in several screens). */
public final class OutfitViews {

    private OutfitViews() { }

    public static void bindPieces(LayoutInflater inflater, LinearLayout container,
                                  List<ClothingItem> items) {
        container.removeAllViews();
        float density = container.getResources().getDisplayMetrics().density;

        for (ClothingItem item : items) {
            View row = inflater.inflate(R.layout.item_outfit_piece, container, false);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ClothingOptions.colorInt(item.getColor()));
            bg.setCornerRadius(10 * density);
            bg.setStroke(Math.round(density), Color.parseColor("#E8DCC8"));
            row.findViewById(R.id.pieceSwatch).setBackground(bg);

            ((TextView) row.findViewById(R.id.pieceEmoji))
                    .setText(ClothingOptions.categoryEmoji(item.getCategory()));
            ((TextView) row.findViewById(R.id.pieceName)).setText(item.getName());
            ((TextView) row.findViewById(R.id.pieceMeta)).setText(
                    ClothingOptions.categoryLabel(item.getCategory()) + " \u00B7 " + item.getColor());

            container.addView(row);
        }
    }
}

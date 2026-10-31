package com.chicflow.tunisia.network;

import com.chicflow.tunisia.models.ClothingItem;

import org.json.JSONException;
import org.json.JSONObject;

/** Clothing item <-> the JSON used by the PHP API (see backend/API.md). */
public final class ItemJson {

    private ItemJson() { }

    /** Server JSON -> item (remoteId = the server's id, status "synced"). */
    public static ClothingItem fromJson(JSONObject o) {
        ClothingItem item = new ClothingItem(
                0,
                text(o, "name"),
                text(o, "category"),
                text(o, "color"),
                o.optDouble("price", 0),
                text(o, "photo_url"),
                text(o, "date_added"));
        item.setRemoteId(o.optInt("id", 0));
        item.setClientUuid(text(o, "client_uuid"));
        return item;
    }

    /** Body of clothes/create.php */
    public static JSONObject toCreateJson(ClothingItem item) {
        JSONObject o = new JSONObject();
        try {
            o.put("name", item.getName());
            o.put("category", item.getCategory());
            o.put("color", item.getColor());
            o.put("price", item.getPrice());
            o.put("date_added", item.getDateAdded());
            o.put("client_uuid", item.getClientUuid());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }

    /** Body of clothes/update.php */
    public static JSONObject toUpdateJson(ClothingItem item) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", item.getRemoteId());
            o.put("name", item.getName());
            o.put("category", item.getCategory());
            o.put("color", item.getColor());
            o.put("price", item.getPrice());
            o.put("date_added", item.getDateAdded());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }

    /** Text value, or null when the key is missing or JSON null (Android would otherwise give the text "null"). */
    private static String text(JSONObject o, String key) {
        return o.isNull(key) ? null : o.optString(key);
    }
}

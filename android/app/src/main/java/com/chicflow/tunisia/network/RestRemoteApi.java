package com.chicflow.tunisia.network;

import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.sync.RemoteApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** RemoteApi for the PHP REST API. Which endpoints are called, and with what, is decided here. */
public class RestRemoteApi implements RemoteApi {

    private final Transport transport;

    public RestRemoteApi(Transport transport) {
        this.transport = transport;
    }

    @Override
    public ClothingItem createItem(ClothingItem item) throws ApiException {
        JSONObject data = transport.post("clothes/create.php", ItemJson.toCreateJson(item));
        JSONObject json = data.optJSONObject("item");
        if (json == null) {
            throw new ApiException("Unexpected answer from the server.", -1);
        }
        return ItemJson.fromJson(json);
    }

    @Override
    public void updateItem(ClothingItem item) throws ApiException {
        transport.post("clothes/update.php", ItemJson.toUpdateJson(item));
    }

    @Override
    public void deleteItem(int remoteId) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("id", remoteId);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        transport.post("clothes/delete.php", body);
    }

    @Override
    public List<ClothingItem> pullItems() throws ApiException {
        JSONObject data = transport.get("clothes/list.php?sort=newest");
        JSONArray array = data.optJSONArray("items");
        if (array == null) {
            throw new ApiException("Unexpected answer from the server.", -1);
        }
        List<ClothingItem> items = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.optJSONObject(i);
            if (json != null) {
                items.add(ItemJson.fromJson(json));
            }
        }
        return items;
    }
}

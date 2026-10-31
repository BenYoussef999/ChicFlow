package com.chicflow.tunisia.sync;

import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.network.ApiException;

import java.util.List;

/** What the sync engine needs from the server. Real implementation: RestRemoteApi. All calls BLOCK. */
public interface RemoteApi {

    /** Sends a new item (with its clientUuid). Returns the server's version, with remoteId set. */
    ClothingItem createItem(ClothingItem item) throws ApiException;

    /** Sends the new values of an item that has a remoteId. */
    void updateItem(ClothingItem item) throws ApiException;

    void deleteItem(int remoteId) throws ApiException;

    /** All items of the logged-in user, as the server has them. */
    List<ClothingItem> pullItems() throws ApiException;
}

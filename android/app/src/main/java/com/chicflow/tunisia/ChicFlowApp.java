package com.chicflow.tunisia;

import android.app.Application;

import com.chicflow.tunisia.repository.WardrobeRepository;
import com.chicflow.tunisia.sync.SyncManager;

/** Runs once when the app process starts: prepares the database layer and the sync. */
public class ChicFlowApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WardrobeRepository.init(this);
        SyncManager.init(this);
        SyncManager.getInstance().startNetworkMonitoring();
    }
}

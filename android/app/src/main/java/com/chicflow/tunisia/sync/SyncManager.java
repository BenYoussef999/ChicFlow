package com.chicflow.tunisia.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.db.ClothingDao;
import com.chicflow.tunisia.db.DatabaseHelper;
import com.chicflow.tunisia.network.ApiClient;
import com.chicflow.tunisia.network.RestRemoteApi;
import com.chicflow.tunisia.utils.SessionManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the synchronisation in the background and tells the screens about it.
 *
 * When does a sync start?
 *   - after every local change (add / edit / delete)
 *   - when the user logs in and the main screen opens
 *   - when the phone gets a network connection back
 *   - when the user taps the status line on the Home screen
 * Several requests at once are merged into a single run.
 */
public class SyncManager {

    /** Called on the main thread whenever the state or the data may have changed. */
    public interface Listener {
        void onSyncChanged();
    }

    public static final int STATE_IDLE = 0;
    public static final int STATE_SYNCING = 1;
    public static final int STATE_OFFLINE = 2;   // could not reach the server: changes wait on the phone
    public static final int STATE_ISSUES = 3;    // finished, but the server refused some changes

    private static final String TAG = "SyncManager";
    private static SyncManager instance;

    private final Context app;
    private final SessionManager session;
    private final ClothingDao dao;
    private final RemoteApi remote;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean queued = new AtomicBoolean(false);
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private volatile int state = STATE_IDLE;
    private volatile long lastSuccessMillis = 0;
    private volatile int lastFailed = 0;
    private volatile boolean authExpired = false;

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SyncManager(context.getApplicationContext());
        }
    }

    public static synchronized SyncManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SyncManager.init() was not called (see ChicFlowApp)");
        }
        return instance;
    }

    private SyncManager(Context app) {
        this.app = app;
        this.session = new SessionManager(app);
        this.dao = new ClothingDao(DatabaseHelper.getInstance(app));
        this.remote = new RestRemoteApi(ApiClient.getInstance(app).asTransport());
    }

    // ================================================================= starting a sync

    /** Asks for a sync. Returns immediately: the work happens on a background thread. */
    public void requestSync() {
        if (!session.isLoggedIn()) return;
        authExpired = false;
        if (queued.compareAndSet(false, true)) {
            executor.execute(this::runSync);
        }
    }

    private void runSync() {
        queued.set(false);
        if (!session.isLoggedIn()) return;

        state = STATE_SYNCING;
        notifyListeners();
        try {
            SyncResult result = new SyncEngine(dao, remote).sync(session.getUserId());
            if (result.authExpired) {
                authExpired = true;
                state = STATE_IDLE;
            } else if (result.offline) {
                state = STATE_OFFLINE;
            } else {
                lastSuccessMillis = System.currentTimeMillis();
                lastFailed = result.failed;
                state = result.failed > 0 ? STATE_ISSUES : STATE_IDLE;
            }
            Log.d(TAG, "sync: completed=" + result.completed + " pushed=" + result.pushed
                    + " pulled=" + result.pulled + " failed=" + result.failed + " " + result.message);
        } catch (RuntimeException e) {
            Log.e(TAG, "Sync crashed", e);      // never let the background thread die silently
            state = STATE_OFFLINE;
        }
        notifyListeners();
    }

    /** Starts a sync every time the phone gets a network connection (Wi-Fi or mobile data). */
    public void startNetworkMonitoring() {
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    requestSync();
                }
            });
        } catch (RuntimeException e) {
            Log.w(TAG, "Network monitoring not available", e);
        }
    }

    // ================================================================= state for the screens

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (Listener l : listeners) {
                l.onSyncChanged();
            }
        });
    }

    /** True when the server said the login is no longer valid: the user must log in again. */
    public boolean isAuthExpired() {
        return authExpired;
    }

    /** Forget the state of the previous user (call when logging out). */
    public void resetForLogout() {
        state = STATE_IDLE;
        lastSuccessMillis = 0;
        lastFailed = 0;
        authExpired = false;
    }

    /** One line for the Home screen, for example "Synced 2 minutes ago". */
    public String describeStatus(Context context) {
        if (state == STATE_SYNCING) {
            return context.getString(R.string.sync_syncing);
        }
        int pending = dao.countPending(session.getUserId());
        String waiting = context.getResources().getQuantityString(R.plurals.sync_waiting, pending, pending);

        if (state == STATE_OFFLINE) {
            return pending > 0
                    ? context.getString(R.string.sync_offline_pending, waiting)
                    : context.getString(R.string.sync_offline);
        }
        if (state == STATE_ISSUES) {
            return context.getString(R.string.sync_issues, lastFailed);
        }
        if (lastSuccessMillis == 0) {
            return pending > 0 ? waiting : context.getString(R.string.sync_never);
        }
        CharSequence ago = DateUtils.getRelativeTimeSpanString(
                lastSuccessMillis, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
        return pending > 0
                ? context.getString(R.string.sync_ok_pending, ago, waiting)
                : context.getString(R.string.sync_ok, ago);
    }
}

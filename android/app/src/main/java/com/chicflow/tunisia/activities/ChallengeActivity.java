package com.chicflow.tunisia.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chicflow.tunisia.R;
import com.chicflow.tunisia.adapters.ClothingAdapter;
import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.models.Outfit;
import com.chicflow.tunisia.repository.OutfitRepository;
import com.chicflow.tunisia.repository.WardrobeRepository;
import com.chicflow.tunisia.utils.Occasions;
import com.chicflow.tunisia.utils.OutfitGenerator;
import com.chicflow.tunisia.utils.OutfitViews;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * 60-second outfit challenge: a random occasion, a CountDownTimer, and a score at the end.
 * Three states: IDLE (before start), RUNNING (timer counting down), FINISHED (result shown).
 */
public class ChallengeActivity extends AppCompatActivity {

    private static final long DURATION_MS = 60000L;
    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_FINISHED = 2;

    private final WardrobeRepository wardrobe = WardrobeRepository.getInstance();
    private final Set<Integer> selected = new HashSet<>();
    private final Random random = new Random();

    private int state = STATE_IDLE;
    private CountDownTimer timer;
    private long remainingMs = DURATION_MS;
    private String occasion = "university";
    private List<ClothingItem> lastPicked = new ArrayList<>();
    private int lastScore;

    private TextView tvOccasion, tvTimer, tvIdleHint, tvScore, tvTimeInfo, tvFeedback;
    private LinearProgressIndicator progress;
    private RecyclerView rvPieces;
    private View svResult;
    private LinearLayout containerPieces;
    private MaterialButton btnAction, btnSaveOutfit;
    private ClothingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvOccasion = findViewById(R.id.tvOccasion);
        tvTimer = findViewById(R.id.tvTimer);
        progress = findViewById(R.id.progressTimer);
        tvIdleHint = findViewById(R.id.tvIdleHint);
        rvPieces = findViewById(R.id.rvPieces);
        svResult = findViewById(R.id.svResult);
        tvScore = findViewById(R.id.tvScore);
        tvTimeInfo = findViewById(R.id.tvTimeInfo);
        tvFeedback = findViewById(R.id.tvFeedback);
        containerPieces = findViewById(R.id.containerPieces);
        btnAction = findViewById(R.id.btnAction);
        btnSaveOutfit = findViewById(R.id.btnSaveOutfit);

        adapter = new ClothingAdapter(item -> toggle(item));
        rvPieces.setLayoutManager(new GridLayoutManager(this, 2));
        rvPieces.setAdapter(adapter);

        btnAction.setOnClickListener(v -> {
            if (state == STATE_RUNNING) {
                finishChallenge(false);
            } else {
                startChallenge();
            }
        });
        btnSaveOutfit.setOnClickListener(v -> saveOutfit());

        showIdle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    private void showIdle() {
        state = STATE_IDLE;
        tvOccasion.setText(R.string.challenge_ready);
        updateTimer(DURATION_MS);

        boolean enough = wardrobe.count() >= 3;
        tvIdleHint.setText(enough ? R.string.challenge_idle_hint : R.string.challenge_need_items);
        tvIdleHint.setVisibility(View.VISIBLE);
        rvPieces.setVisibility(View.GONE);
        svResult.setVisibility(View.GONE);

        btnAction.setText(R.string.btn_start_challenge);
        btnAction.setEnabled(enough);
    }

    private void startChallenge() {
        occasion = Occasions.KEYS[random.nextInt(Occasions.KEYS.length)];
        selected.clear();
        adapter.setItems(wardrobe.getAll());
        adapter.setSelectedIds(selected);

        tvOccasion.setText(getString(R.string.challenge_dress_for, Occasions.label(occasion)));
        state = STATE_RUNNING;
        tvIdleHint.setVisibility(View.GONE);
        svResult.setVisibility(View.GONE);
        rvPieces.setVisibility(View.VISIBLE);
        btnAction.setText(R.string.btn_finish);
        updateTimer(DURATION_MS);

        if (timer != null) timer.cancel();
        timer = new CountDownTimer(DURATION_MS, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimer(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                updateTimer(0);
                finishChallenge(true);
            }
        }.start();
    }

    /** Shows the remaining time as text and in the progress bar. Red during the last 10 seconds. */
    private void updateTimer(long ms) {
        remainingMs = ms;
        int seconds = (int) Math.ceil(ms / 1000.0);
        tvTimer.setText(String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60));
        progress.setProgressCompat((int) ms, false);

        boolean low = state == STATE_RUNNING && seconds <= 10;
        int color = ContextCompat.getColor(this, low ? R.color.cf_error : R.color.cf_burgundy);
        tvTimer.setTextColor(low ? color : ContextCompat.getColor(this, R.color.cf_brown));
        progress.setIndicatorColor(color);
    }

    private void toggle(ClothingItem item) {
        if (state != STATE_RUNNING) return;
        if (!selected.remove(item.getId())) {
            selected.add(item.getId());
        }
        adapter.setSelectedIds(selected);
    }

    private void finishChallenge(boolean timeUp) {
        if (state != STATE_RUNNING) return;
        state = STATE_FINISHED;
        if (timer != null) timer.cancel();

        lastPicked = new ArrayList<>();
        for (ClothingItem item : wardrobe.getAll()) {
            if (selected.contains(item.getId())) lastPicked.add(item);
        }
        OutfitGenerator.Evaluation result = OutfitGenerator.evaluate(lastPicked, occasion);
        lastScore = result.score;

        tvScore.setText(getString(R.string.challenge_score, result.score));
        tvTimeInfo.setText(timeUp
                ? getString(R.string.challenge_time_up)
                : getString(R.string.challenge_time_left, (int) (remainingMs / 1000)));

        StringBuilder sb = new StringBuilder();
        for (String line : result.feedback) {
            sb.append("\u2022 ").append(line).append("\n");
        }
        tvFeedback.setText(sb.toString().trim());

        OutfitViews.bindPieces(getLayoutInflater(), containerPieces, lastPicked);
        btnSaveOutfit.setVisibility(lastPicked.isEmpty() ? View.GONE : View.VISIBLE);
        btnSaveOutfit.setText(R.string.btn_save_outfit);
        btnSaveOutfit.setEnabled(true);

        rvPieces.setVisibility(View.GONE);
        svResult.setVisibility(View.VISIBLE);
        btnAction.setText(R.string.btn_play_again);
        updateTimer(remainingMs);
    }

    private void saveOutfit() {
        List<Integer> ids = new ArrayList<>();
        for (ClothingItem item : lastPicked) {
            ids.add(item.getId());
        }
        Outfit outfit = new Outfit(0,
                getString(R.string.challenge_outfit_name, Occasions.label(occasion)),
                occasion,
                getString(R.string.challenge_outfit_note, lastScore),
                Outfit.SOURCE_MANUAL, true, ids);
        OutfitRepository.getInstance().add(outfit);

        btnSaveOutfit.setText(R.string.btn_saved);
        btnSaveOutfit.setEnabled(false);
    }
}

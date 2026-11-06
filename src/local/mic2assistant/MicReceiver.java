package local.mic2assistant;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/**
 * Receives the JMGO remote mic-button broadcast and opens Google Assistant (katniss)
 * in listening mode. If the assistant settings were reset (JMGO force-stops katniss
 * before standby, and Android then clears them), restores them first.
 */
public class MicReceiver extends BroadcastReceiver {
    private static final String TAG = "Mic2Assistant";
    private static final long DEBOUNCE_MS = 1500;
    /** Time for VoiceInteractionManagerService to bind katniss after a settings change. */
    private static final long BIND_DELAY_MS = 1500;

    private static final ComponentName KATNISS = new ComponentName(
            "com.google.android.katniss",
            "com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline");

    @Override
    public void onReceive(Context context, Intent intent) {
        String operate = null;
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Object v = extras.get("operate");
            if (v != null) operate = String.valueOf(v);
        }
        Log.i(TAG, "AI_VOICE received, operate=" + operate);

        // React only to the press (NEAR_START); ignore release / stop style events.
        if (operate != null) {
            String op = operate.toUpperCase();
            if (op.contains("STOP") || op.contains("END") || op.contains("CANCEL")) {
                return;
            }
        }

        SharedPreferences prefs = context.getSharedPreferences("state", Context.MODE_PRIVATE);
        long now = SystemClock.elapsedRealtime();
        long last = prefs.getLong("last", 0);
        if (last != 0 && now >= last && now - last < DEBOUNCE_MS) {
            return;
        }
        prefs.edit().putLong("last", now).apply();

        final Context app = context.getApplicationContext();
        boolean restored = AssistantSettings.ensure(app);
        if (!restored) {
            launch(app);
            return;
        }
        final PendingResult pending = goAsync();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    launch(app);
                } finally {
                    pending.finish();
                }
            }
        }, BIND_DELAY_MS);
    }

    private static void launch(Context context) {
        Intent assist = new Intent(Intent.ACTION_ASSIST);
        assist.setComponent(KATNISS);
        assist.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(assist);
        } catch (Exception e) {
            Log.w(TAG, "katniss launch failed, falling back to generic ASSIST", e);
            try {
                Intent generic = new Intent(Intent.ACTION_ASSIST);
                generic.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(generic);
            } catch (Exception e2) {
                Log.e(TAG, "ASSIST launch failed", e2);
            }
        }
    }
}

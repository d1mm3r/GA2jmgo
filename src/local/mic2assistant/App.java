package local.mic2assistant;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

/**
 * Keeps the assistant settings restored without waiting for the first mic press.
 *
 * JMGO force-stops katniss before standby, and Android then clears the assistant settings.
 * This process survives standby (package name is in the firmware whitelist), so it watches
 * the settings and restores them as soon as they are reset, and once more on screen-on.
 */
public class App extends Application {
    private static final String TAG = "Mic2Assistant";
    /** Let VoiceInteractionManagerService finish its own reset before writing back. */
    private static final long SETTLE_MS = 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable restore = new Runnable() {
        @Override
        public void run() {
            AssistantSettings.restoreIfReset(App.this);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        ContentObserver observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                schedule("settings changed");
            }
        };
        for (String key : new String[] {"assistant", "voice_interaction_service"}) {
            Uri uri = Settings.Secure.getUriFor(key);
            getContentResolver().registerContentObserver(uri, false, observer);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                schedule(intent.getAction());
            }
        }, filter);

        schedule("process start");
    }

    private void schedule(String reason) {
        Log.i(TAG, "check assistant settings: " + reason);
        handler.removeCallbacks(restore);
        handler.postDelayed(restore, SETTLE_MS);
    }
}

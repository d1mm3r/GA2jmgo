package local.mic2assistant;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

/** Keeps Google TV Assistant (katniss) configured as the system assistant. */
final class AssistantSettings {
    private static final String TAG = "Mic2Assistant";

    static final String VIS =
            "com.google.android.katniss/com.google.android.katniss.search.serviceapi.KatnissVoiceInteractionService";
    static final String RECOGNIZER =
            "com.google.android.katniss/com.google.android.apps.tvsearch.voice.recognition.KatnissRecognitionService";

    private AssistantSettings() {}

    /**
     * Restores the assistant settings if something reset them.
     *
     * @return true if settings had to be changed (the assistant needs a moment to bind).
     */
    static boolean ensure(Context context) {
        ContentResolver cr = context.getContentResolver();
        boolean changed = false;
        try {
            changed |= put(cr, "voice_interaction_service", VIS);
            changed |= put(cr, "assistant", VIS);
            changed |= put(cr, "voice_recognition_service", RECOGNIZER);
        } catch (SecurityException e) {
            Log.e(TAG, "No WRITE_SECURE_SETTINGS; run: adb shell pm grant "
                    + context.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS", e);
        }
        if (changed) Log.i(TAG, "Assistant settings restored to katniss");
        return changed;
    }

    /**
     * Restores the settings only if they were cleared (empty / null), so a different
     * assistant chosen on purpose is left alone.
     */
    static boolean restoreIfReset(Context context) {
        ContentResolver cr = context.getContentResolver();
        String assistant = Settings.Secure.getString(cr, "assistant");
        String vis = Settings.Secure.getString(cr, "voice_interaction_service");
        if (isEmpty(assistant) || isEmpty(vis)) {
            return ensure(context);
        }
        return false;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty() || "null".equals(s);
    }

    private static boolean put(ContentResolver cr, String key, String value) {
        if (value.equals(Settings.Secure.getString(cr, key))) return false;
        Settings.Secure.putString(cr, key, value);
        return true;
    }
}

package local.mic2assistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Restores the assistant settings on boot / wake-up / app update. */
public class RestoreReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Mic2Assistant", "restore on " + intent.getAction());
        AssistantSettings.ensure(context);
    }
}

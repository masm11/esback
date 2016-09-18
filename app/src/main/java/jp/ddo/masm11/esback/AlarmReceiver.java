package jp.ddo.masm11.esback;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
	PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	PowerManager.WakeLock lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EsBack");
	lock.acquire(1000);
	intent = new Intent(context, EsBackService.class);
	context.startService(intent);
    }
}

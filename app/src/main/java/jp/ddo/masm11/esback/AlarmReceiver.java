package jp.ddo.masm11.esback;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
	intent = new Intent(context, EsBackService.class);
	context.startService(intent);
    }
}

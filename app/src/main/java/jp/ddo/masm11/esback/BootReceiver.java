package jp.ddo.masm11.esback;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
	try {
	    Log.init(context.getExternalCacheDir());
	    Log.d("Boot completion received.");
	    if (intent != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int start_time = prefs.getInt("start_time", -1);
		if (start_time != -1)
		    EsBackService.schedule(context, start_time);
	    }
	} catch (Throwable e) {	// 念の為
	}
    }
}

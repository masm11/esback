package jp.ddo.masm11.esback;

import android.app.Service;
import android.os.IBinder;
import android.os.Environment;
import android.os.BatteryManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.preference.PreferenceManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.Map;
import java.io.File;

public class EsBackService extends Service {
    @Override
    public void onCreate() {
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	Log.d("flags=%d", flags);
	if (flags == 0 && intent != null) {
	    Map<String, ?> prefMap = PreferenceManager.getDefaultSharedPreferences(this).getAll();
	    
	    if (checkCondition(prefMap)) {
		Thread thread = new Thread(new EsBackThread(
				Environment.getExternalStorageDirectory(),
				new File(getFilesDir(), "privkey").toString(),
				prefMap));
		// 重いので下げておく。
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	    }
	}
	return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
    
    private boolean checkCondition(Map<String, ?> prefMap) {
	if (!checkBatteryCondition(prefMap))
	    return false;
	if (!checkWifiCondition(prefMap))
	    return false;
	Log.i("OK.");
	return true;
    }
    
    private boolean checkBatteryCondition(Map<String, ?> prefMap) {
	IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	Intent batt = registerReceiver(null, filter);
	int batt_status = batt.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	
	switch (batt_status) {
	case BatteryManager.BATTERY_STATUS_CHARGING:
	case BatteryManager.BATTERY_STATUS_FULL:
	    return true;
	    
	default:
	    Log.i("Battery not charging.");
	    return false;
	}
    }
    
    private boolean checkWifiCondition(Map<String, ?> prefMap) {
	WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	WifiInfo info = manager.getConnectionInfo();
	if (info == null) {
	    Log.i("No current Wi-Fi connection.");
	    return false;
	}
	String pref_essid = (String) prefMap.get("essid");
	if (!pref_essid.equals("")) {
	    if (info.getSSID().equals(pref_essid)) {
		Log.i("Essid not matches.");
		return false;
	    }
	}
	return true;
    }
}

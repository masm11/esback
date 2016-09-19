package jp.ddo.masm11.esback;

import android.support.v7.app.NotificationCompat;
import android.app.Service;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.IBinder;
import android.os.Environment;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.preference.PreferenceManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.Map;
import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;

public class EsBackService extends Service {
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager wifiManager;
    private WifiManager.WifiLock wifiLock;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private long lastProgressTime = 0;
    
    private class ThreadProgressListener implements EsBackThread.ProgressListener {
	private Throwable throwable;
	
	@Override
	public void onProgress(long cur, long max) {
	    setNotification(false, cur, max, null);
	}
	
	@Override
	public void onError(Throwable e) {
	    throwable = e;
	}
	
	@Override
	public void onFinished() {
	    setNotification(true, 0, 0, throwable);
	    // thread.join() したいけど、ここじゃ無理だよなぁ。
	    Log.d("release wakelock.");
	    wakeLock.release();
	    wifiLock.release();
	    Log.d("stop self.");
	    stopSelf();
	    Log.d("end.");
	}
    }
    
    @Override
    public void onCreate() {
	Log.init(getExternalCacheDir());
	Log.d("");
	powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EsBack");
	wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "EsBack");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	Log.d("flags=%d", flags);
	if (flags == 0 && intent != null) {
	    Map<String, ?> prefMap = PreferenceManager.getDefaultSharedPreferences(this).getAll();
	    
	    schedule(prefMap);
	    
	    if (checkCondition(prefMap)) {
		Thread thread = new Thread(new EsBackThread(
				Environment.getExternalStorageDirectory(),
				new File(getFilesDir(), "privkey").toString(),
				prefMap,
				new ThreadProgressListener()));
		// 重いので下げておく。
		thread.setPriority(Thread.MIN_PRIORITY);
		wakeLock.acquire();
		wifiLock.acquire();
		thread.start();
		setNotification(false, 0, 0, null);
	    }
	}
	return START_NOT_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
    
    private void setNotification(boolean completed, long cur, long max, Throwable e) {
	if (notificationManager == null) {
	    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    notificationBuilder = new NotificationCompat.Builder(this);
	    notificationBuilder.setContentTitle("EsBack");
	    notificationBuilder.setContentText(getResources().getText(R.string.backup_in_progress));
	    notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
	}
	
	if (completed) {
	    notificationBuilder.setProgress(0, 0, false);
	    notificationBuilder.setContentText(getResources().getText(e == null ? R.string.backup_done : R.string.backup_error));
	    notificationManager.notify(0, notificationBuilder.build());
	} else if (max == 0) {
	    notificationBuilder.setProgress(0, 0, true);
	    notificationManager.notify(0, notificationBuilder.build());
	} else {
	    int m = (int) (max / 1024);
	    int c = (int) (cur / 1024);
	    if (c > m)
		c = m;
	    notificationBuilder.setProgress(m, c, false);
	    
	    /* 超高速で更新すると、画面がめちゃめちゃ重くなるので、
	     * ゆっくり更新する。
	     */
	    long now = System.currentTimeMillis();
	    if (now >= lastProgressTime + 500) {
		lastProgressTime = now;
		notificationManager.notify(0, notificationBuilder.build());
	    }
	}
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
	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo ni = cm.getActiveNetworkInfo();
	if (ni == null) {
	    Log.i("No active network.");
	    return false;
	}
	if (!ni.isConnected()) {
	    Log.i("Active network not connected.");
	    return false;
	}
	if (ni.getType() != ConnectivityManager.TYPE_WIFI) {
	    Log.i("Active network is not wifi.");
	    return false;
	}
	
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
    
    private void schedule(Map<String, ?> prefMap) {
	schedule(this, (Integer) prefMap.get("start_time"));
    }
    
    static void schedule(Context context, int setting) {
	AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	
	Intent intent = new Intent(context, EsBackService.class);
	PendingIntent pi = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
	
	Calendar now = Calendar.getInstance();
	now.add(Calendar.SECOND, 5);
	Calendar sched = (Calendar) now.clone();
	sched.set(Calendar.HOUR_OF_DAY, setting / 60);
	sched.set(Calendar.MINUTE, setting % 60);
	sched.set(Calendar.SECOND, 0);
	sched.set(Calendar.MILLISECOND, 0);
	if (sched.compareTo(now) < 0) {
	    // 指定時刻はもう過ぎていたので、次の日に。
	    sched.add(Calendar.DAY_OF_MONTH, 1);
	}
	
	Log.d("scheduling at %s", DateFormat.getDateTimeInstance().format(sched.getTime()));
	manager.cancel(pi);
	manager.set(AlarmManager.RTC, sched.getTimeInMillis(), pi);
    }
}

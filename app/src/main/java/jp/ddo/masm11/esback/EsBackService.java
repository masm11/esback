package jp.ddo.masm11.esback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.AlarmManager;
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
import android.widget.Toast;

import java.util.Map;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class EsBackService extends Service {
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager wifiManager;
    private WifiManager.WifiLock wifiLock;
    private NotificationManager notificationManager;
    private Notification.Builder notificationBuilder;
    private long lastProgressTime = 0;
    private long startTime;
    private long newLastBackupTime;
    
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
	    if (throwable == null) {
		try (BufferedWriter br = new BufferedWriter(new FileWriter(new File(getFilesDir(), "lastBackupTime.txt")))) {
		    String line = Long.toString(newLastBackupTime);
		    br.write(line, 0, line.length());
		} catch (IOException e) {
		    Log.e(e, "lastBackupTime.txt");
		} finally {
		}
	    }
	    setNotification(true, 0, 0, throwable);
	    // thread.join() したいけど、ここじゃ無理だよなぁ。
	    stopForeground(false);
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
	/* 画面が消えると、ICMPv6 RA を受け取らなくなるらしい。
	 * そのまま IPv6 アドレスが消滅して、接続が切れる。
	 * 消滅した後には RA を受け取って、IPv6 アドレスが復活する。
	 * わけが解らないが、画面を消さなければ大丈夫みたい。
	 * 充電中でなかったら backup しないので、バッテリ的には問題ない。
	 * 2016/09/25, Xperia X Performance, Android 6.0.1.
	 */
	wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "EsBack");
	wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "EsBack");
	
/*
	// wifiManager.enableVerboseLogging(10);
	try {
	    Class klass = wifiManager.getClass();
	    Method method = klass.getMethod("enableVerboseLogging", int.class);
	    method.invoke(wifiManager, 10);
	} catch (Exception e) {
	    Log.w(e, "no such method?");
	}
*/
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	Log.d("flags=%d", flags);
	if (flags == 0 && intent != null) {
	    Map<String, ?> prefMap = PreferenceManager.getDefaultSharedPreferences(this).getAll();
	    
	    schedule(prefMap);
	    
	    if (checkCondition(prefMap)) {
		long lastBackupTime = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(new File(getFilesDir(), "lastBackupTime.txt")))) {
		    String line = br.readLine();
		    if (line != null)
			lastBackupTime = Long.parseLong(line);
		} catch (IOException e) {
		    Log.e(e, "lastBackupTime.txt");
		}
		newLastBackupTime = System.currentTimeMillis();
		Thread thread = new Thread(new EsBackThread(
				Environment.getExternalStorageDirectory(),
				prefMap,
				new ThreadProgressListener(),
				lastBackupTime));
		// 重いので下げておく。
		thread.setPriority(Thread.MIN_PRIORITY);
		wakeLock.acquire();
		wifiLock.acquire();
		startTime = 0;
		thread.start();
		setNotification(false, 0, 0, null);
		startForeground(1, notificationBuilder.build());
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
	    
	    NotificationChannel channel = new NotificationChannel("notify_channel_1", "バックアップ中", NotificationManager.IMPORTANCE_LOW);
	    notificationManager.createNotificationChannel(channel);
	    
	    notificationBuilder = new Notification.Builder(this, "notify_channel_1");
	    notificationBuilder.setContentTitle("EsBack");
	    notificationBuilder.setContentText(getResources().getText(R.string.backup_in_progress));
	    notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
	}
	
	if (completed) {
	    notificationBuilder.setProgress(0, 0, false);
	    notificationBuilder.setContentText(getResources().getText(e == null ? R.string.backup_done : R.string.backup_error));
	    notificationManager.notify(1, notificationBuilder.build());
	} else if (max == 0) {
	    notificationBuilder.setProgress(0, 0, true);
	    notificationManager.notify(1, notificationBuilder.build());
	} else {
	    int m = (int) (max / 1024);
	    int c = (int) (cur / 1024);
	    if (c > m)
		c = m;
	    notificationBuilder.setProgress(m, c, false);
	    
	    long now = System.currentTimeMillis();
	    if (startTime == 0)
		startTime = now;
	    
	    /* 超高速で更新すると、画面がめちゃめちゃ重くなるので、
	     * ゆっくり更新する。
	     */
	    if (now >= lastProgressTime + 500) {
		if (now > startTime && c > 0) {
		    double elapsed = (now - startTime) / 1000.0;
		    int eta = (int) (elapsed * ((double) m / c) - elapsed);
		    // Log.d("elapsed: %d, eta: %d", (int) elapsed, eta);
		    int hh, mm, ss;
		    ss = eta % 60;
		    eta = eta / 60;
		    mm = eta % 60;
		    eta = eta / 60;
		    hh = eta;
		    String text;
		    if (hh >= 1)
			text = String.format("ETA: %d:%02d:%02d", hh, mm, ss);
		    else if (mm >= 1)
			text = String.format("ETA: %d:%02d", mm, ss);
		    else
			text = String.format("ETA: %ds", ss);
		    notificationBuilder.setSubText(text);
		}
		
		lastProgressTime = now;
		notificationManager.notify(1, notificationBuilder.build());
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
	    showToast("EsBack: 充電中ではありません");
	    return false;
	}
    }
    
    private boolean checkWifiCondition(Map<String, ?> prefMap) {
	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo ni = cm.getActiveNetworkInfo();
	if (ni == null) {
	    Log.i("No active network.");
	    showToast("EsBack: ネットワークがありません");
	    return false;
	}
	if (!ni.isConnected()) {
	    Log.i("Active network not connected.");
	    showToast("EsBack: ネットワークにつながっていません");
	    return false;
	}
	if (ni.getType() != ConnectivityManager.TYPE_WIFI) {
	    Log.i("Active network is not wifi.");
	    showToast("EsBack: ネットワークが Wi-Fi ではありません");
	    return false;
	}
	
	WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	WifiInfo info = manager.getConnectionInfo();
	if (info == null) {
	    Log.i("No current Wi-Fi connection.");
	    showToast("EsBack: Wi-Fi 情報が取得できません");
	    return false;
	}
	String pref_essid = (String) prefMap.get("essid");
	if (!pref_essid.equals("")) {
	    if (!info.getSSID().equals("\"" + pref_essid + "\"")) {
		Log.i("Essid not matches.");
		showToast("EsBack: ESSID が一致しません");
		return false;
	    }
	}
	return true;
    }
    
    private void schedule(Map<String, ?> prefMap) {
	schedule(this, (Integer) prefMap.get("start_time"));
    }
    
    private void showToast(String msg) {
	Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
	toast.show();
    }

    static void schedule(Context context, int setting) {
	AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	
	Intent intent = new Intent(context, EsBackService.class);
	PendingIntent pi = PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
	
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
	Log.d("scheduled.");
    }
}

package jp.ddo.masm11.esback;

import android.app.Service;
import android.os.IBinder;
import android.os.Environment;
import android.content.Intent;
import android.preference.PreferenceManager;

import java.util.Map;
import java.io.File;

public class EsBackService extends Service {
    @Override
    public void onCreate() {
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	if (intent != null) {
	    Thread thread = new Thread(new EsBackThread(
			    Environment.getExternalStorageDirectory(),
			    new File(getFilesDir(), "privkey").toString(),
			    PreferenceManager.getDefaultSharedPreferences(this).getAll()));
	    // 重いので下げておく。
	    thread.setPriority(Thread.MIN_PRIORITY);
	    thread.start();
	}
	return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
}

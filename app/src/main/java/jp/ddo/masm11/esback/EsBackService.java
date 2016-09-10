package jp.ddo.masm11.esback;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;

public class EsBackService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
}

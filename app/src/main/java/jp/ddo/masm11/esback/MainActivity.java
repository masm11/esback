package jp.ddo.masm11.esback;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.app.AlertDialog;
import android.Manifest;

public class MainActivity extends AppCompatActivity
			  implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int REQ_PERMISSION_ON_CREATE = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.init(getExternalCacheDir());
	Log.d("start.");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
	
	if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
	    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
		AlertDialog dialog = new AlertDialog.Builder(this)
			.setMessage("Please grant permission.")
			.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int id) {
				String[] permissions = new String[] {
				    Manifest.permission.READ_EXTERNAL_STORAGE,
				};
				ActivityCompat.requestPermissions(MainActivity.this, permissions, REQ_PERMISSION_ON_CREATE);
			    }
			})
			.create();
		dialog.show();
	    } else {
		// permission がない && 説明不要 => request。
		String[] permissions = new String[] {
		    Manifest.permission.READ_EXTERNAL_STORAGE,
		};
		ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSION_ON_CREATE);
	    }
	}
	
	// EsBackThread.createKeyPair(getFilesDir(), getExternalCacheDir());
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode,
	    @NonNull String[] permissions,
	    @NonNull int[] grantResults) {
	if (requestCode == REQ_PERMISSION_ON_CREATE) {
	}
    }
}

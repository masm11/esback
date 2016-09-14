package jp.ddo.masm11.esback;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.init(getExternalCacheDir());
	Log.d("start.");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
	
	// EsBackThread.createKeyPair(getFilesDir(), getExternalCacheDir());
    }
}

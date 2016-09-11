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
/*
	
	Button btn = (Button) findViewById(R.id.button);
	btn.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View v) {
		Log.d("clicked.");
		Thread thread = new Thread(new EsBackThread(getFilesDir()));
		thread.start();
	    }
	});
*/
	
	// EsBackThread.createKeyPair(getFilesDir(), getExternalCacheDir());
    }
}

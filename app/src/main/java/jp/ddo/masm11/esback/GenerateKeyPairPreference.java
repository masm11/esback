package jp.ddo.masm11.esback;

import android.preference.Preference;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

public class GenerateKeyPairPreference extends Preference {
    public static final String KEYPAIR_INIT = "init";
    public static final String KEYPAIR_FAILED = "failed";
    public static final String KEYPAIR_SUCCESS = "success";
    private String curState;
    
    private class InBackground extends AsyncTask<File, Void, Void> {
	private boolean failed;
	private ProgressDialog pd;
	
	@Override
	protected void onPreExecute() {
	    failed = true;
	    
	    pd = ProgressDialog.show(getContext(), null, "Generating...");
	}
	
	@Override
	protected Void doInBackground(File... args) {
	    Log.d("start.");
	    File privDir = args[0];
	    File pubDir = args[1];
	    try {
		JSch jsch = new JSch();
		KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096);
		keyPair.writePrivateKey(new File(privDir, "privkey").toString(), null);
		keyPair.writePublicKey(new File(pubDir, "pubkey").toString(), null);
		keyPair.dispose();
		failed = false;
	    } catch (IOException e) {
		Log.e(e, "failed to save keypair.");
		failed = true;
	    } catch (JSchException e) {
		Log.e(e, "failed to generate keypair.");
		failed = true;
	    }
	    
	    Log.d("end.");
	    return null;
	}
	
	@Override
	protected void onPostExecute(Void arg) {
	    Log.d("post execute.");
	    pd.dismiss();
	    
	    String newState = failed ? KEYPAIR_FAILED : KEYPAIR_SUCCESS;
	    if (callChangeListener(newState)) {
		curState = newState;
		persistString(newState);
		updateSummary();
	    }
	    
	    AlertDialog dialog = new AlertDialog.Builder(getContext())
		    .setMessage(failed ? "Key pair generation was failed." : "A key pair was successfully generated and saved.")
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    // NOP
			}
		    })
		    .create();
	    dialog.show();
	}
    }
    
    public GenerateKeyPairPreference(Context context, AttributeSet attrs) {
	super(context, attrs);
    }
    
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	if (restorePersistedValue) {
	    // 設定値が保存されている。それを読み出してセット。
	    curState = getPersistedString(KEYPAIR_INIT);
	    Log.d("persistent curState=%s", curState);
	} else {
	    // 設定値がまだない。defaultValue (onGetDefaultValue() で返した値) をセット。
	    curState = (String) defaultValue;
	    Log.d("non-persistent curState=%s", curState);
	    persistString(curState);
	}
	
	updateSummary();
    }
    
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
	return KEYPAIR_INIT;
    }
    
    @Override
    protected void onClick() {
	new InBackground().execute(getContext().getFilesDir(), getContext().getExternalFilesDir(null));
    }
    
    private void updateSummary() {
	File pubkey_file = new File(getContext().getExternalFilesDir(null), "pubkey");
	String pubkey_path = pubkey_file.toString();
	String summary;
	
	switch (curState) {
	default:
	case KEYPAIR_INIT:
	    summary = "Tap here to generate a key pair.";
	    break;
	case KEYPAIR_FAILED:
	    summary = "Latest keypair generation was failed. Tap here to regenerate one.";
	    break;
	case KEYPAIR_SUCCESS:
	    if (pubkey_file.exists()) {
		summary = String.format("The public key was saved as %s. Append its content to your server's ~/.ssh/authorized_keys. Tap here to regenerate a key pair.", pubkey_path);
	    } else {
		summary = "Latest key pair generation was successful. Tap here to regenerate one.";
	    }
	}
	
	setSummary(summary);
    }
}

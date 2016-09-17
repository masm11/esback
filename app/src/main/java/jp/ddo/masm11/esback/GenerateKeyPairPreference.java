package jp.ddo.masm11.esback;

import android.preference.Preference;
import android.content.Context;
import android.content.DialogInterface;
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
	protected void onCancelled(Void arg) {
	    Log.d("cancelled.");
	    pd.dismiss();
	}
	
	@Override
	protected void onPostExecute(Void arg) {
	    Log.d("post execute.");
	    pd.dismiss();
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
/*
	if (!restorePersistedValue) {
	    generateAndSaveKeyPair();
	}
*/
    }
    
    @Override
    protected void onClick() {
	// generateAndSaveKeyPair();
	new InBackground().execute(getContext().getFilesDir(), getContext().getExternalFilesDir(null));
    }
    
    private void generateAndSaveKeyPair() {
	boolean failed;
	
	try {
	    JSch jsch = new JSch();
	    KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096);
	    keyPair.writePrivateKey(new File(getContext().getFilesDir(), "privkey").toString(), null);
	    keyPair.writePublicKey(new File(getContext().getExternalFilesDir(null), "pubkey").toString(), null);
	    keyPair.dispose();
	    failed = false;
	} catch (IOException e) {
	    Log.e(e, "failed to save keypair.");
	    failed = true;
	} catch (JSchException e) {
	    Log.e(e, "failed to generate keypair.");
	    failed = true;
	}
	
	if (!failed)
	    updateSummary();
	
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
    
    private void updateSummary() {
	String pubkey_path = new File(getContext().getExternalFilesDir(null), "pubkey").toString();
	String summary = String.format("The public key is saved as %s. Append its content to your server's ~/.ssh/authorized_keys.", pubkey_path);
	setSummary(summary);
    }
}

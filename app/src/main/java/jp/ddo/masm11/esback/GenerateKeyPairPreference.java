package jp.ddo.masm11.esback;

import android.preference.Preference;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.app.AlertDialog;

import java.io.File;
import java.io.IOException;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

public class GenerateKeyPairPreference extends Preference {
    public GenerateKeyPairPreference(Context context, AttributeSet attrs) {
	super(context, attrs);
    }
    
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	if (!restorePersistedValue) {
	    generateAndSaveKeyPair();
	}
    }
    
    @Override
    protected void onClick() {
	generateAndSaveKeyPair();
    }
    
    private void generateAndSaveKeyPair() {
	boolean failed;
	try {
	    JSch jsch = new JSch();
	    KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
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

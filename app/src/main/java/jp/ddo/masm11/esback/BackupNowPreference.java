package jp.ddo.masm11.esback;

import android.preference.Preference;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.AttributeSet;
import android.app.AlertDialog;

public class BackupNowPreference extends Preference {
    public BackupNowPreference(Context context, AttributeSet attrs) {
	super(context, attrs);
    }
    
    @Override
    protected void onClick() {
	AlertDialog dialog = new AlertDialog.Builder(getContext())
		.setMessage("Backup now?")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			Intent intent = new Intent(getContext(), EsBackService.class);
			getContext().startService(intent);
		    }
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			// NOP
		    }
		})
		.create();
	dialog.show();
    }
}

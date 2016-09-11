package jp.ddo.masm11.esback;

import android.preference.Preference;
import android.preference.DialogPreference;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.app.AlertDialog;

public class TimePickerPreference extends DialogPreference {
    private static int DEFAULT_VALUE = 0;
    private int curVal;
    private int newVal;
    
    public TimePickerPreference(Context context, AttributeSet attrs) {
	super(context, attrs);
	
	setDialogLayoutResource(R.layout.timepicker_dialog);
	setPositiveButtonText(android.R.string.ok);
	setNegativeButtonText(android.R.string.cancel);
	setDialogIcon(null);
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
	if (positiveResult)
	    persistInt(newVal);
    }
    
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	if (restorePersistedValue) {
	    curVal = getPersistedInt(DEFAULT_VALUE);
	} else {
	    curVal = (Integer) defaultValue;
	    persistInt(curVal);
	}
    }
    
/*
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
	return a.getInteger(index, DEFAULT_VALUE);
    }
*/
}

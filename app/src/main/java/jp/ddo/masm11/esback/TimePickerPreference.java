package jp.ddo.masm11.esback;

import android.preference.Preference;
import android.preference.DialogPreference;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.app.AlertDialog;
import android.widget.TimePicker;
import android.view.View;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimePickerPreference extends DialogPreference {
    private static final int DEFAULT_VALUE = 1;
    private static final Pattern pattern = Pattern.compile("(\\d+):(\\d+)");
    
    private TimePicker timePicker;
    private int curVal;
    
    public TimePickerPreference(Context context, AttributeSet attrs) {
	super(context, attrs);
	
	setDialogLayoutResource(R.layout.timepicker_dialog);
	setPositiveButtonText(android.R.string.ok);
	setNegativeButtonText(android.R.string.cancel);
	setDialogIcon(null);
    }
    
    @Override
    protected View onCreateDialogView() {
	timePicker = (TimePicker) super.onCreateDialogView();
	timePicker.setIs24HourView(true);
	Log.d("curVal=%d", curVal);
	timePicker.setHour(curVal / 60 % 24);
	timePicker.setMinute(curVal % 60);
	return timePicker;
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
	if (positiveResult) {
	    curVal = timePicker.getHour() * 60 + timePicker.getMinute();
	    Log.d("curVal=%d", curVal);
	    persistInt(curVal);
	}
    }
    
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	if (restorePersistedValue) {
	    // 設定値が保存されている。それを読み出してセット。
	    curVal = getPersistedInt(DEFAULT_VALUE);
	    Log.d("persistent curVal=%d", curVal);
	} else {
	    // 設定値がまだない。defaultValue (onGetDefaultValue() で返した値) をセット。
	    curVal = (Integer) defaultValue;
	    Log.d("non-persistent curVal=%d", curVal);
	    persistInt(curVal);
	}
    }
    
    /* Preference の constructor で、
     * android:defaultValue を見つけた時に呼ばれる。
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
	String str = a.getString(index);
	Matcher m = pattern.matcher(str);
	if (m.matches()) {
	    String hh = m.group(1);
	    String mm = m.group(2);
	    Log.d("hh=%s", hh);
	    Log.d("mm=%s", mm);
	    int hour = Integer.parseInt(hh);
	    int minute = Integer.parseInt(mm);
	    Log.d("ret=%d", hour * 60 + minute);
	    return hour * 60 + minute;
	} else {
	    Log.w("not match.");
	    return null;
	}
    }
}

package jp.ddo.masm11.esback;

import android.preference.PreferenceFragment;
import android.preference.EditTextPreference;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.os.Bundle;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class PrefFragDestination extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d("");
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.pref_frag_destination);
	
	PreferenceManager manager = getPreferenceManager();
	PreferenceCategory cat = (PreferenceCategory) manager.findPreference("dirs");
	addDirs(cat);
    }
    
    private void addDirs(PreferenceCategory cat) {
	try {
	    File[] paths = new File("/sdcard/").listFiles();
	    if (paths == null)	// permission がない状態では null になる。
		return;
	    String[] dirs = new String[paths.length];
	    for (int i = 0; i < paths.length; i++)
		dirs[i] = paths[i].toString().replaceAll("^.*/", "");
	    Arrays.sort(dirs, new Comparator<String>() {
		public int compare(String o1, String o2) {
		    return o1.compareToIgnoreCase(o2);
		}
	    });
	    
	    for (String dir: dirs) {
		SwitchPreference pref = new SwitchPreference(getContext());
		pref.setKey("dir_" + dir);
		pref.setTitle(dir);
		cat.addPreference(pref);
	    }
	} catch (Throwable e) {
	    Log.e(e, "error...");
	}
    }
}

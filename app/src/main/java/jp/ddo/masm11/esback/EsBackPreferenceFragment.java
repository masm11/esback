package jp.ddo.masm11.esback;

import android.preference.PreferenceFragment;
import android.preference.EditTextPreference;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
import java.util.ArrayList;
import java.text.DateFormat;

public class EsBackPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d("");
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.preference_fragment);
	
	PreferenceManager manager = getPreferenceManager();
	PreferenceCategory cat = (PreferenceCategory) manager.findPreference("dirs");
	addDirs(cat);
	
	for (String key: new String[] { "hostname", "username", "directory", "essid" }) {
	    EditTextPreference etp;
	    etp = (EditTextPreference) findPreference(key);
	    etp.setSummary(etp.getText());
	    etp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference pref, Object val) {
		    pref.setSummary(val.toString());
		    return true;
		}
	    });
	}
	
	TimePickerPreference tpp = (TimePickerPreference) findPreference("start_time");
	tpp.setSummary(tpp.getDisplayString());
	tpp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
	    @Override
	    public boolean onPreferenceChange(Preference pref, Object val) {
		((TimePickerPreference) pref).setSummary(TimePickerPreference.getDisplayString((Integer) val));
		EsBackService.schedule(getContext(), (Integer) val);
		return true;
	    }
	});
    }
    
    private void addDirs(PreferenceCategory cat) {
	ArrayList<Directory> dirs = listDirectory();
	for (Directory dir: dirs) {
	    SwitchPreference pref = new SwitchPreference(getContext());
	    pref.setKey(dir.key);
	    pref.setTitle(dir.display_name);
	    cat.addPreference(pref);
	}
    }
    
    public static class Directory {
	String path_to;		// /storage/[path_to]/, or null if internal.
	String name;
	String key;		// key in preferences.
	String display_name;
    }
    public static ArrayList<Directory> listDirectory() {
	ArrayList<Directory> dirs = new ArrayList<Directory>();
	
	listOneStorageDirectory(null, dirs);
	
	File[] storages = new File("/storage").listFiles();
	if (storages != null) {
	    for (File storage: storages) {
		if (storage.getName().equals("emulated"))
		    continue;
		if (storage.getName().equals("self"))
		    continue;
		listOneStorageDirectory(storage.getName(), dirs);
	    }
	}
	
	Collections.sort(dirs, new Comparator<Directory>() {
	    public int compare(Directory o1, Directory o2) {
		return o1.display_name.compareToIgnoreCase(o2.display_name);
	    }
	});
	
	for (Directory dir: dirs) {
	    android.util.Log.d("EsBackPreferenceFragment", "key=" + dir.key + ", name=" + dir.name);
	}
	return dirs;
    }
    private static void listOneStorageDirectory(String path_to, ArrayList<Directory> result) {
	File path;
	if (path_to == null)
	    path = Environment.getExternalStorageDirectory();
	else
	    path = new File("/storage/" + path_to);
	File[] paths = path.listFiles();
	if (paths == null)	// permission がない状態では null になる。
	    return;
	
	for (int i = 0; i < paths.length; i++) {
	    Directory dir = new Directory();
	    dir.path_to = path_to;
	    dir.name = paths[i].getName();
	    dir.key = "dir_" + (dir.path_to == null ? "(internal)" : dir.path_to) + "_" + dir.name;
	    if (path_to == null)
		dir.display_name = "(internal)/" + dir.name;
	    else
		dir.display_name = path_to + "/" + dir.name;
	    result.add(dir);
	}
    }
}

package jp.ddo.masm11.esback;

import android.preference.PreferenceFragment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.os.Bundle;

public class PrefFragDestination extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d("");
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.pref_frag_destination);
    }
}

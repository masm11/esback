package jp.ddo.masm11.esback;

import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.KeyPair;

public class EsBackThread implements Runnable {
    
    private void sendFileTo(File topDir, String relPath, ZipOutputStream zip)
	    throws IOException {
	Log.d("relPath=%s", relPath);
	File file = new File(topDir, relPath);
	
	if (file.isDirectory()) {
	    File[] files = file.listFiles();
	    if (files == null) {
		Log.w("%s: can't access.", file.toString());
		return;
	    }
	    for (File f: files)
		sendFileTo(topDir, relPath + "/" + f.getName(), zip);
	} else {
	    FileInputStream fis = new FileInputStream(file);
	    
	    zip.putNextEntry(new ZipEntry(relPath));
	    byte[] buf = new byte[1024];
	    while (true) {
		int s = fis.read(buf);
		if (s == -1)
		    break;
		zip.write(buf, 0, s);
	    }
	    zip.closeEntry();
	    
	    fis.close();
	}
    }
    
    private void sendTreeTo(File topDir, ZipOutputStream zip)
	    throws IOException {
	File[] files = topDir.listFiles();
	if (files == null) {
	    Log.w("listFiles() failed.");
	    return;
	}
	
	for (File file: files) {
	    String name = file.getName();
	    Boolean onoff = (Boolean) pref.get("dir_" + name);
	    if (onoff != null && onoff) {
		Log.d("%s: on", name);
		sendFileTo(topDir, name, zip);
	    } else {
		Log.d("%s: off", name);
	    }
	}
    }
    
    private final File topDir;
    private final String privKeyFile;
    private final Map<String, ?> pref;
    
    EsBackThread(File topDir, String privKeyFile, Map<String, ?> pref) {
	this.topDir = topDir;
	this.privKeyFile = privKeyFile;
	this.pref = pref;
    }
    
    public void run() {
	Log.d("start.");
	try {
	    JSch jsch = new JSch();
	    
	    jsch.addIdentity(privKeyFile, (String) null);
	    
	    String host = (String) pref.get("hostname");
	    String user = (String) pref.get("username");
	    Log.d("host=%s", host);
	    Log.d("user=%s", user);
	    
	    Session session = jsch.getSession(user, host, 22);
	    UserInfo ui = new EsUserInfo();
	    session.setUserInfo(ui);
	    session.connect();
	    
	    ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
	    channel.connect();
	    
	    OutputStream os = channel.put("/home/backup/android/android-test.zip");
	    ZipOutputStream zip = new ZipOutputStream(os);
	    
	    sendTreeTo(topDir, zip);
	    
	    zip.close();
	    channel.exit();
	    session.disconnect();
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	Log.d("end.");
    }
    
    private class EsUserInfo implements UserInfo {
	public String getPassphrase() {
	    return null;
	}
	public String getPassword() {
	    return null;
	}
	public boolean promptPassword(String message) {
	    Log.i("%s", message);
	    return true;
	}
	public boolean promptPassphrase(String message) {
	    Log.i("%s", message);
	    return true;
	}
	public boolean promptYesNo(String message) {
	    Log.i("%s", message);
	    return true;
	}
	public void showMessage(String message) {
	    Log.i("%s", message);
	}
    }
}

package jp.ddo.masm11.esback;

import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.KeyPair;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

public class EsBackThread implements Runnable {
    
    public interface FinishListener {
	void onFinished();
    }
    
    private int min(long a, long b) {
	return (int) (a < b ? a : b);
    }
    
    private void sendFileTo(File topDir, String relPath, TarArchiveOutputStream tar)
	    throws IOException {
	Log.d("relPath=%s", relPath);
	File file = new File(topDir, relPath);
	
	if (file.isDirectory()) {
	    TarArchiveEntry e = (TarArchiveEntry) tar.createArchiveEntry(file, relPath);
	    tar.putArchiveEntry(e);
	    tar.closeArchiveEntry();
	    
	    File[] files = file.listFiles();
	    if (files == null) {
		Log.w("%s: can't access.", file.toString());
		return;
	    }
	    for (File f: files)
		sendFileTo(topDir, relPath + "/" + f.getName(), tar);
	} else {
	    FileInputStream fis = new FileInputStream(file);
	    
	    TarArchiveEntry e = (TarArchiveEntry) tar.createArchiveEntry(file, relPath);
	    tar.putArchiveEntry(e);
	    
	    long fileSize = e.getSize();
	    long writtenSize = 0;
	    
	    byte[] buf = new byte[1024];
	    while (writtenSize < fileSize) {
		int s = min(fileSize - writtenSize, buf.length);
		s = fis.read(buf, 0, s);
		if (s == -1)
		    break;
		tar.write(buf, 0, s);
		writtenSize += s;
	    }
	    
	    // read 中に file が小さくなった場合の処理
	    byte[] buf0 = new byte[1024];
	    while (writtenSize < fileSize) {
		int s = min(fileSize - writtenSize, buf0.length);
		tar.write(buf0, 0, s);
		writtenSize += s;
	    }
	    
	    tar.closeArchiveEntry();
	    
	    fis.close();
	}
    }
    
    private void sendTreeTo(File topDir, TarArchiveOutputStream tar)
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
		sendFileTo(topDir, name, tar);
	    } else {
		Log.d("%s: off", name);
	    }
	}
    }
    
    private final FinishListener finishListener;
    private final File topDir;
    private final String privKeyFile;
    private final Map<String, ?> pref;
    
    EsBackThread(File topDir, String privKeyFile, Map<String, ?> pref, FinishListener finishListener) {
	this.topDir = topDir;
	this.privKeyFile = privKeyFile;
	this.pref = pref;
	this.finishListener = finishListener;
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
	    
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
	    String bkupPath = String.format("%s/android-%s.tar.gz",
		    pref.get("directory"), sdf.format(new Date()));
	    
	    TarArchiveOutputStream tar =
		    new TarArchiveOutputStream(
			    new GzipCompressorOutputStream(
				    channel.put(bkupPath)
				));
	    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
	    
	    sendTreeTo(topDir, tar);
	    
	    tar.close();
	    channel.exit();
	    session.disconnect();
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
	
	finishListener.onFinished();
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

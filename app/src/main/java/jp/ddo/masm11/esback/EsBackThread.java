package jp.ddo.masm11.esback;

import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import javax.net.ssl.HttpsURLConnection;

public class EsBackThread implements Runnable {
    private static final String URL_BASE = "http://mike/esback/";
    
    public interface ProgressListener {
	void onProgress(long cur, long max);
	void onError(Throwable e);
	void onFinished();
    }
    
    private int min(long a, long b) {
	return (int) (a < b ? a : b);
    }
    
    private long lastBackupTime;
    private HttpCookie cookie;
    private long curBytes, maxBytes;
    
    private void sendFileTo(File topDir, String prefix, String relPath, boolean scanning)
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
		sendFileTo(topDir, prefix, relPath + "/" + f.getName(), scanning);
	} else {
	    if (scanning)
		curBytes += file.length();
	    else {
		long lastModified = file.lastModified();
		if (lastModified < lastBackupTime) {
		    try {
			URL url = new URL(URL_BASE + "file/" + prefix + "/" + relPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Cookie", cookie.toString());
			conn.setRequestProperty("Content-Type", "application/binary");
			conn.connect();
			
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
			    throw new RuntimeException("file not ok.");
		    } catch (Exception e) {
			Log.e(e, "error");
		    }
		    curBytes += file.length();
		} else {
		    FileInputStream fis = new FileInputStream(file);
		    
		    try {
			URL url = new URL(URL_BASE + "file/" + prefix + "/" + relPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setChunkedStreamingMode(10240);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Cookie", cookie.toString());
			conn.setRequestProperty("Content-Type", "application/binary");
			conn.connect();
			OutputStream os = conn.getOutputStream();
			
			byte[] buf = new byte[1024];
			while (true) {
			    int s = fis.read(buf);
			    if (s == -1)
				break;
			    os.write(buf, 0, s);
			    curBytes += s;
			    progressListener.onProgress(curBytes, maxBytes);
			}
			os.flush();
			
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
			    throw new RuntimeException("file not ok.");
		    } catch (Exception e) {
			Log.e(e, "error");
		    } finally {
			fis.close();
		    }
		}
	    }
	}
    }
    
    private void sendTreeTo(File topDir, boolean scanning)
	    throws IOException {
	ArrayList<EsBackPreferenceFragment.Directory> dirs = EsBackPreferenceFragment.listDirectory();
	if (dirs.isEmpty()) {
	    Log.w("listDirectory() failed.");
	    return;
	}
	
	for (EsBackPreferenceFragment.Directory dir: dirs) {
	    File parent = topDir;
	    if (dir.path_to != null)
		parent = new File("/storage/" + dir.path_to);
	    String name = dir.name;
	    Boolean onoff = (Boolean) pref.get(dir.key);
	    if (onoff != null && onoff) {
		Log.d("%s: on", dir.display_name);
		sendFileTo(parent, dir.path_to == null ? "(internal)" : dir.path_to, name, scanning);
	    } else {
		Log.d("%s: off", dir.display_name);
	    }
	}
    }
    
    private HttpCookie getSession()
	    throws MalformedURLException, IOException {
	URL url = new URL(URL_BASE + "begin");
	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	conn.connect();
	if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
	    throw new RuntimeException("begin not ok.");
	String str = conn.getHeaderField("Set-Cookie");
	if (str == null)
	    throw new RuntimeException("No set-cookie.");
	List<HttpCookie> cookies = HttpCookie.parse(str);
	for (HttpCookie cookie: cookies) {
	    if (cookie.getName().equals("session"))
		return cookie;
	}
	throw new RuntimeException("No session.");
    }
    
    private void finishSession()
	    throws MalformedURLException, IOException {
	URL url = new URL(URL_BASE + "finish");
	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	conn.setRequestProperty("Cookie", cookie.toString());
	conn.connect();
	if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
	    throw new RuntimeException("finish not ok.");
    }
    
    private final ProgressListener progressListener;
    private final File topDir;
    private final Map<String, ?> pref;
    
    EsBackThread(File topDir, Map<String, ?> pref, ProgressListener progressListener, long lastBackupTime) {
	this.topDir = topDir;
	this.pref = pref;
	this.progressListener = progressListener;
	this.lastBackupTime = lastBackupTime;
    }
    
    public void run() {
	Log.d("start.");
	
	try {
	    cookie = getSession();
	    Log.d("cookie=%s", cookie);
	    
	    maxBytes = 0;
	    curBytes = 0;
	    sendTreeTo(topDir, true);
	    
	    maxBytes = curBytes;
	    curBytes = 0;
	    Log.d("maxBytes=%d", maxBytes);
	    sendTreeTo(topDir, false);
	    
	    finishSession();
/*
	    
	    
	    
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
	    
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
	    String bkupPath = String.format("%s/android-%s.tar.gz",
		    pref.get("directory"), sdf.format(new Date()));
	    
	    TarArchiveOutputStream tar =
		    new TarArchiveOutputStream(
			    new GzipCompressorOutputStream(
				    channel.put(bkupPath)
				));
	    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
	    
	    maxBytes = 0;
	    curBytes = 0;
	    sendTreeTo(topDir, null, true);
	    
	    maxBytes = curBytes;
	    curBytes = 0;
	    Log.d("maxBytes=%d", maxBytes);
	    sendTreeTo(topDir, tar, false);
	    
	    tar.close();
	    channel.exit();
	    session.disconnect();
*/
	} catch (Exception e) {
	    Log.e(e, "exception");
	    progressListener.onError(e);
	}
	
	progressListener.onFinished();
	Log.d("end.");
    }
}

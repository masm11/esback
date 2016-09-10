package jp.ddo.masm11.esback;

import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.KeyPair;

public class EsBackThread implements Runnable {
    
    private void sendFileTo(ZipOutputStream zip)
	    throws IOException {
	String path = "/ueventd.qcom.rc";
	FileInputStream fis = new FileInputStream(path);
	
	zip.putNextEntry(new ZipEntry(path));
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
    
    static void createKeyPair(File privDir, File pubDir) {
	try {
	    JSch jsch = new JSch();
	    KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
	    keyPair.writePrivateKey(new File(privDir, "privkey").toString(), null);
	    keyPair.writePublicKey(new File(pubDir, "pubkey").toString(), null);
	    keyPair.dispose();
	} catch (Exception e) {
	    Log.e(e, "exception");
	}
    }
    
    final File privDir;
    EsBackThread(File privDir) {
	this.privDir = privDir;
    }
    
    public void run() {
	Log.d("start.");
	try {
	    JSch jsch = new JSch();
	    
	    jsch.addIdentity(new File(privDir, "privkey").toString(), (String) null);
	    
	    String host = "mike";
	    String user = "masm";
	    
	    Session session = jsch.getSession(user, host, 22);
	    UserInfo ui = new EsUserInfo();
	    session.setUserInfo(ui);
	    session.connect();
	    
	    ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
	    channel.connect();
	    
	    OutputStream os = channel.put("/home/backup/android/android-test.zip");
	    ZipOutputStream zip = new ZipOutputStream(os);
	    
	    sendFileTo(zip);
	    
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

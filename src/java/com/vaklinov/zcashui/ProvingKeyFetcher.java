package com.vaklinov.zcashui;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitorInputStream;
import javax.xml.bind.DatatypeConverter;


/**
 * Fetches the proving key.  Deliberately hardcoded.
 * @author zab
 */
public class ProvingKeyFetcher {
    
    private static final int PROVING_KEY_SIZE = 910173851;
    private static final String SHA256 = "8bc20a7f013b2b58970cddd2e7ea028975c88ae7ceb9259a5344a16bc2c0eef7";
    private static final String pathURL = "https://zcash.dl.mercerweiss.com/sprout-proving.key";
    // TODO: add backups
    
    public void fetchIfMissing(StartupProgressDialog parent) throws IOException {
        try {
            verifyOrFetch(parent);
        } catch (InterruptedIOException iox) {
            JOptionPane.showMessageDialog(parent, "HUSH cannot proceed without a proving key.");
            System.exit(-3);
        }
    }
    
    private void verifyOrFetch(StartupProgressDialog parent) throws IOException {
        File zCashParams = new File(System.getenv("APPDATA") + "/ZcashParams");
        zCashParams = zCashParams.getCanonicalFile();
        
        boolean needsFetch = false;
        if (!zCashParams.exists()) 
        {    
            needsFetch = true;
            zCashParams.mkdirs();
        }
        
        // verifying key is small, always copy it
        File verifyingKeyFile = new File(zCashParams,"sprout-verifying.key");
        FileOutputStream fos = new FileOutputStream(verifyingKeyFile);
        InputStream is = ProvingKeyFetcher.class.getClassLoader().getResourceAsStream("keys/sprout-verifying.key");
        copy(is,fos);
        fos.close();
        is = null;
        
        File provingKeyFile = new File(zCashParams,"sprout-proving.key");
        provingKeyFile = provingKeyFile.getCanonicalFile();
        if (!provingKeyFile.exists()) 
        {
            needsFetch = true;
        } else if (provingKeyFile.length() != PROVING_KEY_SIZE) 
        {
            needsFetch = true;
        } 
        /*
         * We skip proving key verification every start - this is impractical.
         * If the proving key exists and is the correct size, then it should be OK.
        else 
        {
            parent.setProgressText("Verifying proving key...");
            needsFetch = !checkSHA256(provingKeyFile,parent);
        }*/
        
        if (!needsFetch) 
        {
            return;
        }
        
        JOptionPane.showMessageDialog(
        	parent, 
        	"The wallet needs to download the Z cryptographic proving key (approx. 900 MB).\n" + 
        	"This will be done only once. Please be patient... Press OK to continue");
        
        parent.setProgressText("Downloading proving key...");
        provingKeyFile.delete();
        OutputStream os = new BufferedOutputStream(new FileOutputStream(provingKeyFile));
        URL keyURL = new URL(pathURL);
        try 
        {
        	is = keyURL.openStream();
            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent, "Downloading proving key", is);
            pmis.getProgressMonitor().setMaximum(PROVING_KEY_SIZE);
            pmis.getProgressMonitor().setMillisToPopup(10);
            
            copy(pmis,os);
            os.close();
        } finally 
        {
            try { if (is != null) is.close(); } catch (IOException ignore){}
        }
        parent.setProgressText("Verifying downloaded proving key...");
        if (!checkSHA256(provingKeyFile, parent)) 
        {
            JOptionPane.showMessageDialog(parent, "Failed to download proving key properly. Cannot continue!");
            System.exit(-4);
        }
    }
            

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[0x1 << 13];
        int read;
        while ((read = is.read(buf)) >- 0) {
            os.write(buf,0,read);
        }
        os.flush();
    }
    
    private static boolean checkSHA256(File provingKey, Component parent) throws IOException {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new RuntimeException(impossible);
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(provingKey))) {
            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent,"Verifying proving key",is);
            pmis.getProgressMonitor().setMaximum(PROVING_KEY_SIZE);
            pmis.getProgressMonitor().setMillisToPopup(10);
            DigestInputStream dis = new DigestInputStream(pmis, sha256);
            byte [] temp = new byte[0x1 << 13];
            while(dis.read(temp) >= 0);
            byte [] digest = sha256.digest();
            return SHA256.equalsIgnoreCase(DatatypeConverter.printHexBinary(digest));
        }
    }
}

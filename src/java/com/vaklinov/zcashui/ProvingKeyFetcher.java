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
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitorInputStream;

import com.vaklinov.zcashui.OSUtil.OS_TYPE;


/**
 * Fetches the proving key.  Deliberately hardcoded.
 * @author zab
 */
public class ProvingKeyFetcher {
    
    private static final int PROVING_KEY_SIZE = 910173851;
    private static final String SHA256 = "8bc20a7f013b2b58970cddd2e7ea028975c88ae7ceb9259a5344a16bc2c0eef7";
    private static final String pathURL = "https://downloads.horizen.io/file/TrustedSetup/sprout-proving.key";
    private static final int SPROUT_GROTH_SIZE = 725523612;
    private static final String SHA256SG = "b685d700c60328498fbde589c8c7c484c722b788b265b72af448a5bf0ee55b50";
    private static final String pathURLSG = "https://downloads.horizen.io/file/TrustedSetup/sprout-groth16.params";
    private static final int SAPLING_SPEND_SIZE = 47958396;
    private static final String SHA256SS = "8e48ffd23abb3a5fd9c5589204f32d9c31285a04b78096ba40a79b75677efc13";
    private static final String pathURLSS = "https://downloads.horizen.io/file/TrustedSetup/sapling-spend.params";
    // TODO: add backups
    private LanguageUtil langUtil;

    public void fetchIfMissing(StartupProgressDialog parent) throws IOException {
        langUtil = LanguageUtil.instance();
        try {
            verifyOrFetch(parent);
        } catch (InterruptedIOException iox) {
            JOptionPane.showMessageDialog(parent, langUtil.getString("proving.key.fetcher.option.pane.message"));
            System.exit(-3);
        }
    }
    
    private void verifyOrFetch(StartupProgressDialog parent) 
    	throws IOException 
    {
    	OS_TYPE ost = OSUtil.getOSType();
        
    	File zCashParams = null;
        // TODO: isolate getting ZcashParams in a utility method
        if (ost == OS_TYPE.WINDOWS)  
        {
        	zCashParams = new File(System.getenv("APPDATA") + "/ZcashParams");
        } else if (ost == OS_TYPE.MAC_OS)
        {
        	File userHome = new File(System.getProperty("user.home"));
        	zCashParams = new File(userHome, "Library/Application Support/ZcashParams");
        }
        
        zCashParams = zCashParams.getCanonicalFile();
        
        boolean needsFetch = false;
        boolean needsFetchSG = false;
        boolean needsFetchSS = false;
        if (!zCashParams.exists()) 
        {    
            needsFetch = true;
            needsFetchSG = true;
            needsFetchSS = true;
            zCashParams.mkdirs();
        }
        
        // verifying key is small, always copy it
        File verifyingKeyFile = new File(zCashParams,"sprout-verifying.key");
        FileOutputStream fos = new FileOutputStream(verifyingKeyFile);
        InputStream is = ProvingKeyFetcher.class.getClassLoader().getResourceAsStream("keys/sprout-verifying.key");
        copy(is,fos);
        fos.close();
        is = null;

        // sapling output params is small, always copy it
        File saplingOutputFile = new File(zCashParams,"sapling-output.params");
        FileOutputStream fosB = new FileOutputStream(saplingOutputFile);
        InputStream isB = ProvingKeyFetcher.class.getClassLoader().getResourceAsStream("keys/sapling-output.params");
        copy(isB,fosB);
        fosB.close();
        isB = null;
        
        File provingKeyFile = new File(zCashParams,"sprout-proving.key");
        provingKeyFile = provingKeyFile.getCanonicalFile();
        File sproutGrothFile = new File(zCashParams,"sprout-groth16.params");
        sproutGrothFile = sproutGrothFile.getCanonicalFile();
        File saplingSpendFile = new File(zCashParams,"sapling-spend.params");
        saplingSpendFile = saplingSpendFile.getCanonicalFile();
        if (!provingKeyFile.exists()) 
        {
            needsFetch = true;
        } else if (provingKeyFile.length() != PROVING_KEY_SIZE) 
        {
            needsFetch = true;
        } 

        if (!sproutGrothFile.exists()) 
        {
            needsFetchSG = true;
        } else if (sproutGrothFile.length() != SPROUT_GROTH_SIZE) 
        {
            needsFetchSG = true;
        }

        if (!saplingSpendFile.exists()) 
        {
            needsFetchSS = true;
        } else if (saplingSpendFile.length() != SAPLING_SPEND_SIZE) 
        {
            needsFetchSS = true;
        }
        
        /*
         * We skip proving key verification every start - this is impractical.
         * If the proving key exists and is the correct size, then it should be OK.
        else 
        {
            parent.setProgressText("Verifying proving key...");
            needsFetch = !checkSHA256(provingKeyFile,parent);
        }*/
        
        if (!needsFetch && !needsFetchSG && !needsFetchSS) 
        {
            return;
        }
        
        JOptionPane.showMessageDialog(
        	parent, 
        	langUtil.getString("proving.key.fetcher.option.pane.verify.message"));
        
        parent.setProgressText(langUtil.getString("proving.key.fetcher.option.pane.verify.progress.text"));
        if (needsFetch) {
        provingKeyFile.delete();
        OutputStream os = new BufferedOutputStream(new FileOutputStream(provingKeyFile));
        URL keyURL = new URL(pathURL);
        URLConnection urlc = keyURL.openConnection();
        urlc.setRequestProperty("User-Agent", "Wget/1.17.1 (linux-gnu)");        
        
        try 
        {
        	is = urlc.getInputStream();
            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent, langUtil.getString("proving.key.fetcher.option.pane.verify.progress.monitor.text"), is);
            pmis.getProgressMonitor().setMaximum(PROVING_KEY_SIZE);
            pmis.getProgressMonitor().setMillisToPopup(10);
            
            copy(pmis,os);
            os.close();
        } finally 
        {
            try { if (is != null) is.close(); } catch (IOException ignore){}
        }
        parent.setProgressText(langUtil.getString("proving.key.fetcher.option.pane.verify.key.text"));
        if (!checkSHA256(provingKeyFile, parent)) 
        {
            JOptionPane.showMessageDialog(parent, langUtil.getString("proving.key.fetcher.option.pane.verify.key.failed.text"));
            System.exit(-4);
        }
        }
        if (needsFetchSG) {
        sproutGrothFile.delete();
        OutputStream os = new BufferedOutputStream(new FileOutputStream(sproutGrothFile));
        URL keyURL = new URL(pathURLSG);
        URLConnection urlc = keyURL.openConnection();
        urlc.setRequestProperty("User-Agent", "Wget/1.17.1 (linux-gnu)");        
        
        try 
        {
        	is = urlc.getInputStream();
            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent, langUtil.getString("sprout.groth.fetcher.option.pane.verify.progress.monitor.text"), is);
            pmis.getProgressMonitor().setMaximum(SPROUT_GROTH_SIZE);
            pmis.getProgressMonitor().setMillisToPopup(10);
            
            copy(pmis,os);
            os.close();
        } finally 
        {
            try { if (is != null) is.close(); } catch (IOException ignore){}
        }
        parent.setProgressText(langUtil.getString("sprout.groth.fetcher.option.pane.verify.key.text"));
        if (!checkSHA256SG(sproutGrothFile, parent)) 
        {
            JOptionPane.showMessageDialog(parent, langUtil.getString("sapsproutling.groth.fetcher.option.pane.verify.key.failed.text"));
            System.exit(-4);
        }
        }
        if (needsFetchSS) {
        saplingSpendFile.delete();
        OutputStream os = new BufferedOutputStream(new FileOutputStream(saplingSpendFile));
        URL keyURL = new URL(pathURLSS);
        URLConnection urlc = keyURL.openConnection();
        urlc.setRequestProperty("User-Agent", "Wget/1.17.1 (linux-gnu)");        
        
        try 
        {
        	is = urlc.getInputStream();
            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent, langUtil.getString("sapling.spend.fetcher.option.pane.verify.progress.monitor.text"), is);
            pmis.getProgressMonitor().setMaximum(SAPLING_SPEND_SIZE);
            pmis.getProgressMonitor().setMillisToPopup(10);
            
            copy(pmis,os);
            os.close();
        } finally 
        {
            try { if (is != null) is.close(); } catch (IOException ignore){}
        }
        parent.setProgressText(langUtil.getString("sapling.spend.fetcher.option.pane.verify.key.text"));
        if (!checkSHA256SS(saplingSpendFile, parent)) 
        {
            JOptionPane.showMessageDialog(parent, langUtil.getString("sapling.spend.fetcher.option.pane.verify.key.failed.text"));
            System.exit(-4);
        }
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
            throw new IOException(impossible);
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(provingKey))) {
            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent,
                    LanguageUtil.instance().getString("proving.key.fetcher.option.pane.verify.progress.monitor.text"),
                    is);
            pmis.getProgressMonitor().setMaximum(PROVING_KEY_SIZE);
            pmis.getProgressMonitor().setMillisToPopup(10);
            DigestInputStream dis = new DigestInputStream(pmis, sha256);
            byte [] temp = new byte[0x1 << 13];
            while(dis.read(temp) >= 0);
            byte [] digest = sha256.digest();

            return SHA256.equalsIgnoreCase(Util.bytesToHex(digest));
        }
    }

    private static boolean checkSHA256SG(File sproutGroth, Component parent) throws IOException {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException(impossible);
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(sproutGroth))) {
            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent,
                    LanguageUtil.instance().getString("sprout.groth.fetcher.option.pane.verify.progress.monitor.text"),
                    is);
            pmis.getProgressMonitor().setMaximum(SPROUT_GROTH_SIZE);
            pmis.getProgressMonitor().setMillisToPopup(10);
            DigestInputStream dis = new DigestInputStream(pmis, sha256);
            byte [] temp = new byte[0x1 << 13];
            while(dis.read(temp) >= 0);
            byte [] digest = sha256.digest();
            
            return SHA256SG.equalsIgnoreCase(Util.bytesToHex(digest));
        }
    }

    private static boolean checkSHA256SS(File saplingSpend, Component parent) throws IOException {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException(impossible);
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(saplingSpend))) {
            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent,
                    LanguageUtil.instance().getString("sapling.spend.fetcher.option.pane.verify.progress.monitor.text"),
                    is);
            pmis.getProgressMonitor().setMaximum(SAPLING_SPEND_SIZE);
            pmis.getProgressMonitor().setMillisToPopup(10);
            DigestInputStream dis = new DigestInputStream(pmis, sha256);
            byte [] temp = new byte[0x1 << 13];
            while(dis.read(temp) >= 0);
            byte [] digest = sha256.digest();

            return SHA256SS.equalsIgnoreCase(Util.bytesToHex(digest));
        }
    }
}

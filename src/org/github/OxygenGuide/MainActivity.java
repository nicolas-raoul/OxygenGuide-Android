package org.github.OxygenGuide;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Main activity of OxygenGuide, shows a menu with available actions.
 */
public class MainActivity extends Activity {

    public static final String TAG = "OxygenGuide";

    private static final String PATH = "/sdcard/OxygenGuide";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void download(View view) {
        Log.d(TAG, "Entering download method.");

        // Find out latest release
        String latestVersion = "OxygenGuide_2013-01-12-a"; // TODO read from http://code.google.com/p/oxygenguide/downloads/list

        // Delete files if exist already.
        deleteIfExists(PATH);
        deleteIfExists("/sdcard/" + latestVersion);
        deleteIfExists(PATH + ".zip");

        // Check available space on SD card.
        // Difference found by "adb shell df" on my SD card before/after: 917568K
        // Not sure why it takes so much space, maybe because of the 32K blocks.
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdAvailSize = (double)statFs.getAvailableBlocks()
                * (double)statFs.getBlockSize();
         if (sdAvailSize < 1000*1048576) {
             TextView message=(TextView)findViewById(R.id.message);
             message.setText("Insufficient space on SD card, needs 1GB");
             return;
         }

        // Download.
        try {
            downloadFile("http://oxygenguide.googlecode.com/files/" + latestVersion + ".zip", "/sdcard/OxygenGuide.zip");
            Log.d(TAG, "Download complete.");
        }
        catch(Exception e) {
            Toast.makeText(this, "Problem downloading, try later", Toast.LENGTH_LONG).show();
            Log.e(TAG, e.getMessage());
            return;
        }

        // Unzip.
        new UnZipper("/sdcard/OxygenGuide.zip", "/sdcard/").unzip();
        Log.d(TAG, "Unzipped.");

        // Rename.
        boolean result = new File("/sdcard/" + latestVersion).renameTo(new File(PATH));
        Log.d(TAG, "Renaming result:" + result);

        // Store OxygenGuide data version.
        try {
            FileWriter writer = new FileWriter(new File(PATH + "/OxygenGuide-version.txt"));
            writer.append(latestVersion);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error writing version");
        }

        // Delete ZIP file.
        deleteRecursive(new File(PATH + ".zip"));
        Log.d(TAG, "Deleted ZIP file");
    }

    void deleteIfExists(String path) {
        if(new File(path).exists())
            deleteRecursive(new File(path));
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    /**
     * Downloads a remote file and stores it locally
     * @param from Remote URL of the file to download
     * @param to Local path where to store the file
     * @throws Exception Read/write exception
     */
    static private void downloadFile(String from, String to) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)new URL(from).openConnection();
        conn.setDoInput(true);
        conn.setConnectTimeout(10000); // timeout 10 secs
        conn.connect();
        InputStream input = conn.getInputStream();
        FileOutputStream fOut = new FileOutputStream(to);
        int byteCount = 0;
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = input.read(buffer)) != -1) {
            fOut.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }
        fOut.flush();
        fOut.close();
    }

    public void launchHtmlViewer(View view) {
        Toast.makeText(this, "Launching HtmlViewer", Toast.LENGTH_SHORT).show();
        String url = "content://com.android.htmlfileprovider" + PATH + "/index.html";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "text/html");
        startActivity(intent);
    }

    public void launchAndroidBrowser(View view) {
        Toast.makeText(this, "Launching Android Browser", Toast.LENGTH_SHORT).show();
        String url = "content://com.android.htmlfileprovider\" + PATH + \"/index.html";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "text/html");
        intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
        startActivity(intent);
    }

    public void checkUpdate(View view) {
        Toast.makeText(this, "Check update (Not implemented, check manually)", Toast.LENGTH_SHORT).show();
    }
}
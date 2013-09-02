package org.github.OxygenGuide;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity of OxygenGuide, shows a menu with available actions.
 */
public class MainActivity extends Activity {

	public static final String TAG = "OxygenGuide";
    // Find out latest release
    // TODO read from http://code.google.com/p/oxygenguide/downloads/list
    // TODO 2: google code are canceling their hosting services....
    private static final String downURL = "http://oxygenguide.googlecode.com/files/OxygenGuide_2013-08-14-a.zip";

	private  static String zipPATH = Environment.getExternalStorageDirectory().getPath() + "/OxygenGuide.zip";

    private static final String AUTHORITY = "org.github.OxygenGuide";

	TextView mDownloadText;
	ProgressDialog mProgressDialog;
	private String progressMessage = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
        Intent intent = getIntent();
        if(Intent.ACTION_VIEW.equals(intent.getAction())){
            Log.v(TAG, "opened with intent to view");
            // replace the path if opened with a given zip
            zipPATH = intent.getData().toString();
        }
        else{
            setContentView(R.layout.main);
            
            mDownloadText = (TextView) findViewById(R.id.download_text);

            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage("Removing previous versions of OxygenGuide...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }
	}

	// /////////////////////////////////////////////////////////////////////////////////////
	// / UI callbacks
	// /////////////////////////////////////////////////////////////////////////////////////

	public void download(View view) {
		Log.d(TAG, "Entering download method.");
		DownloadFile downloadFile = new DownloadFile();
		downloadFile.execute();
	}

	public void launchHtmlViewer(View view) {
		Toast.makeText(this, "Launching HtmlViewer", Toast.LENGTH_SHORT).show();
		Uri url = getIndexUri();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(url);
		startActivity(intent);
	}

	public void launchAndroidBrowser(View view) {
		Toast.makeText(this, "Launching Android Browser", Toast.LENGTH_SHORT).show();
        Uri url = getIndexUri();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(url, "text/html");
		intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
		startActivity(intent);
	}

	public void checkUpdate(View view) {
		Toast.makeText(this, "Check update (Not implemented, check manually)",
				Toast.LENGTH_SHORT).show();
        
	}

	public Runnable updateProgressMessage = new Runnable() {
		@Override
		public void run() {
			mProgressDialog.setMessage(progressMessage);
		}
	};

	// /////////////////////////////////////////////////////////////////////////////////////
	// / Utilities
	// /////////////////////////////////////////////////////////////////////////////////////

    
    public static Uri getIndexUri ( ) {
        //now handle any zip top level directory, hoping it includes index.html
        // actually reads only first entry in zip, assuming it is the top directory.
        try{
        ZipFile zf = new ZipFile(zipPATH);
        String TopDir =  zf.entries().nextElement().toString();
        Log.v(TAG, "Found top level zip dir : " +TopDir);
        zf.close();
        return Uri.parse("content://" + AUTHORITY +"/"+ TopDir + "index.html");
        
    } catch (Exception e) {
				Log.e(TAG, "Exception:" + e.getMessage());
                return null;
			}
    
        //return Uri.parse("content://" + AUTHORITY );
    }
	// /////////////////////////////////////////////////////////////////////////////////////
	// / Download async task
	// /////////////////////////////////////////////////////////////////////////////////////

	private class DownloadFile extends AsyncTask<String, Integer, String> {

		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog.show();
		}
		
		@Override
        protected void onPostExecute(String unused) {
			mProgressDialog.dismiss();
			mDownloadText.setVisibility(View.VISIBLE);
        }

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);
		}

		@Override
		protected String doInBackground(String... sUrlNOTUSED) {
			try {
				
				// Delete files if exist already.
                if (new File(zipPATH).exists()){
				new File(zipPATH).delete();
            }
				publishProgress(10);

				progressMessage = "Downloading OxygenGuide ZIP of HTML pages...";
				runOnUiThread(updateProgressMessage);


				// /////////////////////////////////////// Download
				URL url = new URL(downURL);
				URLConnection connection = url.openConnection();
				connection.setDoInput(true);
				connection.setConnectTimeout(10000); // timeout 10 seconds
				connection.connect();
				// File length to calculate progress.
				int fileLength = connection.getContentLength();

				// Download the ZIP.
				InputStream input = new BufferedInputStream(url.openStream());
				OutputStream output =
					new FileOutputStream(zipPATH);

				byte buffer[] = new byte[4096];
				long total = 0;
				int count;
				while ((count = input.read(buffer)) != -1) {
					total += count;
					// Progress from 0% to 100%.
					publishProgress((int) ( total * 100 / fileLength));
					output.write(buffer, 0, count);
				}

				output.flush();
				output.close();
				input.close();
				// //////////////////////////////////////////// Download end
			} catch (Exception e) {
				Log.e(TAG, "Exception:" + e.getMessage());
			}
			return null;
		}

		
	}
}

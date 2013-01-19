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

	private static final String PATH = "/sdcard/OxygenGuide";

	TextView mDownloadText;
	ProgressDialog mProgressDialog;
	private String progressMessage = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mDownloadText = (TextView) findViewById(R.id.download_text);

		mProgressDialog = new ProgressDialog(MainActivity.this);
		mProgressDialog.setMessage("Removing previous versions of OxygenGuide...");
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setMax(100);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
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
		String url = "content://com.android.htmlfileprovider" + PATH + "/index.html";
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(url), "text/html");
		startActivity(intent);
	}

	public void launchAndroidBrowser(View view) {
		Toast.makeText(this, "Launching Android Browser", Toast.LENGTH_SHORT).show();
		String url = "file:/" + PATH + "/index.html";
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(url), "text/html");
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

	void deleteIfExists(String path) {
		if (new File(path).exists())
			deleteRecursive(new File(path));
	}

	void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);
		fileOrDirectory.delete();
	}

	// /////////////////////////////////////////////////////////////////////////////////////
	// / Download async task
	// /////////////////////////////////////////////////////////////////////////////////////

	private class DownloadFile extends AsyncTask<String, Integer, String> {

		/**
		 * For the progress dialog, keep track of the number of unzipped directories.
		 */
		private int zipDirsCounter = 0;

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
				// Find out latest release
				// TODO read from http://code.google.com/p/oxygenguide/downloads/list
				String latestVersion = "OxygenGuide_2013-01-12-a";

				// Delete files if exist already.
				deleteIfExists(PATH + ".zip");
				publishProgress(2); // 2%
				deleteIfExists("/sdcard/" + latestVersion);
				publishProgress(6);
				deleteIfExists(PATH);
				publishProgress(10);

				progressMessage = "Downloading OxygenGuide ZIP of HTML pages...";
				runOnUiThread(updateProgressMessage);

				// Check available space on SD card.
				// Difference found by "adb shell df" on my SD card
				// before/after: 917568K
				// Not sure why it takes so much space, maybe because of the 32K
				// blocks.
				StatFs statFs = new StatFs(
					Environment.getExternalStorageDirectory().getPath());
				double sdAvailSize = (double) statFs.getAvailableBlocks()
						* (double) statFs.getBlockSize();
				if (sdAvailSize < 1000 * 1048576) {
					TextView message = (TextView) findViewById(R.id.message);
					message.setText("Insufficient space on SD card, needs 1GB");
					return "Insufficient space on SD card, needs 1GB";
				}

				// /////////////////////////////////////// Download
				URL url = new URL("http://oxygenguide.googlecode.com/files/"
						+ latestVersion + ".zip");
				URLConnection connection = url.openConnection();
				connection.setDoInput(true);
				connection.setConnectTimeout(10000); // timeout 10 seconds
				connection.connect();
				// File length to calculate progress.
				int fileLength = connection.getContentLength();

				// Download the ZIP.
				InputStream input = new BufferedInputStream(url.openStream());
				OutputStream output =
					new FileOutputStream("/sdcard/OxygenGuide.zip");

				byte buffer[] = new byte[4096];
				long total = 0;
				int count;
				while ((count = input.read(buffer)) != -1) {
					total += count;
					// Progress from 10% to 40%.
					publishProgress((int) (10 + total * 40 / fileLength));
					output.write(buffer, 0, count);
				}

				output.flush();
				output.close();
				input.close();
				// //////////////////////////////////////////// Download end

				progressMessage = "Unzipping OxygenGuide HTML pages...";
				runOnUiThread(updateProgressMessage);

				// Unzip.
				unzip("/sdcard/OxygenGuide.zip", "/sdcard/");
				Log.d(TAG, "Unzipped.");

				progressMessage = "Finishing...";
				runOnUiThread(updateProgressMessage);

				// Rename.
				boolean result = new File("/sdcard/" + latestVersion)
						.renameTo(new File(PATH));
				Log.d(TAG, "Renaming result:" + result);

				// Store OxygenGuide data version.
				try {
					FileWriter writer = new FileWriter(new File(PATH
							+ "/OxygenGuide-version.txt"));
					writer.append(latestVersion);
					writer.flush();
					writer.close();
				} catch (IOException e) {
					Log.e(TAG, "Error writing version");
				}

				// Delete ZIP file.
				deleteRecursive(new File(PATH + ".zip"));
				Log.d(TAG, "Deleted ZIP file");
				
			} catch (Exception e) {
				Log.e(TAG, "Exception:" + e.getMessage());
			}
			return null;
		}

		// /////////////////////////////////////////////////////////////////////////////////////
		// / Progress-aware utilities
		// /////////////////////////////////////////////////////////////////////////////////////

		public void unzip(String zipPath, String destinationPath) {
			Log.d(TAG, "unzipping " + zipPath + " to " + destinationPath);

			File archive = new File(zipPath);
			try {
				ZipFile zipfile = new ZipFile(archive);
				for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
					ZipEntry entry = (ZipEntry) e.nextElement();
					unzipEntry(zipfile, entry, destinationPath);
				}
			} catch (Exception e) {
				Log.e(TAG, "Error while extracting file " + archive, e);
			}
			Log.d(TAG, "Unzipped " + zipPath);
		}

		private void unzipEntry(ZipFile zipfile, ZipEntry entry,
				String outputDir) throws IOException {

			if (entry.isDirectory()) {
				createDir(new File(outputDir, entry.getName()));
				return;
			}

			File outputFile = new File(outputDir, entry.getName());
			if (!outputFile.getParentFile().exists()) {
				createDir(outputFile.getParentFile());
			}

			BufferedInputStream inputStream =
				new BufferedInputStream(zipfile.getInputStream(entry), 8192);
			BufferedOutputStream outputStream =
				new BufferedOutputStream(new FileOutputStream(outputFile), 8192);

			try {
				IOUtils.copy(inputStream, outputStream);
			} finally {
				outputStream.close();
				inputStream.close();
			}
		}

		private void createDir(File dir) {
			if (dir.exists()) {
				return;
			}
			Log.v(TAG, "Creating dir " + dir.getName());
			// From 50% to 98%. There are around 90 directories in the ZIP.
			publishProgress((int) (50 + zipDirsCounter++ * (98-50) / 90));
			if (!dir.mkdirs()) {
				throw new RuntimeException("Can not create dir " + dir);
			}
		}
	}
}
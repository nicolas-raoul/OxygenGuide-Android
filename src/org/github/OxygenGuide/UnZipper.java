package org.github.OxygenGuide;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Unzip a folder.
 */
public class UnZipper extends Observable {

    private static final String TAG = MainActivity.TAG;

    private String mZipPath, mDestinationPath;

    public UnZipper (String zipPath, String destinationPath) {
        mZipPath = zipPath;
        mDestinationPath = destinationPath;
    }

    public void unzip () {
        Log.d(TAG, "unzipping " + mZipPath + " to " + mDestinationPath);
/*        new UnZipTask().execute(mZipPath, mDestinationPath);
    }

    private class UnZipTask extends AsyncTask<String, Void, Boolean> {

        @SuppressWarnings("rawtypes")
        @Override
        protected Boolean doInBackground(String... params) {
            String filePath = params[0];
            String destinationPath = params[1];
*/
            String filePath = mZipPath;
            String destinationPath = mDestinationPath;

            File archive = new File(filePath);
            try {
                ZipFile zipfile = new ZipFile(archive);
                for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    unzipEntry(zipfile, entry, destinationPath);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while extracting file " + archive, e);
                //return false;
            }
            Log.d(TAG, "Unzipped " + mZipPath);
            //return true;
        }

        /*@Override
        protected void onPostExecute(Boolean result) {
            setChanged();
            notifyObservers();
        }*/

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

            //Log.v(TAG, "Extracting: " + entry);
            BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry), 8192);
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile), 8192);

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
            if (!dir.mkdirs()) {
                throw new RuntimeException("Can not create dir " + dir);
            }
        }
    //}
}
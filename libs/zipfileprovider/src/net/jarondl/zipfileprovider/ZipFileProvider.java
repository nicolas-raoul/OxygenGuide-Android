package net.jarondl.zipfileprovider;
/*
 * Copyright (C) 2013 Yaron de Leeuw   http://jarondl.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.util.Log;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.webkit.MimeTypeMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.lang.NullPointerException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;


import com.android.vending.expansion.zipfile.ZipResourceFile;

/* ZipFileProvider - a content provider for compressed zip files.
 *
 * This allows applications to access files from a zip file seamlessly,
 * by a content uri. The idea came from APEZProvider which is part of
 * the package com.android.vending.expansion.zipfile
 * But APEZProvider is hard coded for application expansion files,
 * and doesn't work for compressed files.
 * This library allows usage with any compressed zip file in the system.
 *
 * some of the code is based on
 * https://github.com/commonsguy/cw-omnibus/blob/master/ContentProvider/Pipe/src/com/commonsware/android/cp/pipe/PipeProvider.java
 * which is released under the apache license. expansion.zipfile is also apache.
 * Therefore this library is also apache licensed to avoid incompatibilities.
 *
 * To use this you have to subclass ("extend") and override getAuthority and ZipPath.
 *
 *
 * Sources:
 * http://stackoverflow.com/questions/9623350/where-is-the-samplezipfileprovider-class
 * http://stackoverflow.com/questions/8589645/how-to-detemine-mime-type-of-file-in-android
 * https://github.com/commonsguy/cw-omnibus/blob/master/ContentProvider/Pipe/src/com/commonsware/android/cp/pipe/PipeProvider.java
*/
public abstract class ZipFileProvider extends ContentProvider {

    /**
     * This needs to match the authority in your manifest
     */
    public abstract String getAuthority();
    /** and this should be the path to your zip file
     */
    public abstract String ZipPath();

    private boolean isZipOpen = false;
    private ZipResourceFile mAPKExtensionFile;

    private static final String TAG = "ZipFileProvider";

    private boolean initIfNecessary() {
        if ( !isZipOpen ) {
            try {
                mAPKExtensionFile = new ZipResourceFile(ZipPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.v(TAG, "opened zip file");
            isZipOpen = true;
            return true;

        }

        return false;
    }
    @Override
    public String getType(Uri uri) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
    throws FileNotFoundException {

        initIfNecessary();
        Log.v(TAG, "openning pipe for: "
              + uri.toString());
        ParcelFileDescriptor[] pipe=null;

        try {
            pipe=ParcelFileDescriptor.createPipe();
            String path = uri.getEncodedPath();
            if ( path.startsWith("/") ) {
                path = path.substring(1);
            }

            InputStream in = mAPKExtensionFile.getInputStream(path);

            new TransferThread(in,
                               new AutoCloseOutputStream(pipe[1])).start();

        }
        catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Exception opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: "
                                            + uri.toString());
        }

        return pipe[0] ;
    }



    static class TransferThread extends Thread {
        InputStream in;
        OutputStream out;

        TransferThread(InputStream in, OutputStream out) {
            this.in=in;
            this.out=out;
        }

        @Override
        public void run() {
            byte[] buf=new byte[1024];
            int len;

            try {
                while ((len=in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
            }
            catch (IOException e) {
                Log.e(getClass().getSimpleName(),
                      "Exception transferring file", e);
            }
            catch (NullPointerException e) {
                Log.e(TAG, "null pointer exception", e);
            }



        }
    }
    // From here on we override all the methods we dont support, because we have to.
    @Override
    public Cursor query(Uri url, String[] projection, String selection,
                        String[] selectionArgs, String sort) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
                      String[] whereArgs) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new RuntimeException("Operation not supported");
    }
    @Override
    public boolean onCreate() {
        return true ;
    }

}




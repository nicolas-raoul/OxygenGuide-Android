package org.github.OxygenGuide;

import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;


import net.jarondl.zipfileprovider.ZipFileProvider;


public class OxygenProvider extends ZipFileProvider {
    @Override
    public String getAuthority(){
        return "org.github.OxygenGuide";
    }
    @Override
    public String ZipPath(){
        return Environment.getExternalStorageDirectory().getPath() + "/OxygenGuide.zip";
    }

}

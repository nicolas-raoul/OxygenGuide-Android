package org.github.OxygenGuide;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void launchIntent(View view) {
        Toast.makeText(this, "Launching intent", Toast.LENGTH_SHORT).show();
        String url = "content://com.android.htmlfileprovider/sdcard/OxygenGuide/index.html";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "text/html");
        startActivity(intent);
    }

    public void launchAndroidBrowser(View view) {
        Toast.makeText(this, "Launching Android Browser", Toast.LENGTH_SHORT).show();
        String url = "content://com.android.htmlfileprovider/sdcard/OxygenGuide/index.html";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "text/html");
        intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
        startActivity(intent);
    }
}

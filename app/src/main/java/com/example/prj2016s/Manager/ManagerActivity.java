package com.example.prj2016s.Manager;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;

import java.io.IOException;

/**
 * Created by Kang on 2016-06-03.
 */
public class ManagerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            try {
                M3u8Manager man = new M3u8Manager();
                man.managing("test.mp4", this);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}

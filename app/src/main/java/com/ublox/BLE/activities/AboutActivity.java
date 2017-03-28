package com.ublox.BLE.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.ublox.BLE.R;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("");
        getActionBar().setLogo(R.drawable.logo);
        getActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_about);

        findViewById(R.id.bVisit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visit();
            }
        });
    }

    private void visit() {
        String url = "http://u-blox.com";
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri u = Uri.parse(url);
        i.setData(u);
        startActivity(i);
    }
}

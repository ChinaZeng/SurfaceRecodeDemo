package com.zzw.live;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                , Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
        }, 5);
    }


    public void camera(View view) {
        startActivity(new Intent(this, CameraActivity.class));
    }

    public void recode(View view) {
        startActivity(new Intent(this, RecodeActivity.class));
    }
}

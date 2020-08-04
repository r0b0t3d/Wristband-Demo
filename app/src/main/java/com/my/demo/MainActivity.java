package com.my.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.my.demo.manager.WbManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WbManager.getInstance(this).registerCallback(new WbManager.WbCallback() {
            @Override
            public void onHearRateUpdate(int hrValue) {
                // Update value to UI
            }

            @Override
            public void onTemperatureValue(float tempValue) {
                // Update value to UI
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        WbManager.getInstance(this).unRegisterCallback();
    }
}
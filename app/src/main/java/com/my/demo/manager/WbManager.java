package com.my.demo.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wosmart.ukprotocollibary.WristbandManager;
import com.wosmart.ukprotocollibary.WristbandManagerCallback;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerHrpItemPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerHrpPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerTemperatureControlPacket;

public class WbManager {
    public static final String TAG = "WbManager";
    private Context context;
    private Handler handler;
    private WbCallback callback;

    private static WbManager instance;

    public static WbManager getInstance(Context context) {
        if (instance == null) {
            instance = new WbManager(context);
        }
        return instance;
    }

    private WbManager(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new HeartRateHandler();
    }

    private WristbandManagerCallback wristbandManagerCallback = new WristbandManagerCallback() {
        @Override
        public void onHrpDataReceiveIndication(ApplicationLayerHrpPacket packet) {
            super.onHrpDataReceiveIndication(packet);
            for (ApplicationLayerHrpItemPacket item : packet.getHrpItems()) {
                Log.i(TAG, "hr value :" + item.getValue());
                callback.onHearRateUpdate(item.getValue());
            }
        }

        @Override
        public void onDeviceCancelSingleHrpRead() {
            super.onDeviceCancelSingleHrpRead();
            Log.i(TAG, "stop measure hr ");
        }

        @Override
        public void onTemperatureData(ApplicationLayerHrpPacket packet) {
            super.onTemperatureData(packet);
            for (ApplicationLayerHrpItemPacket item : packet.getHrpItems()) {
                Log.i(TAG, "temp origin value :" + item.getTempOriginValue() + " temperature adjust value : " + item.getTemperature() + " is wear :" + item.isWearStatus() + " is adjust : " + item.isAdjustStatus() + "is animation :" + item.isAnimationStatus());
                callback.onTemperatureValue(item.getTempOriginValue());
            }
        }

        @Override
        public void onTemperatureMeasureSetting(ApplicationLayerTemperatureControlPacket packet) {
            super.onTemperatureMeasureSetting(packet);
            Log.i(TAG, "temp setting : show = " + packet.isShow() + " adjust = " + packet.isAdjust() + " celsius unit = " + packet.isCelsiusUnit());
        }

        @Override
        public void onTemperatureMeasureStatus(int status) {
            super.onTemperatureMeasureStatus(status);
            Log.i(TAG, "temp status :" + status);
        }
    };

    public void registerCallback(WbCallback cb) {
        this.callback = cb;
        WristbandManager.getInstance(context).registerCallback(wristbandManagerCallback);
        startMeasurement();
    }

    public void unRegisterCallback() {
        this.callback = null;
        WristbandManager.getInstance(context).unRegisterCallback(wristbandManagerCallback);
        stopMeasurement();
    }

    private void startMeasurement() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Start heart rate
                if (WristbandManager.getInstance(context).readHrpValue()) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }

                // Start temperature
                if (WristbandManager.getInstance(context).setTemperatureStatus(true)) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }
            }
        });
        thread.start();
    }

    private void stopMeasurement() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Stop heart rate
                if (WristbandManager.getInstance(context).stopReadHrpValue()) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }
                // Stop temperature
                if (WristbandManager.getInstance(context).setTemperatureStatus(false)) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }
            }
        });
        thread.start();
    }

    private class HeartRateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x01:
                    // Success
                    break;
                case 0x02:
                    // Fail
                    break;
            }
        }
    }

    public interface WbCallback {
        void onHearRateUpdate(int hrValue);
        void onTemperatureValue(float tempValue);
    }
}

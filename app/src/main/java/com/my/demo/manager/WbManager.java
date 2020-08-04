package com.my.demo.manager;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wosmart.ukprotocollibary.WristbandManager;
import com.wosmart.ukprotocollibary.WristbandManagerCallback;
import com.wosmart.ukprotocollibary.WristbandScanCallback;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerHrpItemPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerHrpPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerTemperatureControlPacket;

import java.util.concurrent.atomic.AtomicBoolean;

public class WbManager {
    public static final String TAG = "WbManager";
    private Context context;
    private Handler handler;
    private WbCallback callback;
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    private static WbManager instance;

    public static WbManager getInstance(Context context) {
        if (instance == null) {
            instance = new WbManager(context);
        }
        return instance;
    }

    private WbManager(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new WbHandler();
    }

    public void registerCallback(WbCallback cb) {
        this.callback = cb;
        WristbandManager.getInstance(context).registerCallback(wristbandManagerCallback);
        if (isConnected.get()) {
            startMeasurement();
        } else {
            startScan();
        }

    }

    public void unRegisterCallback() {
        this.callback = null;
        WristbandManager.getInstance(context).unRegisterCallback(wristbandManagerCallback);
        stopMeasurement();
    }

    /**
     * Start scan and connect to the first device found
     */
    private void startScan() {
        WristbandManager.getInstance(context).startScan(new WristbandScanCallback() {
            @Override
            public void onWristbandDeviceFind(BluetoothDevice device, int rssi, byte[] scanRecord) {
                super.onWristbandDeviceFind(device, rssi, scanRecord);
                connectDevice(device.getAddress(), device.getName());
            }

            @Override
            public void onLeScanEnable(boolean enable) {
                super.onLeScanEnable(enable);
            }

            @Override
            public void onWristbandLoginStateChange(boolean connected) {
                super.onWristbandLoginStateChange(connected);
            }
        });
    }

    private void connectDevice(String mac, String name) {
        Log.d(TAG, "Connecting to " + name + ", mac: " + mac);
        WristbandManager.getInstance(context).connect(mac);
    }

    private WristbandManagerCallback wristbandManagerCallback = new WristbandManagerCallback() {
        @Override
        public void onConnectionStateChange(boolean status) {
            super.onConnectionStateChange(status);
            isConnected.set(status);
            if (status) {
                Log.d(TAG, "Connected");
                startMeasurement();
            } else {
                Log.d(TAG, "Failed to connected");
            }
        }

        @Override
        public void onError(int error) {
            super.onError(error);
            Log.e(TAG, "Error: " + error);
        }

        @Override
        public void onHrpDataReceiveIndication(ApplicationLayerHrpPacket packet) {
            super.onHrpDataReceiveIndication(packet);
            for (ApplicationLayerHrpItemPacket item : packet.getHrpItems()) {
                Log.i(TAG, "hr value :" + item.getValue());
                if (callback != null) {
                    callback.onHearRateUpdate(item.getValue());
                }
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
                if (callback != null) {
                    callback.onTemperatureValue(item.getTempOriginValue());
                }
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


    private void startMeasurement() {
        if (!isConnected.get()) return;
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
        if (!isConnected.get()) return;
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

    private class WbHandler extends Handler {
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

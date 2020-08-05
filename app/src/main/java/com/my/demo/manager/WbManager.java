package com.my.demo.manager;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wosmart.ukprotocollibary.WristbandManager;
import com.wosmart.ukprotocollibary.WristbandManagerCallback;
import com.wosmart.ukprotocollibary.WristbandScanCallback;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerDeviceInfoPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerFunctionPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerHrpItemPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerHrpPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerTemperatureControlPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerUserPacket;

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
        if (isConnected.get()) {
            WristbandManager.getInstance(context).registerCallback(wristbandManagerCallback);
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

    public void disconnect() {
        WristbandManager.getInstance(context).close();
    }

    /**
     * Start scan and connect to the first device found
     */
    private void startScan() {
        Log.e(TAG, "start scanning");
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
        Log.e(TAG, "Connecting to " + name + ", mac: " + mac);
        WristbandManager.getInstance(context).registerCallback(wristbandManagerCallback);
        WristbandManager.getInstance(context).connect(mac);
    }

    private void login() {
        Log.e(TAG, "Login");
        WristbandManager.getInstance(context).startLoginProcess("1234567890");
    }

    private void setup() {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Read device information");
                if (WristbandManager.getInstance(context).requestDeviceInfo()) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }

                Log.e(TAG, "Sync time");
                if (WristbandManager.getInstance(context).setTimeSync()) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }

//                Log.e(TAG, "Sync time");
//                if (WristbandManager.getInstance(context).setClocksSyncRequest()) {
//                    handler.sendEmptyMessage(0x01);
//                } else {
//                    handler.sendEmptyMessage(0x02);
//                }
//
//                ApplicationLayerUserPacket info = new ApplicationLayerUserPacket(true, 18, 180, 50);
//                if (WristbandManager.getInstance(context).setUserProfile(info)) {
//                    handler.sendEmptyMessage(0x01);
//                } else {
//                    handler.sendEmptyMessage(0x02);
//                }

                Log.e(TAG, "Set HR detect");
                if (WristbandManager.getInstance(context).setContinueHrp(true, 1)) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }

                Log.e(TAG, "Read HR detect");
                if (WristbandManager.getInstance(context).sendContinueHrpParamRequest()) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }

                Log.e(TAG, "Check temperature status");
                if (WristbandManager.getInstance(context).checkTemperatureStatus()) {
                    handler.sendEmptyMessage(0x01);
                } else {
                    handler.sendEmptyMessage(0x02);
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startMeasurement();
            }
        });
        thread.start();
    }

    private WristbandManagerCallback wristbandManagerCallback = new WristbandManagerCallback() {
        @Override
        public void onConnectionStateChange(boolean status) {
            super.onConnectionStateChange(status);
            isConnected.set(status);
            if (status) {
                Log.e(TAG, "Connected " + isConnected.get());
                login();
            } else {
                Log.e(TAG, "Failed to connected");
            }
        }

        @Override
        public void onError(int error) {
            super.onError(error);
            Log.e(TAG, "Error: " + error);
        }

        @Override
        public void onLoginStateChange(int state) {
            super.onLoginStateChange(state);
            Log.e(TAG, "onLoginStateChange " + state);
            if (state == WristbandManager.STATE_WRIST_LOGIN) {
                setup();
            }
        }

        @Override
        public void onDeviceInfo(ApplicationLayerDeviceInfoPacket packet) {
            super.onDeviceInfo(packet);
            Log.e(TAG, "device info = " + packet.toString());
        }

        @Override
        public void onDeviceFunction(ApplicationLayerFunctionPacket packet) {
            super.onDeviceFunction(packet);
            Log.e(TAG, "function info = " + packet.toString());
        }

        @Override
        public void onHrpContinueParamRsp(boolean enable, int interval) {
            super.onHrpContinueParamRsp(enable, interval);
            Log.e(TAG, "enable : " + enable + "interval : " + interval);
        }

        @Override
        public void onHrpDataReceiveIndication(ApplicationLayerHrpPacket packet) {
            super.onHrpDataReceiveIndication(packet);
            for (ApplicationLayerHrpItemPacket item : packet.getHrpItems()) {
                Log.e(TAG, "hr value :" + item.getValue());
                if (callback != null) {
                    callback.onHearRateUpdate(item.getValue());
                }
            }
        }

        @Override
        public void onDeviceCancelSingleHrpRead() {
            super.onDeviceCancelSingleHrpRead();
            Log.e(TAG, "stop measure hr ");
        }

        @Override
        public void onTemperatureData(ApplicationLayerHrpPacket packet) {
            super.onTemperatureData(packet);
            for (ApplicationLayerHrpItemPacket item : packet.getHrpItems()) {
                Log.e(TAG, "temp origin value :" + item.getTempOriginValue() + " temperature adjust value : " + item.getTemperature() + " is wear :" + item.isWearStatus() + " is adjust : " + item.isAdjustStatus() + "is animation :" + item.isAnimationStatus());
                if (callback != null) {
                    callback.onTemperatureValue(item.getTempOriginValue());
                }
            }
        }

        @Override
        public void onTemperatureMeasureSetting(ApplicationLayerTemperatureControlPacket packet) {
            super.onTemperatureMeasureSetting(packet);
            Log.e(TAG, "temp setting : show = " + packet.isShow() + " adjust = " + packet.isAdjust() + " celsius unit = " + packet.isCelsiusUnit());
        }

        @Override
        public void onTemperatureMeasureStatus(int status) {
            super.onTemperatureMeasureStatus(status);
            /**
             * 0 Temperature detection is off
             * 1 Start of temperature detection
             * 2 Heart rate detection is not enabled on the device (temperature depends on heart rate)
             * 3 The device heart rate detection is turned on, you can start temperature measurement
             */
            Log.e(TAG, "temp status :" + status);
        }
    };

    private void startMeasurement() {
        Log.e(TAG, "Start measurement " + isConnected.get());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Start heart rate
                if (WristbandManager.getInstance(context).readHrpValue()) {
                    Log.e(TAG, "Start HR succeed ");
                    handler.sendEmptyMessage(0x01);
                } else {
                    Log.e(TAG, "Start HR failed");
                    handler.sendEmptyMessage(0x02);
                }

                // Start temperature
                if (WristbandManager.getInstance(context).setTemperatureStatus(true)) {
                    Log.e(TAG, "Start temp succeed ");
                    handler.sendEmptyMessage(0x01);
                } else {
                    Log.e(TAG, "Start temp failed");
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

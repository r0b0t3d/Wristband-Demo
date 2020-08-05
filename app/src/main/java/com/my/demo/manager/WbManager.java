package com.my.demo.manager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.content.Context;
import android.util.Log;

import com.wosmart.ukprotocollibary.WristbandManager;
import com.wosmart.ukprotocollibary.WristbandManagerCallback;
import com.wosmart.ukprotocollibary.WristbandScanCallback;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerDeviceInfoPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerFunctionPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerHrpItemPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerHrpPacket;
import com.wosmart.ukprotocollibary.applicationlayer.ApplicationLayerTemperatureControlPacket;

public class WbManager {
    public static final String TAG = "WbManager";
    private Context context;

    private static WbManager instance;

    public static WbManager getInstance(Context context) {
        if (instance == null) {
            instance = new WbManager(context);
        }
        return instance;
    }

    private WbManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start() {
        startScan();
    }

    // Scan process
    private void startScan() {
        Log.e(TAG, "start scan");
        WristbandManager.getInstance(context).startScan(new WristbandScanCallback() {
            @Override
            public void onWristbandDeviceFind(BluetoothDevice device, int rssi, byte[] scanRecord) {
                super.onWristbandDeviceFind(device, rssi, scanRecord);
                Log.e(TAG, "Device found " + device.getAddress());
                stopScan();
                connect(device.getAddress(), device.getName());
            }

            @Override
            public void onWristbandDeviceFind(BluetoothDevice device, int rssi, ScanRecord scanRecord) {
                super.onWristbandDeviceFind(device, rssi, scanRecord);
            }

            @Override
            public void onLeScanEnable(boolean enable) {
                super.onLeScanEnable(enable);
                if (!enable) {}
            }

            @Override
            public void onWristbandLoginStateChange(boolean connected) {
                super.onWristbandLoginStateChange(connected);
            }
        });
    }

    private void stopScan() {
        WristbandManager.getInstance(context).stopScan();
    }

    private void connect(final String mac, final String name) {

        WristbandManager.getInstance(context).registerCallback(new WristbandManagerCallback() {
            @Override
            public void onConnectionStateChange(boolean status) {
                super.onConnectionStateChange(status);
                if (status) {
                    login();
                } else {
                    disconnect();
                }
            }

            @Override
            public void onError(int error) {
                super.onError(error);
            }
        });

        WristbandManager.getInstance(context).connect(mac);

    }

    private void disconnect() {
        WristbandManager.getInstance(context).close();
    }

    public void login() {
        WristbandManager.getInstance(context).registerCallback(new WristbandManagerCallback() {
            @Override
            public void onLoginStateChange(int state) {
                super.onLoginStateChange(state);
                if (state == WristbandManager.STATE_WRIST_LOGIN) {
                    Log.e(TAG, "Login success");

                    readDeviceInformation();
                }
            }
        });
        WristbandManager.getInstance(context).startLoginProcess("1234567890");
    }



    public void readDeviceInformation() {
        WristbandManager.getInstance(context).registerCallback(new WristbandManagerCallback() {

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
        });

        readVersion();
    }

    private void readVersion() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (WristbandManager.getInstance(context).requestDeviceInfo()) {
                    Log.e(TAG, "readVersion SUCCESS");
                    readFunction();
                } else {
                    Log.e(TAG, "readVersion FAIL");
                }
            }
        });
        thread.start();
    }

    private void readFunction() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (WristbandManager.getInstance(context).sendFunctionReq()) {
                    Log.e(TAG, "readFunction SUCCESS");
                    syncTime();
                } else {
                    Log.e(TAG, "readFunction FAIL");
                }
            }
        });
        thread.start();
    }

    private void syncTime() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (WristbandManager.getInstance(context).setTimeSync()) {
                    Log.e(TAG, "syncTime SUCCESS");
                    startMeasure();
                } else {
                    Log.e(TAG, "syncTime FAILED");
                }
            }
        });
        thread.start();
    }

    private void startMeasure() {
        WristbandManager.getInstance(context).registerCallback(new WristbandManagerCallback() {
            @Override
            public void onHrpDataReceiveIndication(ApplicationLayerHrpPacket packet) {
                super.onHrpDataReceiveIndication(packet);
                for (ApplicationLayerHrpItemPacket item : packet.getHrpItems()) {
                    Log.e(TAG, "hr value :" + item.getValue());
                }
            }

            @Override
            public void onDeviceCancelSingleHrpRead() {
                super.onDeviceCancelSingleHrpRead();
                Log.e(TAG, "stop measure hr ");
            }
        });
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (WristbandManager.getInstance(context).readHrpValue()) {
                    Log.e(TAG, "startMeasure SUCCESS");
                    startMeasureTemp();
                } else {
                    Log.e(TAG, "startMeasure FAIL");
                }
            }
        });
        thread.start();
    }

    private void stopMeasure() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (WristbandManager.getInstance(context).stopReadHrpValue()) {
                    Log.e(TAG, "stopMeasure SUCCESS");
                } else {
                    Log.e(TAG, "stopMeasure FAIL");
                }
            }
        });
        thread.start();
    }

    private void startMeasureTemp() {
        WristbandManager.getInstance(context).registerCallback(new WristbandManagerCallback() {
            @Override
            public void onTemperatureData(ApplicationLayerHrpPacket packet) {
                super.onTemperatureData(packet);
                for (ApplicationLayerHrpItemPacket item : packet.getHrpItems()) {
                    Log.e(TAG, "temp origin value :" + item.getTempOriginValue() + " temperature adjust value : " + item.getTemperature() + " is wear :" + item.isWearStatus() + " is adjust : " + item.isAdjustStatus() + "is animation :" + item.isAnimationStatus());
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
                Log.e(TAG, "temp status :" + status);
            }
        });
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (WristbandManager.getInstance(context).setTemperatureStatus(true)) {
                    Log.e(TAG, "startMeasureTemp SUCCESS");
                } else {
                    Log.e(TAG, "startMeasureTemp FAIL");
                }
            }
        });
        thread.start();
    }

    private void stopMeasureTemp() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (WristbandManager.getInstance(context).setTemperatureStatus(false)) {
                    Log.e(TAG, "stopMeasureTemp SUCCESS");
                } else {
                    Log.e(TAG, "stopMeasureTemp FAIL");
                }
            }
        });
        thread.start();
    }
}

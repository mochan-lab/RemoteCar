package com.mochan.unitynativepluginbcore4;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;

import com.unity3d.player.UnityPlayer;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

public class AndroidBLEbCore4 {
    private final static String TAG = AndroidBLEbCore4.class.getSimpleName();   // same as Unity GameObject Name
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_MULTI_PERMISSIONS = 101;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private boolean mScanning;
    private static final long SCAN_PERIOD = 10000;
    private static final UUID BCORE_SERVICE_UUID = UUID.fromString("389CAAF0-843F-4d3b-959D-C954CCE14655");
    private static final UUID GET_BATTERY_VOLTAGE_UUID = UUID.fromString("389CAAF1-843F-4d3b-959D-C954CCE14655");
    private static final UUID BURST_COMMAND_UUID = UUID.fromString("389CAAF5-843F-4d3b-959D-C954CCE14655");
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGattCharacteristic mCharaGetBatteryVoltage;
    private BluetoothGattCharacteristic mCharaBurstCommand;

    public void initialize() {
        mHandler = new Handler();
        // checking support BLE
        if (!UnityPlayer.currentActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(UnityPlayer.currentActivity, "Not support BLE!", Toast.LENGTH_SHORT).show();
            UnityPlayer.currentActivity.finish();
            return;
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) UnityPlayer.currentActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothAdapter == null) {
            Toast.makeText(UnityPlayer.currentActivity, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
            UnityPlayer.currentActivity.finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            ArrayList<String> requestPermissions = new ArrayList<>();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(UnityPlayer.currentActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ActivityCompat.checkSelfPermission(UnityPlayer.currentActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (!requestPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(UnityPlayer.currentActivity, requestPermissions.toArray(new String[0]), REQUEST_MULTI_PERMISSIONS);
            }
        }
        unitySendMessage("MESSAGE", "Succeed construct BLE");
    }

    public void scanDevice(Context context, final boolean enable) {
        ScanSettings.Builder scanSettings = new ScanSettings.Builder();
        scanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        ScanSettings settings = scanSettings.build();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(scanCallBack);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothLeScanner.startScan(null, settings, scanCallBack);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(scanCallBack);
        }
    }

    private ScanCallback scanCallBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice() == null) {
                unitySendMessage("CAUTION", "No device founded!");
                return;
            }
            String deviceName = result.getDevice().getName();
            String address = result.getDevice().getAddress();
            unitySendDevice(deviceName, address);
        }
    };

    public void connectToDevice(Context context, String address) {
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        if (mBluetoothDevice == null) {
            unitySendMessage("ERROR", "Cannot find the device!");
            return;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        mBluetoothGatt = mBluetoothDevice.connectGatt(UnityPlayer.currentActivity, true, gattCallback);
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                unitySendMessage("MESSAGE", "Connected device");
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                unitySendMessage("MESSAGE", "Disconnected device");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        unitySendMessage("MESSAGE", "Discover characteristic", service.getUuid().toString(), characteristic.getUuid().toString());
                        if(GET_BATTERY_VOLTAGE_UUID.equals(characteristic.getUuid())) {
                            mCharaGetBatteryVoltage = characteristic;
                            unitySendMessage("MESSAGE", "Discover GET_BATTERY_VOLTAGE");
                        }
                        if(BURST_COMMAND_UUID.equals(characteristic.getUuid())) {
                            mCharaBurstCommand = characteristic;
                            unitySendMessage("MESSAGE", "Discover BURST_COMMAND");
                        }
                    }
                }
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                if(GET_BATTERY_VOLTAGE_UUID.equals(characteristic.getUuid())) {
                    byte[] returnBytes = characteristic.getValue();
                    ByteBuffer bb = ByteBuffer.wrap(returnBytes);
                    final String str = String.valueOf(bb.getShort(0));
                    UnityPlayer.UnitySendMessage(TAG, "ReturnBatteryVoltage", str);
                }
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status){
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if(BURST_COMMAND_UUID.equals(characteristic.getUuid())){
                    unitySendMessage("MESSAGE", "WRITE BURST COMMAND");
                }
            }
        }
    };

    public void closeGatt(Context context) {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        unitySendMessage("LOG", "Close gatt connection");
        return;
    }

    public void discoverService(){
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.discoverServices();
        unitySendMessage("LOG","start discoverService");
    }

    private void bleWriteNoResponse(UUID uuid_service, UUID uuid_characteristic, byte[] byteChara){
        if(mBluetoothGatt==null){
            return;
        }
        BluetoothGattService bleService = mBluetoothGatt.getService(uuid_service);
        BluetoothGattCharacteristic bleChara = bleService.getCharacteristic(uuid_characteristic);
        bleChara.setValue(byteChara);
        mBluetoothGatt.writeCharacteristic(bleChara);
    }

    public void getBatteryVoltage(){
        if(mBluetoothGatt==null || mCharaGetBatteryVoltage == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(mCharaGetBatteryVoltage);
    }

    public void burstCommand(byte[] bytes){
        if(mBluetoothGatt==null || mCharaBurstCommand == null) {
            return;
        }
        mCharaBurstCommand.setValue(bytes);
        mBluetoothGatt.writeCharacteristic(mCharaBurstCommand);
    }

    private void unitySendMessage(String... messages){
        String message = String.join(",", messages);
        UnityPlayer.UnitySendMessage(TAG, "MessageFromPlugin", message);
    }

    private void unitySendDevice(String... messages){
        String message = String.join(",", messages);
        UnityPlayer.UnitySendMessage(TAG, "DeviceFromPlugin", message);
    }
}

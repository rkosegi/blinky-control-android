/*
Copyright 2025 Richard Kosegi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.github.rkosegi.blinkycontrol;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Strings;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static com.github.rkosegi.blinkycontrol.Constants.LED_BIT_CHAR_UUID;
import static com.github.rkosegi.blinkycontrol.Constants.LED_MODE_CHAR_UUID;
import static com.github.rkosegi.blinkycontrol.Constants.LED_SERVICE_UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private List<BluetoothGattService> btServices = List.of();

    private final BluetoothGattCallback bleCb = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(MainActivity.class.getSimpleName(), "BluetoothGattCallback:onServicesDiscovered(status=" + status + ")");
            if(status == GATT_SUCCESS) {
                btServices = gatt.getServices();
            }
            super.onServicesDiscovered(gatt, status);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(MainActivity.class.getSimpleName(), "onConnectionStateChange(newState=" + newState + ")");
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(MainActivity.class.getSimpleName(), "Discovering services");
                    if (!gatt.discoverServices()) {
                        Log.w(MainActivity.class.getSimpleName(), "refreshBluetoothConnection: gatt->discoverServices returned false");
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    clearGatt();
                    break;
                default:
                    break;
            }
            MainActivity.this.runOnUiThread(() -> enableControls(newState == BluetoothProfile.STATE_CONNECTED));
            super.onConnectionStateChange(gatt, status, newState);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_left).setTag(11);
        findViewById(R.id.btn_right).setTag(12);
        findViewById(R.id.btn_stop).setTag(0);
        findViewById(R.id.btn_red1).setTag(1);
        findViewById(R.id.btn_red2).setTag(2);
        findViewById(R.id.btn_red3).setTag(3);
        findViewById(R.id.btn_red4).setTag(4);
        findViewById(R.id.btn_red5).setTag(5);

        enableControls(false);

        final SwipeRefreshLayout srl = findViewById(R.id.swipe_refresh);
        srl.setOnRefreshListener(this::onRefresh);
    }

    private void onRefresh() {
        refreshBluetoothConnection();
        ((SwipeRefreshLayout) findViewById(R.id.swipe_refresh)).setRefreshing(false);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshBluetoothConnection();
    }

    private void enableControls(boolean enable) {
        Stream.of(
                R.id.btn_left, R.id.btn_stop, R.id.btn_right,
                R.id.btn_red1, R.id.btn_red2, R.id.btn_red3, R.id.btn_red4, R.id.btn_red5,
                R.id.switch_orange1, R.id.switch_orange2, R.id.switch_orange3, R.id.switch_orange4,
                R.id.switch_orange5, R.id.switch_orange6, R.id.switch_orange7, R.id.switch_orange8,
                R.id.switch_red1, R.id.switch_red2, R.id.switch_red3, R.id.switch_red4,
                R.id.switch_red5, R.id.switch_red6, R.id.switch_red7, R.id.switch_red8
        ).map((Function<Integer, View>) this::findViewById).forEach(view -> view.setEnabled(enable));
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(MainActivity.class.getSimpleName(), "onRequestPermissionsResult(rc=" +
                requestCode + ", gr=" + List.of(grantResults));
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void ensurePermission(String perm) {
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(perm)) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, 1);
            }
        }
    }

    private void requestPermissions() {
        ensurePermission(BLUETOOTH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ensurePermission(BLUETOOTH_CONNECT);
            ensurePermission(BLUETOOTH_SCAN);
        }
        final BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(MainActivity.class.getSimpleName(), "bluetooth is not enabled");
            return;
        }
        this.bluetoothAdapter = bluetoothAdapter;
    }

    @SuppressLint("MissingPermission")
    public void refreshBluetoothConnection() {
        requestPermissions();
        if (bluetoothAdapter != null) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final String bleAddress = prefs.getString("ble_device_address", null);
            Log.i(MainActivity.class.getSimpleName(), "Device address from preferences : " + bleAddress);
            runOnUiThread(() -> ((TextView) findViewById(R.id.label_device_address)).setText(
                    String.format(getString(R.string.device_address_label), bleAddress)));
            if (!Strings.isNullOrEmpty(bleAddress)) {
                final BluetoothDevice device;
                try {
                    device = bluetoothAdapter.getRemoteDevice(bleAddress);
                } catch (IllegalArgumentException e) {
                    Log.e(MainActivity.class.getSimpleName(), "Invalid device address : " + bleAddress, e);
                    return;
                }
                clearGatt();
                bluetoothGatt = device.connectGatt(this.getBaseContext(), false, bleCb);
                if (bluetoothGatt.connect()) {
                    Log.w(MainActivity.class.getSimpleName(), "refreshBluetoothConnection: bluetoothGatt->connect returned false");
                }
            } else {
                Log.w(MainActivity.class.getSimpleName(), "Device is not yet configured");
            }
        } else {
            Log.w(MainActivity.class.getSimpleName(), "BluetoothAdapter is not available");
        }
    }

    public void onSettings(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @SuppressLint("MissingPermission")
    public void onToggleBit(View view) {
        int redVal = 0;
        redVal |= (((SwitchCompat) findViewById(R.id.switch_red1)).isChecked() ? 1 : 0);
        redVal |= (((SwitchCompat) findViewById(R.id.switch_red2)).isChecked() ? 1 : 0) << 1;
        redVal |= (((SwitchCompat) findViewById(R.id.switch_red3)).isChecked() ? 1 : 0) << 2;
        redVal |= (((SwitchCompat) findViewById(R.id.switch_red4)).isChecked() ? 1 : 0) << 3;
        redVal |= (((SwitchCompat) findViewById(R.id.switch_red5)).isChecked() ? 1 : 0) << 4;
        redVal |= (((SwitchCompat) findViewById(R.id.switch_red6)).isChecked() ? 1 : 0) << 5;
        redVal |= (((SwitchCompat) findViewById(R.id.switch_red7)).isChecked() ? 1 : 0) << 6;
        redVal |= (((SwitchCompat) findViewById(R.id.switch_red8)).isChecked() ? 1 : 0) << 7;

        int oraVal = 0;
        oraVal |= (((SwitchCompat) findViewById(R.id.switch_orange1)).isChecked() ? 1 : 0);
        oraVal |= (((SwitchCompat) findViewById(R.id.switch_orange2)).isChecked() ? 1 : 0) << 1;
        oraVal |= (((SwitchCompat) findViewById(R.id.switch_orange3)).isChecked() ? 1 : 0) << 2;
        oraVal |= (((SwitchCompat) findViewById(R.id.switch_orange4)).isChecked() ? 1 : 0) << 3;
        oraVal |= (((SwitchCompat) findViewById(R.id.switch_orange5)).isChecked() ? 1 : 0) << 4;
        oraVal |= (((SwitchCompat) findViewById(R.id.switch_orange6)).isChecked() ? 1 : 0) << 5;
        oraVal |= (((SwitchCompat) findViewById(R.id.switch_orange7)).isChecked() ? 1 : 0) << 6;
        oraVal |= (((SwitchCompat) findViewById(R.id.switch_orange8)).isChecked() ? 1 : 0) << 7;

        Log.i(MainActivity.class.getSimpleName(), "onToggleBit: computed hex values: " +
                String.format("%02X %02X", oraVal, redVal));

        if (bluetoothGatt != null) {
            final Optional<BluetoothGattService> ledSvc = btServices.stream()
                    .filter(s -> s.getUuid().equals(Constants.LED_SERVICE_UUID))
                    .findFirst();
            if (ledSvc.isPresent()) {
                final BluetoothGattCharacteristic ledBitsChar = ledSvc.get().getCharacteristic(LED_BIT_CHAR_UUID);
                if (ledBitsChar != null) {
                    final byte[] buff = new byte[2];
                    buff[0] = (byte) (oraVal & 0xff);
                    buff[1] = (byte) (redVal & 0xff);
                    ledBitsChar.setValue(buff);
                    if (!bluetoothGatt.writeCharacteristic(ledBitsChar)) {
                        Log.w(MainActivity.class.getSimpleName(), "onToggleBit: bluetoothGatt->writeCharacteristic returned false");
                    }
                } else {
                    Log.w(MainActivity.class.getSimpleName(), "onToggleBit: LED bits characteristic (" +
                            LED_BIT_CHAR_UUID + ") is not present in LED service (" +
                            LED_SERVICE_UUID + ")");
                }
            } else {
                Log.w(MainActivity.class.getSimpleName(), "onToggleBit: Advertised services does not include LED service (" +
                        LED_SERVICE_UUID.toString() + ")");
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void onBlinkModeChange(View view) {
        int mode = (int) view.getTag();
        if (bluetoothGatt != null) {
            final Optional<BluetoothGattService> ledSvc = btServices.stream()
                    .filter(s -> s.getUuid().equals(Constants.LED_SERVICE_UUID))
                    .findFirst();
            if (ledSvc.isPresent()) {
                final BluetoothGattCharacteristic ledModeChar = ledSvc.get().getCharacteristic(LED_MODE_CHAR_UUID);
                if (ledModeChar != null) {
                    ledModeChar.setValue(new byte[]{(byte) (mode & 0xff)});
                    if (!bluetoothGatt.writeCharacteristic(ledModeChar)) {
                        Log.w(MainActivity.class.getSimpleName(), "onBlinkModeChange: bluetoothGatt->writeCharacteristic returned false");
                    }
                } else {
                    Log.w(MainActivity.class.getSimpleName(), "onToggleBit: LED mode characteristic (" +
                            LED_MODE_CHAR_UUID + ") is not present in LED service (" +
                            LED_SERVICE_UUID + ")");
                }
            } else {
                Log.w(MainActivity.class.getSimpleName(), "onToggleBit: Advertised services does not include LED service (" +
                        LED_SERVICE_UUID.toString() + ")");
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void clearGatt() {
        btServices = List.of();
        if (bluetoothGatt != null) {
            Log.i(MainActivity.class.getSimpleName(), "Closing Gatt");
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
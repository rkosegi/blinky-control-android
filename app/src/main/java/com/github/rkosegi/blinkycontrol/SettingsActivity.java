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
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(), ar -> {
                    Log.i(SettingsFragment.class.getSimpleName(), "onActivityResult(" + ar + ")");
                    if (ar.getResultCode() == Activity.RESULT_OK) {
                        if (ar.getData() != null) {
                            final BluetoothDevice device = ar.getData().getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
                            if (device != null) {
                                updateDeviceAddress(device.getAddress());
                            } else {
                                Log.w(SettingsActivity.class.getSimpleName(), "Device not present in activity result data (" +
                                        CompanionDeviceManager.EXTRA_DEVICE + ")");
                            }
                        } else {
                            Log.w(SettingsActivity.class.getSimpleName(), "Data not present in activity result");
                        }
                    }
                });

        @SuppressLint("MissingPermission")
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            final Preference blePref = findPreference("ble_device_address");
            if (blePref != null) {
                blePref.setOnPreferenceClickListener(pref -> startDevicePicker());
            }
        }

        private void updateDeviceAddress(String address) {
            Log.i(SettingsActivity.class.getSimpleName(), "Got device " + address);
            final SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit();
            prefs.putString(Constants.PREF_KEY_DEV_ADDR, address);
            prefs.apply();
        }

        private Pattern getDeviceNamePattern() {
            final String pat = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("ble_name_filter", "blinky");
            try {
                return Pattern.compile(pat);
            } catch (PatternSyntaxException e) {
                return Pattern.compile("blinky");
            }
        }

        private boolean startDevicePicker() {
            Log.i(SettingsFragment.class.getSimpleName(), "startDevicePicker");
            final CompanionDeviceManager manager = requireContext().getSystemService(CompanionDeviceManager.class);
            final List<String> macs = manager.getAssociations();
            if (!macs.isEmpty()) {
                Log.i(SettingsFragment.class.getSimpleName(), "existing associations: " + macs);
                macs.forEach(manager::disassociate);
            }
            manager.associate(getAssociationRequest(), new CompanionDeviceManager.Callback() {
                @Override
                public void onDeviceFound(@NonNull IntentSender is) {
                    Log.i(SettingsFragment.class.getSimpleName(), "CDM::onDeviceFound()");
                    requireActivity().runOnUiThread(() -> launcher.launch(new IntentSenderRequest.Builder(is).build()));
                }

                @Override
                public void onFailure(CharSequence error) {
                    Log.i(SettingsFragment.class.getSimpleName(), "CDM::onFailure() => " + error);
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            }, null);
            return true;
        }

        @NonNull
        private AssociationRequest getAssociationRequest() {
            final BluetoothDeviceFilter filter = new BluetoothDeviceFilter.Builder()
                    //.addServiceUuid(new ParcelUuid(LED_SERVICE_UUID), null) // this is crashing for some reason
                    .setNamePattern(getDeviceNamePattern()) // I don't like to hardcode it here, but ...
                    .build();

            return new AssociationRequest.Builder()
                    .addDeviceFilter(filter)
                    .setSingleDevice(false)
                    .build();
        }
    }
}
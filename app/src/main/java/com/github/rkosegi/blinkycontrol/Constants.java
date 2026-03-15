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

import java.util.UUID;

public final class Constants {

    private  Constants() {}
    static final String PREF_KEY_DEV_ADDR = "ble_device_address";

    // battery service
    static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000–1000–8000–00805f9b34fb");

    // battery level in percent
    static final UUID BATTERY_LEVEL_CHAR_UUID = UUID.fromString("00002A19–0000–1000–8000–00805f9b34fb");

    // battery voltage in mV
    static final UUID BATTERY_VOLTAGE_CHAR_UUID = UUID.fromString("00002B18–0000–1000–8000–00805f9b34fb");

    // device info service
    static final UUID DEVICE_INFO_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    // device info - FW revision characteristic
    static final UUID DI_FW_REV_CHAR_UUID = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb");

    // device info - manufacturer name characteristic
    static final UUID DI_MF_NAME_CHAR_UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");

    // LED service
    static final UUID LED_SERVICE_UUID =  UUID.fromString("27f65506-2524-4df3-803a-5f74e5a32ada");
    // bit-mask characteristic
    static final UUID LED_BIT_CHAR_UUID = UUID.fromString("1e0b46a6-7f06-4fc6-a66e-a054b158828d");
    // blink mode characteristic
    static final UUID LED_MODE_CHAR_UUID = UUID.fromString("d5dc531e-0c9a-4cd9-a696-4bcb8c5be548");
    // dom level characteristic
    static final UUID LED_DIM_LEVEL_CHAR_UUID = UUID.fromString("d51f3202-4901-4624-a426-90b3dc87c0f2");
}

/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.tv.appfunctions

import android.content.Context
import android.content.Intent
import android.media.MediaRoute2Info
import android.media.RoutingChangeInfo
import android.util.Log
import androidx.annotation.Keep
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import com.android.settingslib.media.InfoMediaManager
import com.android.settingslib.media.LocalMediaManager
import com.android.settingslib.media.MediaDevice
import com.android.systemui.SystemUIAppComponentFactoryBase
import com.android.systemui.tv.dagger.TvSysUIComponent
import com.android.systemui.tv.media.MediaOutputUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject

@Keep
class TvSystemUIAppFunctions {
    private val TAG = "TvSystemUIAppFunctions"
    private val DEBUG = false

    private fun getLocalMediaManager(context: Context): LocalMediaManager {
        val entryPoint =
            SystemUIAppComponentFactoryBase.systemUIInitializer?.sysUIComponent as? TvSysUIComponent
                ?: throw IllegalStateException("TvSysUIComponent not found")
        val localBluetoothManager = entryPoint.getLocalBluetoothManager()
        val mInfoMediaManager =
            InfoMediaManager.createInstance(
                context,
                context.packageName,
                /* userHandle= */ null,
                localBluetoothManager,
                /* token= */ null,
            )
        return LocalMediaManager(
            context,
            localBluetoothManager,
            mInfoMediaManager,
            context.packageName,
        )
    }

    // Key definitions for Audio Control
    private val KEY_AUDIO_OUTPUT_DEVICES = "google.audio.output_devices"
    private val KEY_AUDIO_OUTPUT_DEVICE_CURRENT = "google.audio.output_device.current"

    /**
     * Get the metadata for all device state items.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getDeviceStateMetadata(context: AppFunctionContext): DeviceMetadataResponse {
        Log.d(TAG, "getDeviceStateMetadata called")
        val metadataList = mutableListOf<DeviceStateItemMetadata>()

        // Metadata for listing audio output devices
        metadataList.add(
            DeviceStateItemMetadata(
                key = KEY_AUDIO_OUTPUT_DEVICES,
                localizedName = "Available Audio Output Devices",
                description = "Gets a list of available audio output devices (e.g., TV Speakers, Soundbar, Headphones).",
                possibleValues = """{"type": "array", "items": {"type": "object", "properties": { "id": { "type": "string" }, "label": { "type": "string" }, "type": { "type": "string" }, "connected": { "type": "boolean" }, "selected": { "type" : "boolean" } }}}""",
                writable = false,
                isDeviceContext = true, // Pre-fetch this context
            )
        )

        // Metadata for the current audio output device
        metadataList.add(
            DeviceStateItemMetadata(
                key = KEY_AUDIO_OUTPUT_DEVICE_CURRENT,
                localizedName = "Current Audio Output Device",
                description = "Gets or sets the current audio output device. Use device id to set the current output device.",
                possibleValues = """{"type": "String"}""", // Device id
                writable = true,
                isDeviceContext = true, // Pre-fetch this context
            )
        )

        return DeviceMetadataResponse(deviceLocale = "en-US", deviceStateMetadata = metadataList)
    }

    /**
     * Get device state items by their keys.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getDeviceState(
        context: AppFunctionContext,
        keys: List<String>,
    ): GetDeviceStateResponse {
        Log.d(TAG, "getDeviceState called with keys: $keys")
        val responses = mutableListOf<GetDeviceStateItemResponse>()

        for (key in keys) {
            val response = when (key) {
                KEY_AUDIO_OUTPUT_DEVICES -> {
                    val devices = waitForDeviceListUpdate(context.context)
                    val deviceArray = JSONArray()
                    for (device in devices) {
                        val deviceObject = JSONObject()
                        deviceObject.put("label", device.name)
                        if (!device.isConnected && device.routeType == MediaRoute2Info.TYPE_UNKNOWN) {
                            deviceObject.put(
                                "type",
                                MediaOutputUtil.routeTypeToString(MediaRoute2Info.TYPE_BLUETOOTH_A2DP),
                            )
                        } else {
                            deviceObject.put(
                                "type",
                                MediaOutputUtil.routeTypeToString(device.routeType),
                            )
                        }
                        deviceObject.put("connected", device.isConnected)
                        deviceObject.put("selected", device.isSelected)
                        deviceObject.put("id", device.id)
                        deviceArray.put(deviceObject)
                    }
                    if (DEBUG) {
                        val jsonString = deviceArray.toString(4)
                        Log.d(TAG, "getDeviceState output devices json for key $key: $jsonString")
                    }
                    GetDeviceStateItemResponse(key = key, value = deviceArray.toString())
                }

                KEY_AUDIO_OUTPUT_DEVICE_CURRENT -> {
                    val currentlySelected =
                        waitForDeviceListUpdate(context.context).find { it.isSelected }
                    if (currentlySelected == null) {
                        GetDeviceStateItemResponse(key = key, value = "No device selected")
                    } else {
                        GetDeviceStateItemResponse(key = key, value = currentlySelected.name)
                    }
                }

                else -> {
                    GetDeviceStateItemResponse(key = key, value = "Unknown key", deviceMetadata = null)
                }
            }
            responses.add(response)
        }
        return GetDeviceStateResponse(responses = responses)
    }

    /**
     * Set a single device state item by its key.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun setDeviceState(
        context: AppFunctionContext,
        key: String,
        value: String,
    ): SetDeviceStateItemResponse {
        Log.d(TAG, "setDeviceState called with key: $key, value: $value")
        return when (key) {
            KEY_AUDIO_OUTPUT_DEVICE_CURRENT -> {

                val localMediaManager = getLocalMediaManager(context.context)
                val devices = waitForDeviceListUpdate(context.context, localMediaManager)

                val currentConnectedMediaDevice = devices.find { it.isSelected }
                val mediaDevice = devices.find { it.id == value }
                if (DEBUG) {
                    Log.d(
                        TAG,
                        "currentConnectedMediaDevice: ${currentConnectedMediaDevice?.id}, " +
                                "new mediaDevice: ${mediaDevice?.id}",
                    )
                }

                if (mediaDevice == null) {
                    return SetDeviceStateItemResponse(
                        isSuccessful = false,
                        currentValue = value,
                        failureReason = "Device not found",
                    )
                }

                var isAnyDeviceTransferring = false
                for (device in devices) {
                    if (device.state == LocalMediaManager.MediaDeviceState.STATE_CONNECTING) {
                        isAnyDeviceTransferring = true
                        break
                    }
                }
                if (isAnyDeviceTransferring) {
                    return SetDeviceStateItemResponse(
                        isSuccessful = false,
                        currentValue = currentConnectedMediaDevice?.name ?: "",
                        failureReason =
                            "A transfer is currently in progress and should not be interrupted.",
                    )
                }

                if (mediaDevice.id == currentConnectedMediaDevice?.id) {
                    return SetDeviceStateItemResponse(
                        isSuccessful = false,
                        currentValue = currentConnectedMediaDevice.name,
                        failureReason = "The device is already the active one.",
                    )
                }

                val success =
                    localMediaManager.connectDevice(
                        mediaDevice,
                        RoutingChangeInfo(
                            RoutingChangeInfo.ENTRY_POINT_TV_OUTPUT_SWITCHER,
                            /* isSuggested= */ false
                        )
                    )

                SetDeviceStateItemResponse(
                    isSuccessful = success,
                    currentValue = mediaDevice.id ?: "",
                )
            }

            else -> {
                SetDeviceStateItemResponse(
                    isSuccessful = false,
                    currentValue = "",
                    failureReason = "Unknown key",
                )
            }
        }
    }

    suspend fun waitForDeviceListUpdate(context: Context,
        localMediaManager: LocalMediaManager = getLocalMediaManager(context)): List<MediaDevice> {
        return suspendCancellableCoroutine { continuation ->
            val callback =
                object : LocalMediaManager.DeviceCallback {
                    override fun onDeviceListUpdate(devices: List<MediaDevice>) {
                        Log.d(TAG, "onDeviceListUpdate, ${devices.size} items: $devices")
                        localMediaManager.unregisterCallback(this)
                        localMediaManager.stopScan()
                        continuation.resume(devices, {
                            localMediaManager.unregisterCallback(this)
                            localMediaManager.stopScan()
                        })
                    }
                }
            localMediaManager.registerCallback(callback)
            localMediaManager.startScan()
        }
    }
}

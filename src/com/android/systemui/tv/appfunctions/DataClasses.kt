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

import androidx.appfunctions.AppFunctionSerializable

/** Metadata for a single device state item. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DeviceStateItemMetadata(
    /** The unique key for this item. */
    val key: String,
    /** The name of the item localized to the device's current locale. */
    val localizedName: String,
    /** A description of the item. */
    val description: String,
    /** A string representation of the possible values this item can take. */
    val possibleValues: String,
    /** List of keys of other items this item depends on. */
    val dependencies: List<String> = emptyList(),
    /** Natural language description of the dependencies. */
    val dependencyDescription: String? = null,
    /** Optional hints for an LLM on how to use this item. */
    val hintText: String? = null,
    /** Whether this item can be changed. */
    val writable: Boolean = true,
    /** Whether this item represents device context. */
    val isDeviceContext: Boolean = false,
)

/** Response containing metadata for all device items. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DeviceMetadataResponse(
    /** The device's current locale. */
    val deviceLocale: String,
    /** List of metadata for all available device state items. */
    val deviceStateMetadata: List<DeviceStateItemMetadata>,
)

/** Response for a get device state operation. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class GetDeviceStateItemResponse(
    /** The key of the item. */
    val key: String,
    /** The current value of the item. */
    val value: String,
    /** Optional metadata for the item. */
    val deviceMetadata: DeviceStateItemMetadata? = null,
)

/** Response for a batch get device state operation. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class GetDeviceStateResponse(
    /** List of individual item responses. */
    val responses: List<GetDeviceStateItemResponse>,
)

/** Response for a set device state operation. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SetDeviceStateItemResponse(
    /** Whether the operation was successful. */
    val isSuccessful: Boolean,
    /** The current value of the item after the operation. */
    val currentValue: String,
    /** The reason for failure, if any. */
    val failureReason: String? = null,
)

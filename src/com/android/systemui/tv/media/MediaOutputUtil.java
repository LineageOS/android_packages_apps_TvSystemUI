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

package com.android.systemui.tv.media;

import static android.media.MediaRoute2Info.TYPE_AUX_LINE;
import static android.media.MediaRoute2Info.TYPE_BLE_HEADSET;
import static android.media.MediaRoute2Info.TYPE_BLUETOOTH_A2DP;
import static android.media.MediaRoute2Info.TYPE_BUILTIN_SPEAKER;
import static android.media.MediaRoute2Info.TYPE_DOCK;
import static android.media.MediaRoute2Info.TYPE_GROUP;
import static android.media.MediaRoute2Info.TYPE_HDMI;
import static android.media.MediaRoute2Info.TYPE_HDMI_ARC;
import static android.media.MediaRoute2Info.TYPE_HDMI_EARC;
import static android.media.MediaRoute2Info.TYPE_HEARING_AID;
import static android.media.MediaRoute2Info.TYPE_LINE_ANALOG;
import static android.media.MediaRoute2Info.TYPE_LINE_DIGITAL;
import static android.media.MediaRoute2Info.TYPE_MULTICHANNEL_SPEAKER_GROUP;
import static android.media.MediaRoute2Info.TYPE_REMOTE_AUDIO_VIDEO_RECEIVER;
import static android.media.MediaRoute2Info.TYPE_REMOTE_CAR;
import static android.media.MediaRoute2Info.TYPE_REMOTE_COMPUTER;
import static android.media.MediaRoute2Info.TYPE_REMOTE_GAME_CONSOLE;
import static android.media.MediaRoute2Info.TYPE_REMOTE_SMARTPHONE;
import static android.media.MediaRoute2Info.TYPE_REMOTE_SMARTWATCH;
import static android.media.MediaRoute2Info.TYPE_REMOTE_SPEAKER;
import static android.media.MediaRoute2Info.TYPE_REMOTE_TABLET;
import static android.media.MediaRoute2Info.TYPE_REMOTE_TABLET_DOCKED;
import static android.media.MediaRoute2Info.TYPE_REMOTE_TV;
import static android.media.MediaRoute2Info.TYPE_UNKNOWN;
import static android.media.MediaRoute2Info.TYPE_USB_ACCESSORY;
import static android.media.MediaRoute2Info.TYPE_USB_DEVICE;
import static android.media.MediaRoute2Info.TYPE_USB_HEADSET;
import static android.media.MediaRoute2Info.TYPE_WIRED_HEADPHONES;
import static android.media.MediaRoute2Info.TYPE_WIRED_HEADSET;

public class MediaOutputUtil {

    public static String routeTypeToString(int type) {
        return switch (type) {
            case TYPE_UNKNOWN -> "TYPE_UNKNOWN";
            case TYPE_BUILTIN_SPEAKER -> "TYPE_BUILTIN_SPEAKER";
            case TYPE_WIRED_HEADSET -> "TYPE_WIRED_HEADSET";
            case TYPE_WIRED_HEADPHONES -> "TYPE_WIRED_HEADPHONES";
            case TYPE_BLUETOOTH_A2DP -> "TYPE_BLUETOOTH_A2DP";
            case TYPE_HDMI -> "TYPE_HDMI";
            case TYPE_HDMI_ARC -> "TYPE_HDMI_ARC";
            case TYPE_HDMI_EARC -> "TYPE_HDMI_EARC";
            case TYPE_LINE_DIGITAL -> "TYPE_LINE_DIGITAL";
            case TYPE_LINE_ANALOG -> "TYPE_LINE_ANALOG";
            case TYPE_AUX_LINE -> "TYPE_AUX_LINE";
            case TYPE_USB_DEVICE -> "TYPE_USB_DEVICE";
            case TYPE_USB_ACCESSORY -> "TYPE_USB_ACCESSORY";
            case TYPE_DOCK -> "TYPE_DOCK";
            case TYPE_USB_HEADSET -> "TYPE_USB_HEADSET";
            case TYPE_HEARING_AID -> "TYPE_HEARING_AID";
            case TYPE_BLE_HEADSET -> "TYPE_BLE_HEADSET";
            case TYPE_MULTICHANNEL_SPEAKER_GROUP -> "TYPE_MULTICHANNEL_SPEAKER_GROUP";
            case TYPE_REMOTE_TV -> "TYPE_REMOTE_TV";
            case TYPE_REMOTE_SPEAKER -> "TYPE_REMOTE_SPEAKER";
            case TYPE_REMOTE_AUDIO_VIDEO_RECEIVER -> "TYPE_REMOTE_AUDIO_VIDEO_RECEIVER";
            case TYPE_REMOTE_TABLET -> "TYPE_REMOTE_TABLET";
            case TYPE_REMOTE_TABLET_DOCKED -> "TYPE_REMOTE_TABLET_DOCKED";
            case TYPE_REMOTE_COMPUTER -> "TYPE_REMOTE_COMPUTER";
            case TYPE_REMOTE_GAME_CONSOLE -> "TYPE_REMOTE_GAME_CONSOLE";
            case TYPE_REMOTE_CAR -> "TYPE_REMOTE_CAR";
            case TYPE_REMOTE_SMARTWATCH -> "TYPE_REMOTE_SMARTWATCH";
            case TYPE_REMOTE_SMARTPHONE -> "TYPE_REMOTE_SMARTPHONE";
            case TYPE_GROUP -> "TYPE_GROUP";
            default -> "TYPE_UNKNOWN";
        };
    }
}

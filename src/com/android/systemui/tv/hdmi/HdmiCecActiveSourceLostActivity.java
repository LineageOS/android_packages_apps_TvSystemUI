/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.systemui.tv.hdmi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.hdmi.HdmiControlManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.tv.TvBottomSheetActivity;
import com.android.systemui.tv.res.R;

/**
 * Confirmation dialog shown when the device loses active source. Gives the user the option to keep
 * the device active, in case the loss of active source was due to a misleading CEC message from a
 * faulty device.
 */
public class HdmiCecActiveSourceLostActivity extends TvBottomSheetActivity
        implements View.OnClickListener {
    private HdmiControlManager mHdmiControlManager;

    @Override
    public final void onCreate(Bundle b) {
        super.onCreate(b);
        mHdmiControlManager =
                (HdmiControlManager)
                        getApplicationContext().getSystemService(Context.HDMI_CONTROL_SERVICE);
        getWindow()
                .addPrivateFlags(
                        WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

        OnActiveSourceRecoveredBroadcastReceiver onActiveSourceRecoveredBroadcastReceiver =
                new OnActiveSourceRecoveredBroadcastReceiver();
        IntentFilter filter =
                new IntentFilter(HdmiControlManager.ACTION_ON_ACTIVE_SOURCE_RECOVERED_DISMISS_UI);
        getApplicationContext()
                .registerReceiver(
                        onActiveSourceRecoveredBroadcastReceiver,
                        filter,
                        Context.RECEIVER_EXPORTED);
        initUI();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bottom_sheet_negative_button) {
            mHdmiControlManager.setPowerStateChangeOnActiveSourceLost(
                    HdmiControlManager.POWER_STATE_CHANGE_ON_ACTIVE_SOURCE_LOST_NONE);
        }
        finish();
    }

    private void initUI() {
        TextView titleTextView = findViewById(R.id.bottom_sheet_title);
        TextView contentTextView = findViewById(R.id.bottom_sheet_body);
        ImageView icon = findViewById(R.id.bottom_sheet_icon);
        ImageView secondIcon = findViewById(R.id.bottom_sheet_second_icon);
        Button okButton = findViewById(R.id.bottom_sheet_positive_button);
        Button cancelButton = findViewById(R.id.bottom_sheet_negative_button);

        titleTextView.setText(getString(R.string.hdmi_cec_on_active_source_lost_title));
        contentTextView.setText(getString(R.string.hdmi_cec_on_active_source_lost_description));
        icon.setImageResource(R.drawable.ic_input_switch);
        secondIcon.setVisibility(View.GONE);

        okButton.setText(R.string.hdmi_cec_on_active_source_lost_ok);
        okButton.setOnClickListener(this);
        okButton.requestFocus();

        cancelButton.setText(R.string.hdmi_cec_on_active_source_lost_do_not_show);
        cancelButton.setOnClickListener(this);
    }

    private class OnActiveSourceRecoveredBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }
}

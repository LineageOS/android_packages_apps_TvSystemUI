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

package com.android.systemui.tv.media.settings;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.app.slice.Slice.HINT_PARTIAL;

import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_PREFERENCE_INFO_STATUS;
import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_PREFERENCE_KEY;
import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_SLICE_FOLLOWUP;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.tvsettings.TvSettingsEnums;
import android.content.ContentProviderClient;
import android.content.Intent;
import android.content.IntentSender;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.systemui.tv.res.R;

import com.android.tv.twopanelsettings.TwoPanelSettingsFragment.SliceFragmentCallback;
import com.android.tv.twopanelsettings.slices.EmbeddedSlicePreference;
import com.android.tv.twopanelsettings.slices.HasCustomContentDescription;
import com.android.tv.twopanelsettings.slices.HasSliceAction;
import com.android.tv.twopanelsettings.slices.HasSliceUri;
import com.android.tv.twopanelsettings.slices.SettingsPreferenceFragment;
import com.android.tv.twopanelsettings.slices.SlicePreference;
import com.android.tv.twopanelsettings.slices.SliceRadioPreference;
import com.android.tv.twopanelsettings.slices.SliceSeekbarPreference;
import com.android.tv.twopanelsettings.slices.SlicesConstants;
import com.android.tv.twopanelsettings.slices.ContextSingleton;
import com.android.tv.twopanelsettings.slices.compat.Slice;
import com.android.tv.twopanelsettings.slices.compat.SliceItem;
import com.android.tv.twopanelsettings.slices.compat.widget.ListContent;
import com.android.tv.twopanelsettings.slices.compat.widget.SliceContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A screen presenting a slice in TV settings.
 * Forked from {@link com.android.tv.twopanelsettings.slices.SliceFragment}.
 */
@Keep
public class SliceFragment extends SettingsPreferenceFragment implements Observer<Slice>,
        SliceFragmentCallback, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TAG = "SliceFragment";
    private static final boolean DEBUG = false;

    public static final String TAG_SCREEN_SUBTITLE = "TAG_SCREEN_SUBTITLE";

    // Keys for saving state
    private static final String KEY_PREFERENCE_FOLLOWUP_INTENT = "key_preference_followup_intent";
    private static final String KEY_PREFERENCE_FOLLOWUP_RESULT_CODE =
            "key_preference_followup_result_code";
    private static final String KEY_SCREEN_TITLE = "key_screen_title";
    private static final String KEY_SCREEN_SUBTITLE = "key_screen_subtitle";
    private static final String KEY_LAST_PREFERENCE = "key_last_preference";
    private static final String KEY_URI_STRING = "key_uri_string";

    private Slice mSlice;
    private String mUriString = null;
    private int mCurrentPageId;
    private CharSequence mScreenTitle;
    private CharSequence mScreenSubtitle;
    private PendingIntent mPreferenceFollowupIntent;
    private int mFollowupPendingIntentResultCode;
    private Intent mFollowupPendingIntentExtras;
    private Intent mFollowupPendingIntentExtrasCopy;
    private String mLastFocusedPreferenceKey;

    private final ActivityResultLauncher<IntentSenderRequest> mActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    new ActivityResultCallback<>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            Intent data = result.getData();
                            mFollowupPendingIntentExtras = data;
                            mFollowupPendingIntentExtrasCopy = data == null ? null : new Intent(
                                    data);
                            mFollowupPendingIntentResultCode = result.getResultCode();
                        }
                    });
    private final ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            handleUri(uri);
            super.onChange(selfChange, uri);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mUriString = getArguments().getString(SlicesConstants.TAG_TARGET_URI);
        if (!TextUtils.isEmpty(mUriString)) {
            ContextSingleton.getInstance().grantFullAccess(getContext(), Uri.parse(mUriString));
        }
        if (TextUtils.isEmpty(mScreenTitle)) {
            mScreenTitle = getArguments().getCharSequence(SlicesConstants.TAG_SCREEN_TITLE, "");
        }
        if (TextUtils.isEmpty(mScreenSubtitle)) {
            mScreenSubtitle = getArguments().getCharSequence(TAG_SCREEN_SUBTITLE, "");
        }
        super.onCreate(savedInstanceState);
        getPreferenceManager().setPreferenceComparisonCallback(
                new PreferenceManager.SimplePreferenceComparisonCallback() {
                    @Override
                    public boolean arePreferenceContentsTheSame(Preference preference1,
                            Preference preference2) {
                        // Should only check for the default SlicePreference objects, and ignore
                        // other instances of slice reference classes since they all override
                        // Preference.onBindViewHolder(PreferenceViewHolder)
                        return preference1.getClass() == SlicePreference.class
                                && super.arePreferenceContentsTheSame(preference1, preference2);
                    }
                });
    }

    @Override
    public final boolean onPreferenceStartFragment(PreferenceFragmentCompat caller,
            Preference pref) {
        if (DEBUG) Log.d(TAG, "onPreferenceStartFragment");
        if (pref.getFragment() != null) {
            if (pref instanceof SlicePreference) {
                SlicePreference slicePref = (SlicePreference) pref;
                if (slicePref.getUri() == null || !isUriValid(slicePref.getUri())) {
                    return false;
                }
                Bundle b = pref.getExtras();
                b.putString(SlicesConstants.TAG_TARGET_URI, slicePref.getUri());
                b.putCharSequence(SlicesConstants.TAG_SCREEN_TITLE, slicePref.getTitle());
                if (DEBUG) Log.d(TAG, "TAG_TARGET_URI: " + slicePref.getUri()
                        + ", TAG_SCREEN_TITLE: " + slicePref.getTitle());
            }
        }
        final Fragment f =
                Fragment.instantiate(getActivity(), pref.getFragment(), pref.getExtras());
        f.setTargetFragment(caller, 0);
        if (f instanceof PreferenceFragmentCompat || f instanceof PreferenceDialogFragmentCompat) {
            startPreferenceFragment(f);
        }
        return true;
    }

    public void startPreferenceFragment(@NonNull Fragment fragment) {
        if (DEBUG) Log.d(TAG, "startPreferenceFragment");

        getParentFragmentManager().beginTransaction()
                .replace(R.id.media_output_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        this.setTitle(mScreenTitle);
        this.setSubtitle(mScreenSubtitle);

        showProgressBar();
        if (!TextUtils.isEmpty(mUriString)) {
            ContextSingleton.getInstance()
                    .addSliceObserver(getActivity(), Uri.parse(mUriString), this);
        }

        super.onResume();
        if (!TextUtils.isEmpty(mUriString)) {
            getContext().getContentResolver().registerContentObserver(
                    SlicePreferencesUtil.getStatusPath(mUriString), false, mContentObserver);
        }
        fireFollowupPendingIntent();
    }

    private void fireFollowupPendingIntent() {
        if (mFollowupPendingIntentExtras == null) {
            return;
        }
        // If there is followup pendingIntent returned from initial activity, send it.
        // Otherwise send the followup pendingIntent provided by slice api.
        Parcelable followupPendingIntent;
        try {
            followupPendingIntent = mFollowupPendingIntentExtrasCopy.getParcelableExtra(
                    EXTRA_SLICE_FOLLOWUP);
        } catch (Throwable ex) {
            // unable to parse, the Intent has custom Parcelable, fallback
            followupPendingIntent = null;
        }
        if (followupPendingIntent instanceof PendingIntent) {
            try {
                ((PendingIntent) followupPendingIntent).send();
            } catch (CanceledException e) {
                Log.e(TAG, "Followup PendingIntent for slice cannot be sent", e);
            }
        } else {
            if (mPreferenceFollowupIntent == null) {
                return;
            }
            try {
                mPreferenceFollowupIntent.send(getContext(),
                        mFollowupPendingIntentResultCode, mFollowupPendingIntentExtras);
            } catch (CanceledException e) {
                Log.e(TAG, "Followup PendingIntent for slice cannot be sent", e);
            }
            mPreferenceFollowupIntent = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        hideProgressBar();
        getContext().getContentResolver().unregisterContentObserver(mContentObserver);
        if (!TextUtils.isEmpty(mUriString)) {
            ContextSingleton.getInstance()
                    .removeSliceObserver(getActivity(), Uri.parse(mUriString), this);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen preferenceScreen = getPreferenceManager()
                .createPreferenceScreen(getContext());
        setPreferenceScreen(preferenceScreen);
    }

    private boolean isUriValid(String uri) {
        if (uri == null) {
            return false;
        }
        ContentProviderClient client =
                getContext().getContentResolver().acquireContentProviderClient(Uri.parse(uri));
        if (client != null) {
            client.close();
            return true;
        } else {
            return false;
        }
    }

    private void update() {
        PreferenceScreen preferenceScreen =
                getPreferenceManager().getPreferenceScreen();

        if (preferenceScreen == null) {
            return;
        }

        List<SliceContent> items = new ListContent(mSlice).getRowItems();
        if (items.isEmpty()) {
            return;
        }

        SliceItem redirectSliceItem = SlicePreferencesUtil.getRedirectSlice(items);
        String redirectSlice = null;
        if (redirectSliceItem != null) {
            SlicePreferencesUtil.Data data = SlicePreferencesUtil.extract(redirectSliceItem);
            CharSequence title = SlicePreferencesUtil.getText(data.mTitleItem);
            if (!TextUtils.isEmpty(title)) {
                redirectSlice = title.toString();
            }
        }
        if (isUriValid(redirectSlice)) {
            ContextSingleton.getInstance()
                    .removeSliceObserver(getActivity(), Uri.parse(mUriString), this);
            getContext().getContentResolver().unregisterContentObserver(mContentObserver);
            mUriString = redirectSlice;
            ContextSingleton.getInstance()
                    .addSliceObserver(getActivity(), Uri.parse(mUriString), this);
            getContext().getContentResolver().registerContentObserver(
                    SlicePreferencesUtil.getStatusPath(mUriString), false, mContentObserver);
        }

        SliceItem screenTitleItem = SlicePreferencesUtil.getScreenTitleItem(items);
        if (screenTitleItem == null) {
            setTitle(mScreenTitle);
            setSubtitle(mScreenSubtitle);
        } else {
            SlicePreferencesUtil.Data data = SlicePreferencesUtil.extract(screenTitleItem);
            mCurrentPageId = SlicePreferencesUtil.getPageId(screenTitleItem);
            CharSequence title = SlicePreferencesUtil.getText(data.mTitleItem);
            if (!TextUtils.isEmpty(title)) {
                mScreenTitle = title;
            }
            setTitle(mScreenTitle);

            CharSequence subtitle = SlicePreferencesUtil.getText(data.mSubtitleItem);
            if (!TextUtils.isEmpty(subtitle)) {
                mScreenSubtitle = subtitle;
            }
            setSubtitle(subtitle);
        }

        SliceItem focusedPrefItem = SlicePreferencesUtil.getFocusedPreferenceItem(items);
        CharSequence defaultFocusedKey = null;
        if (focusedPrefItem != null) {
            SlicePreferencesUtil.Data data = SlicePreferencesUtil.extract(focusedPrefItem);
            CharSequence title = SlicePreferencesUtil.getText(data.mTitleItem);
            if (!TextUtils.isEmpty(title)) {
                defaultFocusedKey = title;
            }
        }

        List<Preference> newPrefs = new ArrayList<>();
        for (SliceContent contentItem : items) {
            SliceItem item = contentItem.getSliceItem();
            if (SlicesConstants.TYPE_PREFERENCE.equals(item.getSubType())
                    || SlicesConstants.TYPE_PREFERENCE_CATEGORY.equals(item.getSubType())
                    || SlicesConstants.TYPE_PREFERENCE_EMBEDDED_PLACEHOLDER.equals(
                    item.getSubType())) {
                Preference preference =
                        SlicePreferencesUtil.getPreference(
                                item, getContext(), getClass().getCanonicalName());
                if (preference != null) {
                    // Listen to changes of the seekbar.
                    if (preference instanceof SeekbarSlicePreference) {
                        SeekbarSlicePreference seekbarPreference =
                                (SeekbarSlicePreference) preference;
                        seekbarPreference.setOnSeekbarChangedListener(
                                new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int progress,
                                            boolean fromUser) {
                                        onSeekbarPreferenceValueChanged(seekbarPreference,
                                                progress);
                                    }

                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                        // NOOP
                                    }

                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                        // NOOP
                                    }
                                });
                    }
                    newPrefs.add(preference);
                }
            }
        }
        updatePreferenceScreen(preferenceScreen, newPrefs);
        if (defaultFocusedKey != null) {
            scrollToPreference(defaultFocusedKey.toString());
        } else if (mLastFocusedPreferenceKey != null) {
            scrollToPreference(mLastFocusedPreferenceKey);
        }
    }

    private void back() {
        if (DEBUG) Log.d(TAG, "back");
        getParentFragmentManager().popBackStack();
    }

    private void updatePreferenceScreen(PreferenceScreen screen, List<Preference> newPrefs) {
        // Remove all the preferences in the screen that satisfy such three cases:
        // (a) Preference without key
        // (b) Preference with key which does not appear in the new list.
        // (c) Preference with key which does appear in the new list, but the preference has changed
        // ability to handle slices and needs to be replaced instead of re-used.
        int index = 0;
        IdentityHashMap<Preference, Preference> newToOld = new IdentityHashMap<>();
        while (index < screen.getPreferenceCount()) {
            boolean needToRemoveCurrentPref = true;
            Preference oldPref = screen.getPreference(index);
            for (Preference newPref : newPrefs) {
                if (isSamePreference(oldPref, newPref)) {
                    needToRemoveCurrentPref = false;
                    newToOld.put(newPref, oldPref);
                    break;
                }
            }

            if (needToRemoveCurrentPref) {
                screen.removePreference(oldPref);
            } else {
                index++;
            }
        }

        Map<Integer, Boolean> twoStatePreferenceIsCheckedByOrder = new HashMap<>();
        for (int i = 0; i < newPrefs.size(); i++) {
            if (newPrefs.get(i) instanceof TwoStatePreference) {
                twoStatePreferenceIsCheckedByOrder.put(
                        i, ((TwoStatePreference) newPrefs.get(i)).isChecked());
            }
        }

        //Iterate the new preferences list and give each preference a correct order
        for (int i = 0; i < newPrefs.size(); i++) {
            Preference newPref = newPrefs.get(i);
            // If the newPref has a key and has a corresponding old preference, update the old
            // preference and give it a new order.

            Preference oldPref = newToOld.get(newPref);
            if (oldPref == null) {
                newPref.setOrder(i);
                screen.addPreference(newPref);
                continue;
            }

            oldPref.setOrder(i);
            if (oldPref instanceof EmbeddedSlicePreference) {
                // EmbeddedSlicePreference has its own slice observer
                // (EmbeddedSlicePreferenceHelper). Should therefore not be updated by
                // slice observer in SliceFragment.
                // The order will however still need to be updated, as this can not be handled
                // by EmbeddedSlicePreferenceHelper.
                continue;
            }

            oldPref.setTitle(newPref.getTitle());
            oldPref.setSummary(newPref.getSummary());
            oldPref.setEnabled(newPref.isEnabled());
            oldPref.setSelectable(newPref.isSelectable());
            oldPref.setFragment(newPref.getFragment());
            oldPref.getExtras().putAll(newPref.getExtras());
            if ((oldPref instanceof HasSliceAction)
                    && (newPref instanceof HasSliceAction)) {
                ((HasSliceAction) oldPref)
                        .setSliceAction(
                                ((HasSliceAction) newPref).getSliceAction());
            }
            if ((oldPref instanceof HasSliceUri)
                    && (newPref instanceof HasSliceUri)) {
                ((HasSliceUri) oldPref)
                        .setUri(((HasSliceUri) newPref).getUri());
            }
            if ((oldPref instanceof HasCustomContentDescription)
                    && (newPref instanceof HasCustomContentDescription)) {
                ((HasCustomContentDescription) oldPref).setContentDescription(
                        ((HasCustomContentDescription) newPref)
                                .getContentDescription());
            }
        }

        //addPreference will reset the checked status of TwoStatePreference.
        //So we need to add them back
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference screenPref = screen.getPreference(i);
            if (screenPref instanceof TwoStatePreference
                    && twoStatePreferenceIsCheckedByOrder.get(screenPref.getOrder()) != null) {
                ((TwoStatePreference) screenPref)
                        .setChecked(twoStatePreferenceIsCheckedByOrder.get(screenPref.getOrder()));
            }
        }
    }

    private static boolean isSamePreference(Preference oldPref, Preference newPref) {
        if (oldPref == null || newPref == null) {
            return false;
        }

        if (newPref instanceof HasSliceUri != oldPref instanceof HasSliceUri) {
            return false;
        }

        if (newPref instanceof EmbeddedSlicePreference) {
            return oldPref instanceof EmbeddedSlicePreference
                    && Objects.equals(((EmbeddedSlicePreference) newPref).getUri(),
                    ((EmbeddedSlicePreference) oldPref).getUri());
        } else if (oldPref instanceof EmbeddedSlicePreference) {
            return false;
        }

        return newPref.getKey() != null && newPref.getKey().equals(oldPref.getKey());
    }

    @Override
    public void onPreferenceFocused(Preference preference) {
        setLastFocused(preference);
    }

    @Override
    public void onSeekbarPreferenceChanged(SliceSeekbarPreference preference, int addValue) {
        if (DEBUG) Log.d(TAG, "onSeekbarPreferenceChanged, addValue: " + addValue);
        int curValue = preference.getValue();
        onSeekbarPreferenceValueChanged(preference, curValue);
    }

    public void onSeekbarPreferenceValueChanged(SliceSeekbarPreference preference, int newValue) {
        if (DEBUG) Log.d(TAG, "onSeekbarPreferenceChanged, newValue: " + newValue);

        try {
            Intent fillInIntent =
                    new Intent()
                            .putExtra(EXTRA_PREFERENCE_KEY, preference.getKey())
                            .putExtra(SlicesConstants.SUBTYPE_SEEKBAR_VALUE, newValue);
            firePendingIntent(preference, fillInIntent);
        } catch (Exception e) {
            Log.e(TAG, "PendingIntent for slice cannot be sent", e);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof SliceRadioPreference) {
            SliceRadioPreference radioPref = (SliceRadioPreference) preference;
            if (!radioPref.isChecked()) {
                radioPref.setChecked(true);
                if (TextUtils.isEmpty(radioPref.getUri())) {
                    return true;
                }
            }

            Intent fillInIntent =
                    new Intent().putExtra(EXTRA_PREFERENCE_KEY, preference.getKey());
            boolean result = firePendingIntent(radioPref, fillInIntent);
            radioPref.clearOtherRadioPreferences(getPreferenceScreen());
            if (result) {
                return true;
            }
        } else if (preference instanceof TwoStatePreference
                && preference instanceof HasSliceAction) {
            boolean isChecked = ((TwoStatePreference) preference).isChecked();
            preference.getExtras().putBoolean(EXTRA_PREFERENCE_INFO_STATUS, isChecked);
            Intent fillInIntent =
                    new Intent()
                            .putExtra(EXTRA_TOGGLE_STATE, isChecked)
                            .putExtra(EXTRA_PREFERENCE_KEY, preference.getKey());
            if (firePendingIntent((HasSliceAction) preference, fillInIntent)) {
                return true;
            }
            return true;
        } else if (preference instanceof SlicePreference) {
            Intent fillInIntent =
                    new Intent().putExtra(EXTRA_PREFERENCE_KEY, preference.getKey());
            if (firePendingIntent((HasSliceAction) preference, fillInIntent)) {
                return true;
            }
        }

        return super.onPreferenceTreeClick(preference);
    }

    private boolean firePendingIntent(@NonNull HasSliceAction preference, Intent fillInIntent) {
        if (preference.getSliceAction() == null) {
            return false;
        }
        IntentSender intentSender = preference.getSliceAction().getAction().getIntentSender();
        mActivityResultLauncher.launch(
                new IntentSenderRequest.Builder(intentSender).setFillInIntent(
                        fillInIntent).build());
        if (preference.getFollowupSliceAction() != null) {
            mPreferenceFollowupIntent = preference.getFollowupSliceAction().getAction();
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PREFERENCE_FOLLOWUP_INTENT, mPreferenceFollowupIntent);
        outState.putInt(KEY_PREFERENCE_FOLLOWUP_RESULT_CODE, mFollowupPendingIntentResultCode);
        outState.putCharSequence(KEY_SCREEN_TITLE, mScreenTitle);
        outState.putCharSequence(KEY_SCREEN_SUBTITLE, mScreenSubtitle);
        outState.putString(KEY_LAST_PREFERENCE, mLastFocusedPreferenceKey);
        outState.putString(KEY_URI_STRING, mUriString);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mPreferenceFollowupIntent =
                    savedInstanceState.getParcelable(KEY_PREFERENCE_FOLLOWUP_INTENT);
            mFollowupPendingIntentResultCode =
                    savedInstanceState.getInt(KEY_PREFERENCE_FOLLOWUP_RESULT_CODE);
            mScreenTitle = savedInstanceState.getCharSequence(KEY_SCREEN_TITLE);
            mScreenSubtitle = savedInstanceState.getCharSequence(KEY_SCREEN_SUBTITLE);
            mLastFocusedPreferenceKey = savedInstanceState.getString(KEY_LAST_PREFERENCE);
            mUriString = savedInstanceState.getString(KEY_URI_STRING);
        }
    }

    @Override
    public void onChanged(Slice slice) {
        mSlice = slice;
        // Make TvSettings guard against the case that slice provider is not set up correctly
        if (slice == null || slice.getHints() == null) {
            return;
        }

        if (slice.getHints().contains(HINT_PARTIAL)) {
            showProgressBar();
        } else {
            hideProgressBar();
        }
        update();
    }

    private void showProgressBar() {
        View view = this.getView();
        View progressBar = view == null ? null : getView().findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.bringToFront();
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar() {
        View view = this.getView();
        View progressBar = view == null ? null : getView().findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setSubtitle(CharSequence subtitle) {
        View view = this.getView();
        if (view == null) {
            return;
        }
        TextView decorSubtitle = view.findViewById(R.id.decor_subtitle);
        if (decorSubtitle != null) {
            if (TextUtils.isEmpty(subtitle)) {
                decorSubtitle.setVisibility(View.GONE);
            } else {
                decorSubtitle.setVisibility(View.VISIBLE);
                decorSubtitle.setText(subtitle);
            }
        }
        mScreenSubtitle = subtitle;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup view =
                (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);

        LayoutInflater themedInflater = LayoutInflater.from(getContext());

        final View newTitleContainer = themedInflater.inflate(
                R.layout.media_output_settings_title, null);
        if (newTitleContainer != null) {
            newTitleContainer.setOutlineProvider(null);
        }
        view.removeView(
                view.findViewById(androidx.leanback.preference.R.id.decor_title_container));
        view.addView(newTitleContainer, 0);
        view.setBackgroundResource(android.R.color.transparent);

        final View newContainer =
                themedInflater.inflate(R.layout.media_output_settings_progress, null);
        if (newContainer != null) {
            ((ViewGroup) newContainer).addView(view);
        }
        return newContainer;
    }

    public void setLastFocused(Preference preference) {
        mLastFocusedPreferenceKey = preference.getKey();
    }

    private void handleUri(Uri uri) {
        String uriString = uri.getQueryParameter(SlicesConstants.PARAMETER_URI);
        String errorMessage = uri.getQueryParameter(SlicesConstants.PARAMETER_ERROR);
        if (DEBUG) Log.d(TAG, "handleUri: " + uri);

        if (errorMessage != null) {
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
        }
        // Provider should provide the correct slice uri in the parameter if it wants to do certain
        // action(includes go back, forward), otherwise TvSettings would ignore it.
        if (uriString == null || !uriString.equals(mUriString)) {
            return;
        }
        String direction = uri.getQueryParameter(SlicesConstants.PARAMETER_DIRECTION);
        if (DEBUG) Log.d(TAG, "direction: " + direction);
        if (direction != null) {
            if (direction.equals(SlicesConstants.BACKWARD)) {
                back();
            } else if (direction.equals(SlicesConstants.EXIT)) {
                finish();
            }
        }
    }

    private void finish() {
        if (getActivity() != null) {
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    }

    private int getPreferenceActionId(Preference preference) {
        if (preference instanceof HasSliceAction) {
            return ((HasSliceAction) preference).getActionId() != 0
                    ? ((HasSliceAction) preference).getActionId()
                    : TvSettingsEnums.ENTRY_DEFAULT;
        }
        return TvSettingsEnums.ENTRY_DEFAULT;
    }

    @Override
    protected int getPageId() {
        return mCurrentPageId != 0 ? mCurrentPageId : TvSettingsEnums.PAGE_SLICE_DEFAULT;
    }

    @Deprecated
    public int getMetricsCategory() {
        return 0;
    }
}

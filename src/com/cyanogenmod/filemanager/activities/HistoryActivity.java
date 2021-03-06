/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.HighlightedSimpleMenuListAdapter;
import com.cyanogenmod.filemanager.adapters.HistoryAdapter;
import com.cyanogenmod.filemanager.adapters.SimpleMenuListAdapter;
import com.cyanogenmod.filemanager.model.History;
import com.cyanogenmod.filemanager.ui.widgets.ButtonItem;
import com.cyanogenmod.filemanager.util.AndroidHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An activity for show navigation history.
 */
public class HistoryActivity extends Activity implements OnItemClickListener {

    private static final String TAG = "HistoryActivity"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    /**
     * @hide
     */
    ListView mListView;
    /**
     * @hide
     */
    HistoryAdapter mAdapter;
    /**
     * @hide
     */
    boolean mIsEmpty;
    private boolean mIsClearHistory;

    private View mOptionsAnchorView;

    /**
     * Intent extra parameter for the history data.
     */
    public static final String EXTRA_HISTORY_LIST = "extra_history_list";  //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {
        if (DEBUG) {
            Log.d(TAG, "NavigationActivity.onCreate"); //$NON-NLS-1$
        }

        this.mIsEmpty = false;
        this.mIsClearHistory = false;

        //Set in transition
        overridePendingTransition(R.anim.translate_to_right_in, R.anim.hold_out);

        //Set the main layout of the activity
        setContentView(R.layout.history);

        //Initialize action bars and data
        initTitleActionBar();
        initHistory();

        //Save state
        super.onCreate(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        //Set out transition
        overridePendingTransition(R.anim.hold_in, R.anim.translate_to_left_out);
        super.onPause();
    }

    /**
     * Method that initializes the titlebar of the activity.
     */
    private void initTitleActionBar() {
        //Configure the action bar options
        getActionBar().setBackgroundDrawable(
                getResources().getDrawable(R.drawable.bg_holo_titlebar));
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        View customTitle = getLayoutInflater().inflate(R.layout.simple_customtitle, null, false);
        TextView title = (TextView)customTitle.findViewById(R.id.customtitle_title);
        title.setText(R.string.history);
        title.setContentDescription(getString(R.string.history));
        ButtonItem configuration = (ButtonItem)customTitle.findViewById(R.id.ab_button1);
        configuration.setImageResource(R.drawable.ic_holo_light_overflow);
        configuration.setContentDescription(getString(R.string.actionbar_button_overflow_cd));

        View status = findViewById(R.id.history_status);
        boolean showOptionsMenu = AndroidHelper.showOptionsMenu(getApplicationContext());
        configuration.setVisibility(showOptionsMenu ? View.VISIBLE : View.GONE);
        this.mOptionsAnchorView = showOptionsMenu ? configuration : status;

        getActionBar().setCustomView(customTitle);
    }

    /**
     * Method invoked when an action item is clicked.
     *
     * @param view The button pushed
     */
    public void onActionBarItemClick(View view) {
        switch (view.getId()) {
            case R.id.ab_button1:
                //Overflow
                showOverflowPopUp(view);
                break;

            default:
                break;
        }
    }

    /**
     * Method that initializes the titlebar of the activity.
     */
    @SuppressWarnings("unchecked")
    private void initHistory() {
        // Retrieve the loading view
        final View waiting = findViewById(R.id.history_waiting);

        // Load the history in background
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            Exception mCause;
            List<History> mHistory;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    this.mHistory =
                            (List<History>)getIntent().getSerializableExtra(EXTRA_HISTORY_LIST);
                    if (this.mHistory.isEmpty()) {
                        View msg = findViewById(R.id.history_empty_msg);
                        msg.setVisibility(View.VISIBLE);
                        return Boolean.TRUE;
                    }
                    HistoryActivity.this.mIsEmpty = this.mHistory.isEmpty();

                    //Show inverted history
                    final List<History> adapterList = new ArrayList<History>(this.mHistory);
                    Collections.reverse(adapterList);

                    HistoryActivity.this.mListView =
                            (ListView)findViewById(R.id.history_listview);
                    HistoryActivity.this.mAdapter =
                            new HistoryAdapter(HistoryActivity.this, adapterList);

                    return Boolean.TRUE;

                } catch (Exception e) {
                    this.mCause = e;
                    return Boolean.FALSE;
                }
            }

            @Override
            protected void onPreExecute() {
                waiting.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                waiting.setVisibility(View.GONE);
                if (result.booleanValue()) {
                    if (HistoryActivity.this.mListView != null &&
                        HistoryActivity.this.mAdapter != null) {

                        HistoryActivity.this.mListView.
                            setAdapter(HistoryActivity.this.mAdapter);
                        HistoryActivity.this.mListView.
                            setOnItemClickListener(HistoryActivity.this);
                    }

                } else {
                    if (this.mCause != null) {
                        ExceptionUtil.translateException(HistoryActivity.this, this.mCause);
                    }
                }
            }

            @Override
            protected void onCancelled() {
                waiting.setVisibility(View.GONE);
            }
        };
        task.execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (!this.mIsEmpty) {
                    showOverflowPopUp(this.mOptionsAnchorView);
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                back(true, null);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
          case android.R.id.home:
              back(true, null);
              return true;
          default:
             return super.onOptionsItemSelected(item);
       }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        History history = ((HistoryAdapter)parent.getAdapter()).getItem(position);
        back(false, history);
    }

    /**
     * Method that returns to previous activity and.
     *
     * @param cancelled Indicates if the activity was cancelled
     * @param history The selected history
     */
    private void back(final boolean cancelled, final History history) {
        Intent intent =  new Intent();
        if (cancelled) {
            if (this.mIsClearHistory) {
                intent.putExtra(NavigationActivity.EXTRA_HISTORY_CLEAR, true);
            }
            setResult(RESULT_CANCELED, intent);
        } else {
            intent.putExtra(NavigationActivity.EXTRA_HISTORY_ENTRY_SELECTION, history);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    /**
     * Method that clean the history and return back to navigation view
     *  @hide
     */
    void clearHistory() {
        if (this.mAdapter != null) {
            this.mAdapter.clear();
            this.mAdapter.notifyDataSetChanged();
            View msg = findViewById(R.id.history_empty_msg);
            msg.setVisibility(View.VISIBLE);
            this.mIsClearHistory = true;
        }
    }

    /**
     * Method that shows a popup with the activity main menu.
     *
     * @param anchor The anchor of the popup
     */
    private void showOverflowPopUp(View anchor) {
        SimpleMenuListAdapter adapter =
                new HighlightedSimpleMenuListAdapter(this, R.menu.history);
        final ListPopupWindow popup =
                DialogHelper.createListPopupWindow(this, adapter, anchor);
        popup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(
                    final AdapterView<?> parent, final View v,
                    final int position, final long id) {
                final int itemId = (int)id;
                switch (itemId) {
                    case R.id.mnu_clear_history:
                        popup.dismiss();
                        clearHistory();
                        break;
                }
            }
        });
        popup.show();
    }
}

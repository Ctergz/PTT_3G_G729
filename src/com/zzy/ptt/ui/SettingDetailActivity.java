/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:SettingDetailActivity.java
 * Description:SettingDetailActivity
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-6
 */

package com.zzy.ptt.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.zzy.ptt.R;
import com.zzy.ptt.application.MyApplication;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class SettingDetailActivity extends BaseActivity implements
		OnClickListener, OnSeekBarChangeListener, OnCheckedChangeListener {

	private Intent intent;
	private int currentItemId;

	// for register setting

	private CheckBox cbAutoRegister;
	private SharedPreferences prefs;

	private Spinner groupChoseSpinner;
	private TextView groupChosetv;

	// for call setting
	private RadioButton radioHandset, radioSpeaker;
	// private CheckBox cbAutoAnswer;
	// for ringtone setting
	private SeekBar callVolumeBar, pttVolumeBar, ringVolumeBar;
	private CheckBox cbRing, cbVirabate;
	private TextView tvCurrentRingtone;
	private Button btnCallRing;
	private MediaPlayer player;
	// for system setting
	private CheckBox cbAutoStart;

	private BroadcastReceiver pttReceiver;

	private static final String LOG_TAG = "SettingDetailActivity";
	private static boolean bDebug = true;

	private static final int MENU_RETURN = 0;
	private static final int MENU_REREGISTER = 1;

	private AudioManager audioManager = null;
	private PTTUtil pttUtil = PTTUtil.getInstance();

	// add by wangjunhui
	private boolean bDirty = false;
	private ProgressDialog progressDialog;
	private String[] currentGroups;
	private List<GroupInfo> lstGroupData;
	private List<String> lstGroupNum;
	private ArrayAdapter<String> adapter;
	private SharedPreferences sp;
	private Editor editor;
	private String currGrpNum = null;

	public static SettingDetailActivity instance = null;

	// gis setting
	private CheckBox openGisBox;
	private RadioButton mobileBox, gpsBox;
	private LinearLayout gisChoseLayout, gisTimeLayout, gisStartLayout;
	private Button startStopButton;
	private EditText gisTime;
	// baidu location
	private LocationMode mLocationMode;
	private LocationClient mLocClient;
	private boolean mLocationInit;
	public static final int LOCATION_MODEL_MOBIL = 0;
	public static final int LOCATION_MODEL_GPS = 1;
	private int gisRequestLocationTime = 60;

	public String getCurrGrpNum() {
		return this.currGrpNum;
	}

	public void setCurrGrpNum(String currGrpNum) {
		this.currGrpNum = currGrpNum;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PTTUtil.getInstance().initOnCreat(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		intent = getIntent();
		currentItemId = intent.getIntExtra(PTTConstant.SETTING_DISPATCH_KEY, 0);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		showView(currentItemId);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_ptt, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return super.onOptionsItemSelected(item);
	}

	private void showView(int itemId) {

		switch (itemId) {
		case PTTConstant.SETTING_ITEM_REGISTGER:
			IntentFilter filter = new IntentFilter(PTTConstant.ACTION_REGISTER);
			break;
		case PTTConstant.SETTING_ITEM_TALKING:
			setContentView(R.layout.call_setting_layout);
			this.setTitle(getApplicationContext().getString(
					R.string.setting_talking));
			loadCallSettingData();
			addPttReceiver();
			filter = new IntentFilter(PTTConstant.ACTION_PTT);
			registerReceiver(pttReceiver, filter);
			filter = new IntentFilter(PTTConstant.ACTION_DYNAMIC_REGRP);
			registerReceiver(pttReceiver, filter);
			break;
		case PTTConstant.SETTING_ITEM_ALERTRING:
			setContentView(R.layout.ringtone_setting_layout);
			this.setTitle(getApplicationContext().getString(
					R.string.setting_alertring));
			loadRingData();
			break;
		case PTTConstant.SETTING_ITEM_SYSTEM:
			setContentView(R.layout.system_setting_layout);
			this.setTitle(getApplicationContext().getString(
					R.string.setting_system));
			loadSystemData();
			break;
		case PTTConstant.SETTING_ITEM_GIS:
			setContentView(R.layout.gis_setting_layout);
			this.setTitle(getApplicationContext().getString(
					R.string.setting_gis));
			loadGisData();
			break;
		default:
			break;
		}
	}

	private void loadGisData() {
		mLocClient = ((MyApplication) getApplication()).mLocationClient;
		gisChoseLayout = (LinearLayout) findViewById(R.id.gis_chose_layout);
		gisTimeLayout = (LinearLayout) findViewById(R.id.gis_time_layout);
		gisStartLayout = (LinearLayout) findViewById(R.id.gis_start_layout);
		startStopButton = (Button) findViewById(R.id.gis_start_button);
		gisTime = (EditText) findViewById(R.id.gis_send_time_editText);
		openGisBox = (CheckBox) findViewById(R.id.open_gis);
		mobileBox = (RadioButton) findViewById(R.id.gis_chose_mobile);
		gpsBox = (RadioButton) findViewById(R.id.gis_chose_gps);
		editor = prefs.edit();
		boolean isChecked = prefs.getBoolean(PTTConstant.SP_OPEN_GIS, false);
		boolean isMobileChecked = prefs.getBoolean(PTTConstant.SP_GIS_MOBILE,
				false);
		boolean isGpsChecked = prefs.getBoolean(PTTConstant.SP_GIS_GPS, false);
		int gisSendTime = prefs
				.getInt(PTTConstant.SP_GIS_LOCATION_SEND_TIME, 0);
		if (gisSendTime > 0) {
			gisTime.setText(String.valueOf(gisSendTime));
		} else {
			gisTime.setText(String.valueOf(gisRequestLocationTime));
			editor.putInt(PTTConstant.SP_GIS_LOCATION_SEND_TIME, 60);
		}
		if (!isMobileChecked && !isGpsChecked) {
			editor.putBoolean(PTTConstant.SP_GIS_MOBILE, true);
			isMobileChecked = true;
		}
		openGisBox.setChecked(isChecked);
		if (isChecked) {
			gisChoseLayout.setVisibility(View.VISIBLE);
			gisTimeLayout.setVisibility(View.VISIBLE);
			gisStartLayout.setVisibility(View.VISIBLE);
			mobileBox.setChecked(isMobileChecked);
			gpsBox.setChecked(isGpsChecked);
		} else {
			gisStartLayout.setVisibility(View.GONE);
			gisChoseLayout.setVisibility(View.GONE);
			gisTimeLayout.setVisibility(View.GONE);
		}
		openGisBox.setOnCheckedChangeListener(this);
		mobileBox.setOnClickListener(this);
		gpsBox.setOnClickListener(this);
		startStopButton.setOnClickListener(this);
		editor.commit();
	}

	private void loadSystemData() {

		cbAutoStart = (CheckBox) findViewById(R.id.cb_auto_start);
		cbAutoStart.setChecked(prefs.getBoolean(
				getString(R.string.sp_auto_start), true));
		cbAutoStart.setOnCheckedChangeListener(this);
	}

	private void loadRingData() {

		player = new MediaPlayer();

		sp = PTTService.instance.prefs;
		editor = sp.edit();

		callVolumeBar = (SeekBar) findViewById(R.id.seekbar_call_single_volume);
		pttVolumeBar = (SeekBar) findViewById(R.id.seekbar_call_ptt_volume);
		ringVolumeBar = (SeekBar) findViewById(R.id.seekbar_call_ring_volume);

		cbRing = (CheckBox) findViewById(R.id.cb_cond_ring);
		cbVirabate = (CheckBox) findViewById(R.id.cb_cond_virabate);
		tvCurrentRingtone = (TextView) findViewById(R.id.tv_current_ring);
		btnCallRing = (Button) findViewById(R.id.btn_choose_ringtone);

		int maxVolume = audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		ringVolumeBar.setMax(maxVolume);
		int volumeLevle = prefs.getInt(getString(R.string.sp_call_ring_volume),
				-1);
		if (volumeLevle == -1) {
			Editor editor = prefs.edit();
			editor.putInt(getString(R.string.sp_call_ring_volume), maxVolume);
			editor.commit();
			volumeLevle = maxVolume;
		}
		ringVolumeBar.setProgress(volumeLevle);

		maxVolume = audioManager
				.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		callVolumeBar.setMax(maxVolume);
		volumeLevle = prefs.getInt(getString(R.string.sp_call_single_volume),
				-1);
		if (volumeLevle == -1) {
			Editor editor = prefs.edit();
			editor.putInt(getString(R.string.sp_call_single_volume), maxVolume);
			editor.commit();
			volumeLevle = maxVolume;
		}
		callVolumeBar.setProgress(volumeLevle);

		maxVolume = audioManager
				.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		volumeLevle = prefs.getInt(getString(R.string.sp_call_ptt_volume), -1);
		pttVolumeBar.setMax(maxVolume);
		if (volumeLevle == -1) {
			Editor editor = prefs.edit();
			editor.putInt(getString(R.string.sp_call_ptt_volume), maxVolume);
			editor.commit();
			volumeLevle = maxVolume;
		}
		pttVolumeBar.setProgress(volumeLevle);

		cbRing.setChecked(prefs.getBoolean(getString(R.string.sp_ring_enable),
				true));
		cbVirabate.setChecked(prefs.getBoolean(
				getString(R.string.sp_virabate_enable), true));
		btnCallRing.setEnabled(cbRing.isChecked());

		String currentRing = prefs.getString(
				getString(R.string.sp_current_ringtone), null);
		if (currentRing == null || currentRing.length() == 0) {
			currentRing = PTTUtil.getInstance().getFirstRingName(
					getResources(), "call_");
		}
		tvCurrentRingtone.setText(currentRing);

		callVolumeBar.setOnSeekBarChangeListener(this);
		pttVolumeBar.setOnSeekBarChangeListener(this);
		ringVolumeBar.setOnSeekBarChangeListener(this);
		cbRing.setOnCheckedChangeListener(this);
		cbVirabate.setOnCheckedChangeListener(this);
		btnCallRing.setOnClickListener(this);
	}

	private void loadCallSettingData() {

		// cbAutoAnswer = (CheckBox) findViewById(R.id.cb_auto_answer);
		radioHandset = (RadioButton) findViewById(R.id.id_ar_handset);
		radioSpeaker = (RadioButton) findViewById(R.id.id_ar_speaker);

		// cbAutoAnswer.setChecked(prefs.getBoolean(getString(R.string.sp_auto_answer),
		// false));
		radioHandset.setChecked(prefs.getBoolean(
				getString(R.string.sp_ar_handset), true));
		radioSpeaker.setChecked(prefs.getBoolean(
				getString(R.string.sp_ar_speaker), false));

		radioHandset.setOnClickListener(this);
		radioSpeaker.setOnClickListener(this);
		// cbAutoAnswer.setOnClickListener(this);
		// add by wangjunhui
		groupChoseSpinner = (Spinner) findViewById(R.id.group_chose_Spinner);
		groupChosetv = (TextView) findViewById(R.id.group_chose_tv);
		sp = PTTService.instance.prefs;
		editor = sp.edit();
		lstGroupData = GroupManager.getInstance().getGroupData();
		lstGroupNum = new ArrayList<String>();
		currentGroups = new String[lstGroupData.size()];
		for (int i = 0; i < lstGroupData.size(); i++) {
			currentGroups[i] = lstGroupData.get(i).getName() + "--("
					+ lstGroupData.get(i).getNumber() + ")";
			lstGroupNum.add(lstGroupData.get(i).getNumber());
		}
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, currentGroups);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		groupChoseSpinner.setAdapter(adapter);
		if (sp.getString(PTTConstant.SP_CURR_GRP_NUM, "").trim().length() != 0
				&& lstGroupNum.indexOf(sp.getString(
						PTTConstant.SP_CURR_GRP_NUM, "")) != -1) {
			groupChoseSpinner.setSelection(lstGroupNum.indexOf(sp.getString(
					PTTConstant.SP_CURR_GRP_NUM, "")), true);
			groupChosetv.setText(getApplicationContext().getString(
					R.string.setting_current_group));
		} else if (lstGroupData.size() != 0) {
			groupChoseSpinner.setSelection(0, true);
			editor.putString(PTTConstant.SP_CURR_GRP_NUM, lstGroupData.get(0)
					.getNumber());
			editor.commit();
			setCurrGrpNum(lstGroupData.get(0).getNumber());
			groupChosetv.setText(getApplicationContext().getString(
					R.string.setting_current_group));
		} else {
			adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, new String[] { "" });
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			groupChoseSpinner.setAdapter(adapter);
			groupChoseSpinner.setEnabled(false);
		}
		groupChoseSpinner.setOnItemSelectedListener(spinnerListener);
		groupChoseSpinner.setVisibility(View.VISIBLE);
	}

	// add by wangjunhui
	private Spinner.OnItemSelectedListener spinnerListener = new Spinner.OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> arg0, View v, int position,
				long arg3) {
			if (lstGroupData.size() != 0) {
				groupChosetv.setText(getApplicationContext().getString(
						R.string.setting_current_group));
				editor.putString(PTTConstant.SP_CURR_GRP_NUM,
						lstGroupData.get(position).getNumber());
				editor.commit();
				setCurrGrpNum(lstGroupData.get(position).getNumber());
			} else {
				groupChosetv.setText(getApplicationContext().getString(
						R.string.setting_current_group));
				groupChoseSpinner.setEnabled(false);
			}
		}

		public void onNothingSelected(AdapterView<?> arg0) {

		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
		bDirty = false;
		if (pttReceiver != null) {
			unregisterReceiver(pttReceiver);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		super.onKeyDown(keyCode, event);

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			this.finish();
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void addPttReceiver() {
		pttReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action == null || action.trim().length() == 0) {
					return;
				}

				if (action.equals(PTTConstant.ACTION_PTT)) {
					if (PTTManager.getInstance().getCurrentPttState()
							.getStatus() == PTTConstant.PTT_IDLE) {
						groupChoseSpinner.setEnabled(true);
					}
				}
				if (action.equals(PTTConstant.ACTION_DYNAMIC_REGRP)) {
					loadCallSettingData();
				}
			}
		};
	}

	@Override
	public void onClick(View v) {
		Editor editor = prefs.edit();
		if (v == radioHandset || v == radioSpeaker) {
			editor.putBoolean(getString(R.string.sp_ar_handset),
					radioHandset.isChecked());
			editor.putBoolean(getString(R.string.sp_ar_speaker),
					radioSpeaker.isChecked());
		} /*
		 * else if (v == cbAutoAnswer) {
		 * editor.putBoolean(getString(R.string.sp_auto_answer),
		 * cbAutoAnswer.isChecked()); }
		 */
		if (v == mobileBox) {
			editor.putBoolean(PTTConstant.SP_GIS_MOBILE, mobileBox.isChecked());
			editor.putBoolean(PTTConstant.SP_GIS_GPS, gpsBox.isChecked());
			editor.putInt(PTTConstant.SP_GIS_LOCATION_MODE,
					LOCATION_MODEL_MOBIL);
		}
		if (v == gpsBox) {
			if (isGpsOpen()) {
				editor.putBoolean(PTTConstant.SP_GIS_MOBILE,
						mobileBox.isChecked());
				editor.putBoolean(PTTConstant.SP_GIS_GPS, gpsBox.isChecked());
				editor.putInt(PTTConstant.SP_GIS_LOCATION_MODE,
						LOCATION_MODEL_GPS);
			} else {
				mobileBox.setChecked(true);
				gpsBox.setChecked(false);
				editor.putBoolean(PTTConstant.SP_GIS_MOBILE, true);
				editor.putBoolean(PTTConstant.SP_GIS_GPS, false);
				AlertDialog.Builder builder = new AlertDialog.Builder(instance);
				builder.setTitle("打开GPS")
						.setMessage("打开GPS定位功能?")
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										Intent intent = new Intent();
										intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
										intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										try {
											instance.startActivity(intent);

										} catch (ActivityNotFoundException ex) {
											intent.setAction(Settings.ACTION_SETTINGS);
											try {
												instance.startActivity(intent);
											} catch (Exception e) {
											}
										}
									}
								})
						.setNegativeButton("取消",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
									}
								}).show();
			}
		}
		if (v == startStopButton) {
			if (TextUtils.isEmpty(gisTime.getText())) {
				Toast.makeText(this, "定位请求时间未设置！", Toast.LENGTH_SHORT).show();
				return;
			}
			if (startStopButton.getText().equals(
					instance.getString(R.string.setting_gis_start))) {
				startStopButton.setText(instance
						.getString(R.string.setting_gis_stop));
				gisRequestLocationTime = Integer.parseInt(gisTime.getText()
						.toString().trim());
				editor.putInt(PTTConstant.SP_GIS_LOCATION_SEND_TIME,
						gisRequestLocationTime);
				getLocationParams();
				setLocationOption();
				// 开始定位
				if (mLocationInit) {
					mLocClient.start();
				} else {
					startStopButton.setText(instance
							.getString(R.string.setting_gis_start));
					Toast.makeText(this, "定位参数错误，无法定位", Toast.LENGTH_SHORT)
							.show();
					((MyApplication) getApplication()).mLocationClient.stop();
					editor.putBoolean(PTTConstant.SP_GIS_LOCATION_START, false);
					return;
				}
			} else if (startStopButton.getText().equals(
					instance.getString(R.string.setting_gis_stop))) {
				startStopButton.setText(instance
						.getString(R.string.setting_gis_start));
				((MyApplication) getApplication()).mLocationClient.stop();
				editor.putBoolean(PTTConstant.SP_GIS_LOCATION_START, false);
			}
		}
		editor.commit();

		if (v == btnCallRing) {
			chooseCallRing();
		}

		// if (v == groupChoseSpinner) {
		// if (groupChoseSpinner.isEnabled()) {
		// // do nothing
		// } else {
		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setTitle(getString(R.string.alert_title));
		// builder.setMessage(getString(R.string.alert_msg_ptt_not_closed));
		// builder.setNeutralButton(R.string.alert_btn_ok, new
		// DialogInterface.OnClickListener() {
		//
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// dialog.dismiss();
		// }
		// });
		// builder.create().show();
		// }
		// }
	}

	private void setLocationOption() {
		Editor editor = prefs.edit();
		try {
			LocationClientOption option = new LocationClientOption();
			option.setLocationMode(mLocationMode);
			option.setCoorType("bd09ll");
			option.setScanSpan(gisRequestLocationTime * 1000);
			option.setNeedDeviceDirect(false);
			option.setIsNeedAddress(false);
			mLocClient.setLocOption(option);
			mLocationInit = true;
			editor.putBoolean(PTTConstant.SP_GIS_LOCATION_START, mLocationInit);
		} catch (Exception e) {
			e.printStackTrace();
			mLocationInit = false;
			editor.putBoolean(PTTConstant.SP_GIS_LOCATION_START, mLocationInit);
		}
		editor.commit();
	}

	private void getLocationParams() {

		// 定位精度
		int locationMode = prefs.getInt(PTTConstant.SP_GIS_LOCATION_MODE,
				LOCATION_MODEL_MOBIL);
		switch (locationMode) {
		case LOCATION_MODEL_MOBIL:
			mLocationMode = LocationMode.Battery_Saving;
			break;
		case LOCATION_MODEL_GPS:
			mLocationMode = LocationMode.Device_Sensors;
			break;

		default:
			break;
		}
	}

	private class RingDialogListener implements
			DialogInterface.OnClickListener, OnDismissListener, OnShowListener {

		private String[] items;

		private int which;

		private int currentWhich = -1;

		public RingDialogListener(String[] items) {
			this.items = items;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {

			this.which = which;

			if (player == null) {
				player = new MediaPlayer();
			}

			pttUtil.printLog(bDebug, LOG_TAG,
					"onClick :, player " + player.isPlaying());
			if (player.isPlaying()) {
				if (currentWhich == this.which) {
					// do nothing
					return;
				} else {
					player.stop();
					player.release();
				}
			}

			pttUtil.printLog(bDebug, LOG_TAG, "onClick :, which " + which);

			AssetFileDescriptor fd = PTTUtil.getInstance().getFirstRingFD(
					getResources(), items[which]);
			if (fd != null) {
				try {
					player = new MediaPlayer();
					player.setDataSource(fd.getFileDescriptor(),
							fd.getStartOffset(), fd.getLength());
					player.prepare();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				int volume = audioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC);
				if (volume <= 0)
					volume = audioManager
							.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				pttUtil.printLog(bDebug, LOG_TAG, "onClick :, volume " + volume);
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume,
						0);
				audioManager.setSpeakerphoneOn(true);
				audioManager.setMode(AudioManager.MODE_NORMAL);
				pttUtil.printLog(bDebug, LOG_TAG, "ringFile : " + items[which]
						+ " volumn : " + volume);
				player.setLooping(false);
				currentWhich = which;
				player.start();
			}
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>onDismiss");
			if (player != null && player.isPlaying()) {
				player.stop();
				player.release();
			}
			player = null;
			audioManager.setSpeakerphoneOn(false);
			audioManager.setMode(AudioManager.MODE_NORMAL);
			currentWhich = -1;
		}

		public int getWhich() {
			return which;
		}

		@Override
		public void onShow(DialogInterface dialog) {
			pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>onShow");

		}
	}

	private void chooseCallRing() {
		List<String> lstRings = PTTUtil.getInstance().listAllRings(
				getResources(), "call_");
		int currentRingIndex = 0;
		String currentRingName = prefs.getString(
				getString(R.string.sp_current_ringtone), "");

		if (lstRings == null || lstRings.size() == 0) {
			alertMsg(getString(R.string.alert_msg_norings));
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.alert_title));

			int size = lstRings.size();
			final String[] items = new String[size];
			for (int i = 0; i < size; i++) {
				items[i] = lstRings.get(i);
				if (items[i].equals(currentRingName)) {
					currentRingIndex = i;
				}
			}

			final RingDialogListener listener = new RingDialogListener(items);

			builder.setSingleChoiceItems(items, currentRingIndex, listener);
			builder.setNeutralButton(R.string.alert_btn_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							tvCurrentRingtone.setText(items[listener.getWhich()]);
							Editor editor = prefs.edit();
							editor.putString(
									getString(R.string.sp_current_ringtone),
									items[listener.getWhich()]);
							editor.commit();
							dialog.dismiss();
						}
					});
			Dialog d = builder.create();
			d.setOnDismissListener(listener);
			d.setOnShowListener(listener);
			d.show();
		}
	}

	private void alertMsg(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_title));
		builder.setMessage(msg);
		builder.setNeutralButton(R.string.alert_btn_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		Editor editor = prefs.edit();
		String key = "";
		if (buttonView == cbRing) {
			key = getString(R.string.sp_ring_enable);
			btnCallRing.setEnabled(isChecked);
		} else if (buttonView == cbVirabate) {
			key = getString(R.string.sp_virabate_enable);
		} else if (buttonView == cbAutoStart) {
			key = getString(R.string.sp_auto_start);
		} else if (buttonView == cbAutoRegister) {
			key = PTTConstant.SP_AUTOREGISTER;
		} else if (buttonView == openGisBox) {
			key = PTTConstant.SP_OPEN_GIS;
			if (isChecked) {
				boolean isMobileChecked = prefs.getBoolean(
						PTTConstant.SP_GIS_MOBILE, false);
				boolean isGpsChecked = prefs.getBoolean(PTTConstant.SP_GIS_GPS,
						false);
				editor.putBoolean(PTTConstant.SP_GIS_MOBILE, true);
				isMobileChecked = true;
				editor.putInt(PTTConstant.SP_GIS_LOCATION_MODE,
						LOCATION_MODEL_MOBIL);
				gisChoseLayout.setVisibility(View.VISIBLE);
				gisTimeLayout.setVisibility(View.VISIBLE);
				gisStartLayout.setVisibility(View.VISIBLE);
				mobileBox.setChecked(isMobileChecked);
				gpsBox.setChecked(isGpsChecked);
			} else {
				gisChoseLayout.setVisibility(View.GONE);
				gisTimeLayout.setVisibility(View.GONE);
				gisStartLayout.setVisibility(View.GONE);
				editor.putBoolean(PTTConstant.SP_GIS_MOBILE, true);
				editor.putBoolean(PTTConstant.SP_GIS_GPS, false);
				editor.putInt(PTTConstant.SP_GIS_LOCATION_MODE,
						LOCATION_MODEL_MOBIL);
				((MyApplication) getApplication()).mLocationClient.stop();
				startStopButton.setText(instance
						.getString(R.string.setting_gis_start));
				editor.putBoolean(PTTConstant.SP_GIS_LOCATION_START, false);
			}
		}
		editor.putBoolean(key, isChecked);
		editor.commit();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		pttUtil.printLog(bDebug, LOG_TAG, "onProgressChanged");
		seekBar.setProgress(progress);
		Editor editor = prefs.edit();
		if (seekBar == pttVolumeBar)
			editor.putInt(getString(R.string.sp_call_ptt_volume), progress);
		else if (seekBar == callVolumeBar) {
			editor.putInt(getString(R.string.sp_call_single_volume), progress);
		} else {
			editor.putInt(getString(R.string.sp_call_ring_volume), progress);
		}
		editor.commit();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		pttUtil.printLog(bDebug, LOG_TAG, "onStartTrackingTouch");
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		pttUtil.printLog(bDebug, LOG_TAG, "onStartTrackingTouch");
	}

	@Override
	protected void onResume() {
		super.onResume();
		instance = this;

		if (currentItemId == PTTConstant.SETTING_ITEM_TALKING) {
			if (PTTManager.getInstance().getCurrentPttState().getStatus() != PTTConstant.PTT_IDLE) {
				groupChoseSpinner.setEnabled(false);
				Toast.makeText(
						getApplicationContext(),
						getApplicationContext().getString(
								R.string.setting_current_group_off1)
								+ "\n"
								+ getApplicationContext().getString(
										R.string.setting_current_group_off2),
						Toast.LENGTH_LONG).show();
			} else {
				groupChoseSpinner.setEnabled(true);
			}
		} else if (currentItemId == PTTConstant.SETTING_ITEM_ALERTRING) {
			int pttVolume = prefs.getInt(
					getString(R.string.sp_call_ptt_volume), 0);
			int ringVolume = prefs.getInt(
					getString(R.string.sp_call_ring_volume), 0);
			int callVolume = prefs.getInt(
					getString(R.string.sp_call_single_volume), 0);
			if (pttVolume != pttVolumeBar.getProgress())
				pttVolumeBar.setProgress(pttVolume);
			if (ringVolume != ringVolumeBar.getProgress())
				ringVolumeBar.setProgress(ringVolume);
			if (callVolume != callVolumeBar.getProgress())
				callVolumeBar.setProgress(callVolume);
		} else if (currentItemId == PTTConstant.SETTING_ITEM_GIS) {
				boolean isLocationInit = prefs.getBoolean(
						PTTConstant.SP_GIS_LOCATION_START, false);
				if (isLocationInit) {
					startStopButton.setText(instance
							.getString(R.string.setting_gis_stop));
				} else {
					startStopButton.setText(instance
							.getString(R.string.setting_gis_start));
				}
		}
	}

	private boolean isGpsOpen() {
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

}

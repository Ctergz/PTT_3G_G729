/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:MainPageActivity.java
 * Description:MainPageActivity
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-5
 */

package com.zzy.ptt.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.MemberInfo;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.CallStateManager;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.service.StateManager.EnumRegByWho;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

public class MainPageActivity extends Activity implements View.OnClickListener {

	private ProgressDialog initProgressDialog;
	private ProgressDialog regProgressDialog;
	private ProgressDialog deInitProgressDialog;
	private AlertDialog alertDialog;
	private MainPageReceiver registerReceiver;
	// private SharedPreferences prefs;

	private static final String LOG_TAG = "MainPageActivity";
	private static final String MSG_KEY = "handler_msg";

	private static final int MSG_INIT_SUCCESS = 0;
	private static final int MSG_INIT_FAIL = 1;
	private static final int MSG_DEINIT_START = 2;
	private static final int MSG_DEINIT_OVER = 3;

	// private PTTManager pttManager = PTTManager.getInstance();

	private TextView statusTV, statusTV1;
	private ImageView registerImage, setImage, aboutImage, exitImage;
	private ImageView groupLinearLayout, daiLinearLayout, calllogLinearLayout,
			contactLinearLayout, messageLinearLayout, setLinearLayout;

	public static MainPageActivity instance = null;

	private SharedPreferences sp;
	private GroupReceiver groupReceiver;

	private Map<String, MemberInfo[]> groupInfoMap;
	private Map<String, Integer> groupStatusMap;
	private Map<String, String> groupNameMap;
	private List<GroupInfo> lstGroupData;
	private String[] groups;
	private String[] groupString;
	private MemberInfo[][] memberInfos;
	private String[][] memberInfoString;

	public Handler mainPageHandler = new Handler() {

		public void handleMessage(Message msg) {

			Bundle bundle = null;
			int errorCode = -1;
			AlertDialogManager.getInstance().dismissProgressDialog(
					initProgressDialog);
			switch (msg.what) {
			case MSG_DEINIT_START:
				stopService(new Intent(MainPageActivity.this, PTTService.class));
				break;
			case MSG_DEINIT_OVER:
				if (deInitProgressDialog != null
						&& deInitProgressDialog.isShowing()) {
					deInitProgressDialog.dismiss();
				}
				StateManager.reset();
				MainPageActivity.this.finish();
				break;
			case MSG_INIT_SUCCESS:
				// need a service here to update the status
				doRegister();
				break;
			case MSG_INIT_FAIL:
				bundle = msg.getData();
				errorCode = bundle.getInt(MSG_KEY);
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainPageActivity.this);
				builder.setTitle(MainPageActivity.this
						.getString(R.string.alert_title_init));
				builder.setMessage(MainPageActivity.this
						.getString(R.string.alert_msg_init_fail)
						+ " : "
						+ errorCode);
				builder.setNeutralButton(R.string.alert_btn_ok,
						new OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
								// MainPageActivity.this.finish();
							}
						});

				AlertDialog alert = builder.create();
				alert.setCancelable(false);
				alert.setOnDismissListener(new OnDismissListener() {

					public void onDismiss(DialogInterface dialog) {
						MainPageActivity.this.finish();
					}
				});
				alert.show();
				break;
			default:
				break;
			}
		}
	};

	public class MainPageReceiver extends BroadcastReceiver {
		private Context context;

		// construct
		public MainPageReceiver(Context c) {
			context = c;
		}

		public void registerAction(String action) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(action);

			context.registerReceiver(this, filter);
		}

		public void onReceive(Context context, Intent intent) {
			int state = -1;
			String action = intent.getAction();

			Log.d(LOG_TAG, "<<<<<<<<<<<Receive action : " + action);
			if (PTTConstant.ACTION_REGISTER.equals(action)) {

				if (regProgressDialog != null && regProgressDialog.isShowing()) {
					regProgressDialog.dismiss();
				}
				// register state
				state = intent.getIntExtra(PTTConstant.KEY_REGISTER_STATE, 0);
				Log.d(LOG_TAG, "<<<<<<<<<<<Receive register state : " + state);
				// show title info
				if (!StateManager.exitFlag)
					showTitle(state);

				if (PTTConstant.DUMMY) {
					GroupInfo[] groupInfos = new GroupInfo[1];
					groupInfos[0] = new GroupInfo("dummy", "9000");
					PTTService.instance.JNIAddGroupInfo(groupInfos);
				}

			} else if (PTTConstant.ACTION_INIT.equals(action)) {
				// init state
				int initState = intent.getIntExtra(PTTConstant.KEY_INIT_STATE,
						0);
				Log.d(LOG_TAG, "<<<<<<<<<<<Receive init state : " + initState);
				if (initState == PTTConstant.INIT_SUCCESS) {
					mainPageHandler.sendEmptyMessage(MSG_INIT_SUCCESS);
				} else {
					int errorCode = intent.getIntExtra(
							PTTConstant.KEY_RETURN_VALUE, 0);
					Message msg = new Message();
					msg.what = MSG_INIT_FAIL;
					Bundle tempBundle = new Bundle();
					tempBundle.putInt(MSG_KEY, errorCode);
					mainPageHandler.sendMessage(msg);
				}
			} else if (PTTConstant.ACTION_NUMBER_KEY2.equals(action)) {
				// init state
				int keyCode = intent.getIntExtra(PTTConstant.KEY_NUMBER, 0);
				Log.d(LOG_TAG, "()()()()()()()(keyCode : " + keyCode);
				if (keyCode >= KeyEvent.KEYCODE_0
						&& keyCode <= KeyEvent.KEYCODE_9) {
					Intent intent2 = new Intent(MainPageActivity.this,
							DialActivity.class);
					intent2.putExtra("OK", 123);
					intent2.putExtra("keyCode", (keyCode - 7) + "");
					startActivity(intent2);
				}
			} else if (PTTConstant.ACTION_DEINIT.equals(action)) {
				// deinit state
				mainPageHandler.sendEmptyMessage(MSG_DEINIT_OVER);
				Log.d(LOG_TAG, "action : " + action + " ACTION_DEINIT!!!");
			} else {
				Log.d(LOG_TAG, "action : " + action + " unknown!!!");
			}
		}
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mainpage);

		groupInfoMap = new HashMap<String, MemberInfo[]>();
		groupStatusMap = new HashMap<String, Integer>();
		groupNameMap = new HashMap<String, String>();
		lstGroupData = new ArrayList<GroupInfo>();
		
		sp = PreferenceManager.getDefaultSharedPreferences(this);

		initButtons();
		statusTV = (TextView) findViewById(R.id.maintextView1);
		statusTV1 = (TextView) findViewById(R.id.maintextView2);
		// show menu items
		PTTUtil.getInstance().initOnCreat(this);
		// prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// show init dialog
		if (StateManager.getInitState() != PTTConstant.INIT_SUCCESS) {
			initProgressDialog = AlertDialogManager.getInstance()
					.showProgressDialog(this,
							getString(R.string.alert_title_init),
							getString(R.string.alert_msg_initing));
		}
		Intent intent = new Intent(MainPageActivity.this, PTTService.class);
		// intent.setAction(PTTConstant.ACTION_INIT);
		startService(intent);

		instance = this;

		Log.d(LOG_TAG, "MainPageActivity onCreate Over");

		groupReceiver = new GroupReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PTTConstant.ACTION_MEMBERS_INFO);
		filter.addAction(PTTConstant.ACTION_DYNAMIC_REGRP);
		this.registerReceiver(groupReceiver, filter);

		if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS) {
			Intent intentTOHO = new Intent("com.zzy.action.start");
			sendBroadcast(intentTOHO);
		}

	}

	private class GroupReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (PTTConstant.ACTION_MEMBERS_INFO.equals(action)) {
				freshView();
			}
			if (PTTConstant.ACTION_DYNAMIC_REGRP.equals(action)) {
				updateReisterInfo();
			}
		}
	}

	public void freshView() {

		try {
			groupInfoMap = GroupManager.getInstance().getGroupNumMebersMap();
			lstGroupData = GroupManager.getInstance().getGroupData();
			if (lstGroupData.size() > 0 && groupInfoMap.size() > 0) {

				groups = new String[lstGroupData.size()];
				groupString = new String[lstGroupData.size()];
				memberInfos = new MemberInfo[groupInfoMap.size()][];
				memberInfoString = new String[groupInfoMap.size()][];

				for (int i = 0; i < lstGroupData.size(); i++) {
					groupString[i] = lstGroupData.get(i).getNumber() + "--("
							+ lstGroupData.get(i).getName() + ")";
					groups[i] = lstGroupData.get(i).getNumber();
					memberInfos[i] = groupInfoMap.get(groups[i]);
					Arrays.sort(memberInfos[i]);
				}

				for (int i = 0; i < groupInfoMap.size(); i++) {
					memberInfoString[i] = new String[memberInfos[i].length];
					for (int j = 0; j < memberInfos[i].length; j++) {
						String mebNum = memberInfos[i][j].getNumber();
						String mebName = memberInfos[i][j].getName();
						int mebStatus = memberInfos[i][j].getStatus();
						groupStatusMap.put(mebNum, mebStatus);
						groupNameMap.put(mebNum, mebName);
						memberInfoString[i][j] = mebNum;
					}
				}
			}
			String mebNum = PTTUtil.getInstance().getCurrentUserName(this);
			if (!TextUtils.isEmpty(mebNum)) {
				String mebName = groupNameMap.get(mebNum);
				statusTV1.setText(getApplicationContext().getString(
						R.string.current_num)
						+ mebName);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void initButtons() {
		registerImage = (ImageView) findViewById(R.id.imageRegister);
		setImage = (ImageView) findViewById(R.id.imageSet);
		aboutImage = (ImageView) findViewById(R.id.imageAbout);
		exitImage = (ImageView) findViewById(R.id.imageExit);

		groupLinearLayout = (ImageView) findViewById(R.id.image_group);
		daiLinearLayout = (ImageView) findViewById(R.id.image_dail);
		calllogLinearLayout = (ImageView) findViewById(R.id.image_calllog);
		contactLinearLayout = (ImageView) findViewById(R.id.image_contact);
		messageLinearLayout = (ImageView) findViewById(R.id.image_message);
		setLinearLayout = (ImageView) findViewById(R.id.image_ptt);

		groupLinearLayout.setOnClickListener(this);
		daiLinearLayout.setOnClickListener(this);
		calllogLinearLayout.setOnClickListener(this);
		contactLinearLayout.setOnClickListener(this);
		messageLinearLayout.setOnClickListener(this);
		setLinearLayout.setOnClickListener(this);

		registerImage.setOnClickListener(this);
		setImage.setOnClickListener(this);
		aboutImage.setOnClickListener(this);
		exitImage.setOnClickListener(this);
	}

	private void showTitle(int state) {
		String titleInfo = "";
		String curGrp = sp.getString(PTTConstant.SP_CURR_GRP_NUM, "");
		List<GroupInfo> grpData = GroupManager.getInstance().getGroupData();
		if (curGrp == null || curGrp.trim().length() == 0) {
			if (grpData == null || grpData.size() == 0) {
				curGrp = "";
			} else {
				curGrp = grpData.get(0).getNumber();
			}
		} else {
			if (grpData == null || grpData.size() == 0) {
				PTTUtil.getInstance().setCurrentGrp("");
				curGrp = "";
			}
		}
		if (state == PTTConstant.REG_ED) {
			titleInfo = PTTUtil.getInstance().getCurrentUserName(this);
			if (TextUtils.isEmpty(curGrp)) {
				statusTV.setText(getApplicationContext().getString(
						R.string.current_group)
						+ curGrp);
			} else {
				String groupName = GroupManager.getInstance()
						.getGroupInfo(curGrp).getName();
				statusTV.setText(getApplicationContext().getString(
						R.string.current_group)
						+ groupName);
			}
			statusTV1.setText(getApplicationContext().getString(
					R.string.current_num)
					+ titleInfo);
			try {
				SipProxy.getInstance().RequestGetMember(curGrp);
			} catch (PTTException e) {
				e.printStackTrace();
			}
		} else {
			titleInfo = getString(PTTUtil.getInstance().getTitleId(state));
			statusTV.setText("");
			statusTV1.setText(titleInfo);
		}
	}

	private void showTitle(EnumLoginState currentState) {
		String titleInfo = "";
		String curGrp = sp.getString(PTTConstant.SP_CURR_GRP_NUM, "");
		List<GroupInfo> grpData = GroupManager.getInstance().getGroupData();
		if (curGrp == null || curGrp.trim().length() == 0) {
			if (grpData == null || grpData.size() == 0) {
				curGrp = "";
			} else {
				curGrp = grpData.get(0).getNumber();
			}
		} else {
			if (grpData == null || grpData.size() == 0) {
				PTTUtil.getInstance().setCurrentGrp("");
				curGrp = "";
			}
		}
		if (currentState == EnumLoginState.REGISTERE_SUCCESS) {
			titleInfo = PTTUtil.getInstance().getCurrentUserName(this);
			if (TextUtils.isEmpty(curGrp)) {
				statusTV.setText(getApplicationContext().getString(
						R.string.current_group)
						+ curGrp);
			} else {
				String groupName = GroupManager.getInstance()
						.getGroupInfo(curGrp).getName();
				statusTV.setText(getApplicationContext().getString(
						R.string.current_group)
						+ groupName);
			}
			statusTV1.setText(getApplicationContext().getString(
					R.string.current_num)
					+ titleInfo);
			try {
				SipProxy.getInstance().RequestGetMember(curGrp);
			} catch (PTTException e) {
				e.printStackTrace();
			}
		} else {
			titleInfo = getString(PTTUtil.getInstance()
					.getTitleId(currentState));
			statusTV.setText("");
			statusTV1.setText(titleInfo);
		}
	}

	private void registerReceiverAction() {
		registerReceiver = new MainPageReceiver(this);
		registerReceiver.registerAction(PTTConstant.ACTION_INIT);
		registerReceiver.registerAction(PTTConstant.ACTION_REGISTER);
		registerReceiver.registerAction(PTTConstant.ACTION_NUMBER_KEY2);
		registerReceiver.registerAction(PTTConstant.ACTION_DEINIT);

	}

	private void doRegister() {
		// start to register and show progress dialog
		Log.d(LOG_TAG,
				"doRegister regstate : "
						+ PTTUtil.getInstance().getRegStateString(
								StateManager.getCurrentRegState()));
		if (StateManager.getCurrentRegState() != EnumLoginState.REGISTERE_SUCCESS) {
			regProgressDialog = AlertDialogManager.getInstance()
					.showProgressDialog(this,
							getString(R.string.alert_title_register),
							getString(R.string.alert_msg_registering));
			regProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface dialog) {
					if (StateManager.getCurrentRegState() != EnumLoginState.REGISTERE_SUCCESS) {
						StateManager
								.setCurrentRegState(EnumLoginState.UNREGISTERED);
						showTitle(EnumLoginState.UNREGISTERED);
					}
				}
			});
			StateManager.setRegStarter(EnumRegByWho.REG_BY_USER);
			Intent intent2 = new Intent(PTTConstant.ACTION_REGISTER);
			intent2.setClass(this, PTTService.class);
			intent2.putExtra(PTTConstant.KEY_REREGISTER_FORCE, true);
			startService(intent2);

			showTitle(EnumLoginState.REGISTERING);
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		instance = null;
		unregisterReceiver(groupReceiver);
		if (alertDialog != null) {
			alertDialog.dismiss();
		}
		if (deInitProgressDialog != null && deInitProgressDialog.isShowing()) {
			deInitProgressDialog.dismiss();
		}
		if (AlertActivity.instance != null) {
			AlertActivity.instance.finish();
		}
		StateManager.exitFlag = false;
		Log.d(LOG_TAG, "MainPageActivity.onDestory()");
	}

	private void unregisterReceiverAction() {
		if (registerReceiver != null)
			this.unregisterReceiver(registerReceiver);
	}

	protected void onResume() {
		super.onResume();

		instance = this;

		registerReceiverAction();
		Log.d(LOG_TAG, "onResume");

		// update register info
		updateReisterInfo();

		if (StateManager.getInitState() == PTTConstant.INIT_SUCCESS
				&& initProgressDialog != null && initProgressDialog.isShowing()) {
			initProgressDialog.dismiss();
			if (StateManager.getCurrentRegState() == EnumLoginState.UNREGISTERED) {
				doRegister();
			}
		}

		if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS
				&& regProgressDialog != null && regProgressDialog.isShowing()) {
			regProgressDialog.dismiss();
		}

		int iCallState = CallStateManager.getInstance().getCallState();
		if (iCallState >= PTTConstant.CALL_DIALING
				&& iCallState <= PTTConstant.CALL_TALKING) {
			Intent intent = new Intent(this, InCallScreenActivity.class);
			startActivity(intent);
		}

		if (StateManager.getCurrentRegState() == EnumLoginState.ERROR_CODE_NUMBER
				|| StateManager.getCurrentRegState() == EnumLoginState.UNREGISTERED) {
			registerImage.setVisibility(View.VISIBLE);
		} else {
			registerImage.setVisibility(View.GONE);
		}

	}

	protected void onPause() {
		super.onPause();
		// instance = null;
		unregisterReceiverAction();
		Log.d(LOG_TAG, "onPause instance " + instance);
		if (regProgressDialog != null && regProgressDialog.isShowing()) {
			regProgressDialog.cancel();
		}
	}

	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onStop");
		super.onStop();
		instance = null;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		// case KeyEvent.KEYCODE_BACK:
		// askIfExit();
		// return true;
		case PTTConstant.KEYCODE_W_CALL:
		case KeyEvent.KEYCODE_CALL:
			// go to call log
			startActivity(new Intent(this, CallLogTabActivity.class));
			return true;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void updateReisterInfo() {
		showTitle(StateManager.getCurrentRegState());
	}

	private void destorySipStack() {
		Log.d(LOG_TAG, "<<<<<<<<<<<deInitProgressDialog to show: ");
		deInitProgressDialog = ProgressDialog.show(MainPageActivity.this,
				getString(R.string.alert_title_exit),
				getString(R.string.alert_msg_exiting));
		deInitProgressDialog.setCancelable(false);
		Log.d(LOG_TAG, "<<<<<<<<<<<deInitProgressDialog to show: ");
		mainPageHandler.sendEmptyMessage(MSG_DEINIT_START);
	}

	private void askIfExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_title));
		builder.setMessage(getString(R.string.alert_title_ifexit));
		builder.setPositiveButton(getString(R.string.menu_minialmize),
				new OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						MainPageActivity.this.finish();
						dialog.dismiss();
					}
				});
		builder.setNegativeButton(getString(R.string.menu_exit),
				new OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						StateManager.exitFlag = true;
						// MainPageActivity.this.finish();
						dialog.dismiss();
						// -----add by wangjunhui
						StateManager.stopNotification();
						destorySipStack();
					}
				});
		builder.create().show();

	}

	public void onClick(View v) {
		@SuppressWarnings("rawtypes")
		Class clazz = null;
		switch (v.getId()) {
		case R.id.image_group:
			clazz = GroupActivity.class;
			break;
		case R.id.image_dail:
			clazz = DialActivity.class;
			break;
		case R.id.image_calllog:
			clazz = CallLogTabActivity.class;
			break;
		case R.id.image_contact:
			clazz = PBKActivity.class;
			break;
		case R.id.image_message:
			clazz = MessageActivity.class;
			break;
		case R.id.image_ptt:
			clazz = PTTActivity.class;
			break;
		case R.id.imageSet:
			clazz = SettingActivity.class;
			break;
		case R.id.imageAbout:
			clazz = AboutActivity.class;
			break;
		case R.id.imageExit:
			askIfExit();
			break;
		case R.id.imageRegister:
			doRegister();
			break;

		default:
			break;
		}
		if (v.getId() != R.id.imageExit && v.getId() != R.id.imageRegister) {
			Log.d(LOG_TAG, "MainPageActivity -------> " + clazz.getName());
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), clazz);
			if (v.getId() == R.id.image_ptt) {
				intent.setAction(PTTConstant.ACTION_PTT);
			}
			startActivity(intent);
		}
	}

}

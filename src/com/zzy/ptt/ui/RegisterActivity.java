package com.zzy.ptt.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.SipServer;
import com.zzy.ptt.model.SipUser;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.service.StateManager.EnumRegByWho;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

public class RegisterActivity extends BaseActivity {
	private EditText etServerIP, etPort, etUsername, etPassword;
	private Button btnrergi;

	private PTTUtil pttUtil = PTTUtil.getInstance();
	private SharedPreferences prefs;
	private ProgressDialog progressDialog;
	private BroadcastReceiver registerReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_setting_layout);

		etServerIP = (EditText) findViewById(R.id.et_serverip1);
		etUsername = (EditText) findViewById(R.id.et_username);
		etPassword = (EditText) findViewById(R.id.et_password);
		etPort = (EditText) findViewById(R.id.et_port);

		btnrergi = (Button) findViewById(R.id.btn_regi);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		etServerIP.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
				15) });
		etUsername.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
				10) });
		etPassword.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
				10) });
		etPort.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });

		SipServer sipServer = PTTUtil.getInstance()
				.getSipServerFromPrefs(prefs);
		SipUser sipUser = PTTUtil.getInstance().getSipUserFromPrefs(prefs);

		etServerIP.setText(sipServer.getServerIp());
		etPort.setText(sipServer.getPort());
		etUsername.setText(sipUser.getUsername());
		etPassword.setText(sipUser.getPasswd());

		btnrergi.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (doSave()) {
					doReregister();
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter(PTTConstant.ACTION_REGISTER);
		addRegReceiver();
		registerReceiver(registerReceiver, filter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(registerReceiver);
	}

	private void addRegReceiver() {
		registerReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (PTTConstant.ACTION_REGISTER.equals(action)) {
					// register state
					if (progressDialog != null && progressDialog.isShowing()) {
						AlertDialogManager.getInstance().dismissProgressDialog(
								progressDialog);
					}
					if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS) {
						finish();
					}
				}
			}
		};
	}

	private boolean doSave() {
		String ip = etServerIP.getText().toString();
		// check IP addr if correct
		if (!pttUtil.checkIP(ip)) {
			Toast.makeText(getApplicationContext(), "wrong ip addr",
					Toast.LENGTH_SHORT).show();
			return false;
		}

		Editor editor = prefs.edit();

		editor.putString(PTTConstant.SP_SERVERIP, ip);
		editor.putString(PTTConstant.SP_PORT, etPort.getText().toString());
		editor.putString(PTTConstant.SP_USERNAME, etUsername.getText()
				.toString());
		editor.putString(PTTConstant.SP_PASSWORD, etPassword.getText()
				.toString());

		editor.commit();

		return true;
	}

	private void askIfReregister() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_title_register));
		builder.setMessage(getString(R.string.alert_msg_ifreregister));
		builder.setPositiveButton(getString(R.string.alert_btn_yes),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// new thread to register, show progress dialog, update
						// notification and currentState
						dialog.dismiss();
						if (doSave()) {
							doReregister();
						}
					}
				});
		builder.setNegativeButton(getString(R.string.alert_btn_no),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						RegisterActivity.this.finish();
					}
				});
		builder.create().show();
	}

	private void doReregister() {
		progressDialog = AlertDialogManager.getInstance().showProgressDialog(
				RegisterActivity.this,
				getString(R.string.alert_title_register),
				getString(R.string.alert_msg_registering));
		progressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (StateManager.getCurrentRegState() != EnumLoginState.REGISTERE_SUCCESS) {
					StateManager
							.setCurrentRegState(EnumLoginState.UNREGISTERED);
				}
			}
		});
		StateManager.setRegStarter(EnumRegByWho.REG_BY_USER);
		Intent intent = new Intent(PTTConstant.ACTION_REGISTER);
		intent.putExtra(PTTConstant.KEY_REREGISTER_FORCE, true);
		intent.setClass(this, PTTService.class);
		startService(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		super.onKeyDown(keyCode, event);

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (checkIfRegisterDirty()) {
				askIfReregister();
			} else {
				this.finish();
			}
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private boolean checkIfRegisterDirty() {
		String oldServerip = prefs.getString(PTTConstant.SP_SERVERIP, "");
		if (!oldServerip.equals(etServerIP.getText().toString())) {
			return true;
		}

		String oldServerport = prefs.getString(PTTConstant.SP_PORT, "");
		if (!oldServerport.equals(etPort.getText().toString())) {
			return true;
		}

		String oldUsername = prefs.getString(PTTConstant.SP_USERNAME, "");
		if (!oldUsername.equals(etUsername.getText().toString())) {
			return true;
		}

		String oldPassword = prefs.getString(PTTConstant.SP_PASSWORD, "");
		if (!oldPassword.equals(etPassword.getText().toString())) {
			return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_set_register, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (checkIfRegisterDirty()) {
			askIfReregister();
		} else {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

}

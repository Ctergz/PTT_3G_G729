/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:SettingActivity.java
 * Description:SettingPage, store information to file
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-5
 */

package com.zzy.ptt.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.model.VersionInfo;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class SettingActivity extends BaseActivity implements OnClickListener {
	private LayoutInflater inflater;
	
	private RelativeLayout registerSetLayout,ringtongSetLayout,talkingSetLayout,systemSetLayout;
	
	public static SettingActivity instance = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		
		instance = this;
		
		registerSetLayout = (RelativeLayout) findViewById(R.id.relativelayout_setting_register);
		ringtongSetLayout = (RelativeLayout) findViewById(R.id.relativelayout_setting_alertring);
		talkingSetLayout = (RelativeLayout) findViewById(R.id.relativelayout_setting_talking);
		systemSetLayout = (RelativeLayout) findViewById(R.id.relativelayout_setting_system);
		
		registerSetLayout.setOnClickListener(this);
		ringtongSetLayout.setOnClickListener(this);
		talkingSetLayout.setOnClickListener(this);
		systemSetLayout.setOnClickListener(this);
		
		inflater = LayoutInflater.from(this);
		PTTUtil.getInstance().initOnCreat(this);

	}

	List<VersionInfo> parse() {
		InputStream in = null;
		try {
			in = getAssets().open("versioninfo.xml");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		XmlPullParser parser = Xml.newPullParser();

		List<VersionInfo> versionList = new ArrayList<VersionInfo>();

		VersionInfo currentVersion = null;
		try {
			parser.setInput(in, "utf-8");

			int eventType = parser.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {

				switch (eventType) {

				case XmlPullParser.START_TAG:
					String tagName = parser.getName();
					if (tagName != null && tagName.equals("version")) {
						currentVersion = new VersionInfo();
						int id = Integer.parseInt(parser.getAttributeValue(null, "id"));
						currentVersion.setId(id);
					}

					if (tagName != null && tagName.equals("name")) {
						String name = parser.nextText();
						currentVersion.setVersion(name);
					}
					if (tagName != null && tagName.equals("build_time")) {
						String build_time = parser.nextText();
						currentVersion.setBuild_time(build_time);
					}
					if (tagName != null && tagName.equals("register_pw")) {
						String register_pw = parser.nextText();
						currentVersion.setRegister_pw(register_pw);
					}

					break;

				case XmlPullParser.END_TAG:
					if (parser.getName().equals("version")) {
						versionList.add(currentVersion);
					}
					break;
				default:
					break;
				}
				eventType = parser.next();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return versionList;

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
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

	@Override
	public void onClick(View view) {
		View v = inflater.inflate(R.layout.register_pw, null);
		final EditText pwEditText = (EditText) v.findViewById(R.id.registerpw);
		List<VersionInfo> list = parse();
		VersionInfo currentVersion = list.get(2);
		final String register_pw = currentVersion.getRegister_pw();
		switch (view.getId()) {
		case R.id.relativelayout_setting_register:
			new AlertDialog.Builder(this)
			.setTitle(getApplicationContext().getString(R.string.setting_register_pw))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setView(v)
			.setPositiveButton(getApplicationContext().getString(R.string.alert_btn_ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if (pwEditText.getText().toString().trim().equals(register_pw)) {
								Intent intent = new Intent(SettingActivity.this,RegisterActivity.class);
								startActivity(intent);
								finish();
							} else {
								Toast.makeText(getApplicationContext(),
										getApplicationContext().getString(R.string.setting_register_pw_false),
										Toast.LENGTH_LONG).show();
							}
						}
					}).setNegativeButton(getApplicationContext().getString(R.string.alert_btn_cancel), null)
			.show();
			break;
		case R.id.relativelayout_setting_alertring:
			Intent intentalertring = new Intent(this, SettingDetailActivity.class);
			intentalertring.putExtra(PTTConstant.SETTING_DISPATCH_KEY, 2);
			startActivity(intentalertring);
			break;
		case R.id.relativelayout_setting_talking:
			Intent intenttalking = new Intent(this, SettingDetailActivity.class);
			intenttalking.putExtra(PTTConstant.SETTING_DISPATCH_KEY, 0);
			startActivity(intenttalking);
			break;
		case R.id.relativelayout_setting_system:
			Intent intentsystem = new Intent(this, SettingDetailActivity.class);
			intentsystem.putExtra(PTTConstant.SETTING_DISPATCH_KEY, 3);
			startActivity(intentsystem);
			break;

		default:
			break;
		}
		
	}
}

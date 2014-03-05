/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:PTTKeyReceiver.java
 * DescriptionTTReceiver
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-17
 */

package com.zzy.ptt.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Administrator
 * 
 */
public class PTTCmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = null;
		if (intent != null)
			action = intent.getAction();

		if (action == null || action.length() == 0) {
			return;
		}
		
		handleBootCompletedEvent(action);

	}

	private void handleBootCompletedEvent(String action) {
		// auto start
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
		}
	}
}

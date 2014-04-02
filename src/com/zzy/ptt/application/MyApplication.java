package com.zzy.ptt.application;

import android.R.integer;
import android.app.Application;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.zzy.ptt.jni.CallJni;

/**
 * 主Application
 */
public class MyApplication extends Application {
	public LocationClient mLocationClient;
	private MyLocationListener mMyLocationListener;
	public TextView mLocationResult;

	@Override
	public void onCreate() {
		super.onCreate();
		mLocationClient = new LocationClient(this);
		mMyLocationListener = new MyLocationListener();
		mLocationClient.registerLocationListener(mMyLocationListener);
	}

	/**
	 * 实现实位回调监听
	 */
	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			// Receive Location
			StringBuffer sb = new StringBuffer(256);
			sb.append("time : ");
			sb.append(location.getTime());
			sb.append("\nerror code : ");
			sb.append(location.getLocType());
			sb.append("\nlatitude : ");
			sb.append(location.getLatitude());
			sb.append("\nlontitude : ");
			sb.append(location.getLongitude());
			sb.append("\nradius : ");
			sb.append(location.getRadius());
			if (location.getLocType() == BDLocation.TypeGpsLocation) {
				sb.append("\nspeed : ");
				sb.append(location.getSpeed());
				sb.append("\nsatellite : ");
				sb.append(location.getSatelliteNumber());
				sb.append("\ndirection : ");
				sb.append(location.getDirection());
			} else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
				sb.append("\naddr : ");
				sb.append(location.getAddrStr());
				// 运营商信息
				sb.append("\noperationers : ");
				sb.append(location.getOperators());
			}
			// Log.i("定位信息===>", sb.toString());
			if (location.getLocType() == 61 || location.getLocType() == 161) {
				StringBuffer msg = new StringBuffer(256);
				msg.append("ind:gis");
				msg.append("\r\nlatitude:");
				msg.append(location.getLatitude());
				msg.append("\r\nlontitude:");
				msg.append(location.getLongitude());
				msg.append("\r\ntime:");
				msg.append(location.getTime().replaceAll("\\D", ""));
				msg.append("\r\n");
				Log.i("定位信息===>", msg.toString());
				CallJni.sendCommonMessage(msg.toString());
			} else {
				Toast.makeText(getApplicationContext(), "定位失败,请检查定位设置！",
						Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void onReceivePoi(BDLocation arg0) {

		}
	}
}

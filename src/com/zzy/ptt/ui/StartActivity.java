package com.zzy.ptt.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zzy.ptt.R;
import com.zzy.ptt.service.PTTService;

public class StartActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.activity_start);
		RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.welcome_icon);
		//relativeLayout.setBackgroundResource(R.drawable.hbwelcome);
		relativeLayout.setBackgroundResource(R.drawable.welcome);
		TextView ivlogo2 = (TextView) this.findViewById(R.id.textlogo2);
		AlphaAnimation aa = new AlphaAnimation(0.1f, 1.0f);
		aa.setDuration(1000);
		ivlogo2.startAnimation(aa);
		aa.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				Intent it = new Intent(StartActivity.this,
						MainPageActivity.class);
				StartActivity.this.startActivity(it);
				finish();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}

		});
	}
}

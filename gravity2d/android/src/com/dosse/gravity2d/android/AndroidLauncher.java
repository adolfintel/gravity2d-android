package com.dosse.gravity2d.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.dosse.gravity2d.Utils;
import com.dosse.gravity2d.gdxdemo.GdxDemo;

public class AndroidLauncher extends AndroidApplication {
	private Thread toaster;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AlertDialog.Builder builder = new AlertDialog.Builder(
				AndroidLauncher.this);
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		if (displaymetrics.widthPixels<800 || displaymetrics.heightPixels<480 || ((float)displaymetrics.widthPixels/(float)displaymetrics.heightPixels)>2) { //min resolution is 800x480, and aspect ratio must be <2
			DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_NEGATIVE) {
						System.exit(0);
					}
				}
			};
			builder.setMessage(getString(R.string.lowRes))
					.setPositiveButton(getString(R.string.lowRes_continue), l)
					.setNegativeButton(getString(R.string.lowRes_exit), l)
					.show();
		}
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		final GdxDemo d = new GdxDemo();
		initialize(d, config);
		toaster = new Thread() {
			public void run() {
				Utils.sleep(1000, 0); // for some fucking reason, toast
										// notifications don't appear in the
										// first ~second. maybe libgdx changes
										// context or something.
				while (true) {
					synchronized (d.toastQueue) {
						if (!d.toastQueue.isEmpty()) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									try {
										Toast.makeText(getContext(),
												d.toastQueue.removeFirst(),
												Toast.LENGTH_SHORT).show();
									} catch (Throwable t) {
										System.out.println(t);
									}
								}
							});
						} else {
							if (d.isTerminated())
								return;
						}
					}
					Utils.sleep(100, 0);
				}
			}
		};
		toaster.start();
	}
}

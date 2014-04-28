package com.bingoogol.frogcare.ui;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.bingoogol.frogcare.R;
import com.bingoogol.frogcare.ui.view.BtnCallback;
import com.bingoogol.frogcare.ui.view.ConfirmDialog;
import com.bingoogol.frogcare.ui.view.PDialog;
import com.bingoogol.frogcare.util.ConnectivityUtil;
import com.bingoogol.frogcare.util.Constants;
import com.bingoogol.frogcare.util.Logger;
import com.bingoogol.frogcare.util.SpUtil;
import com.bingoogol.frogcare.util.StorageUtil;
import com.bingoogol.frogcare.util.ToastUtil;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

public class SplashActivity extends BaseActivity {
	private static final String TAG = "SplashActivity";
	// 新版本apk文件路径
	private String mApkUrl;
	// 新版本名称
	private String mVersionName;

	private PDialog mPDialog;

	@Override
	protected void initView() {
		setContentView(R.layout.activity_splash);
	}

	@Override
	protected void setListener() {
	}

	@Override
	protected void afterViews(Bundle savedInstanceState) {
		((TextView) findViewById(R.id.tv_splash_versionName)).setText(mApp.getCurrentVersionName());
		checkVersion();
	}

	private void checkVersion() {
		if (SpUtil.getBoolean(Constants.spkey.AUTO_UPGRADE, true) && ConnectivityUtil.isWifiConnected(mApp) && StorageUtil.isExternalStorageWritable()) {
			new AsyncHttpClient().get(Constants.config.UPGRADE_URL, new JsonHttpResponseHandler("UTF-8") {
				@Override
				public void onSuccess(JSONObject jsonObject) {
					try {
						if (mApp.getCurrentVersionCode() < jsonObject.getInt("versionCode")) {
							mApkUrl = jsonObject.getString("apkUrl");
							mVersionName = jsonObject.getString("versionName");
							showUpgradDialog();
						} else {
							// 没有新版本，不用升级，进入主界面
							loadMainActivity();
						}
					} catch (JSONException e) {
						// 解析升级信息失败，进入主界面
						Logger.e(TAG, "解析升级信息异常");
						loadMainActivity();
					}
				}

				@Override
				public void onFailure(Throwable e, JSONObject errorResponse) {
					Logger.e(TAG, "获取升级信息失败");
					loadMainActivityDelay();
				}
			});
		} else {
			loadMainActivityDelay();
		}
	}

	/**
	 * 延时加载主界面
	 */
	private void loadMainActivityDelay() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				loadMainActivity();
			}
		}, 1500);
	}

	/**
	 * 显示升级对话框
	 * 
	 * @param versionName
	 */
	public void showUpgradDialog() {
		StringBuilder sb = new StringBuilder();
		sb.append(getString(R.string.current_version_tips) + mApp.getCurrentVersionName() + "\n");
		sb.append(getString(R.string.new_version_tips) + mVersionName + "\n\n");
		sb.append(getString(R.string.whether_upgrade_tips));
		final ConfirmDialog confirmDialog = new ConfirmDialog(SplashActivity.this, R.string.find_new_version, sb.toString(), R.string.upgrade_later, R.string.upgrade_now);
		confirmDialog.setBtnCallback(new BtnCallback() {
			@Override
			public void onClickRight() {
				mPDialog = new PDialog(SplashActivity.this);
				mPDialog.show();
				upgrade();
			}

			@Override
			public void onClickLeft() {
				loadMainActivity();
			}
		});
		confirmDialog.show();
	}

	private void upgrade() {
		File apkFile = new File(StorageUtil.getDownloadDir(), Constants.file.NEW_APK_NAME);
		apkFile.deleteOnExit();
		new AsyncHttpClient().get(mApkUrl, new FileAsyncHttpResponseHandler(apkFile) {
			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				mPDialog.setMax(totalSize);
				mPDialog.setProgress(bytesWritten);
			}

			@Override
			public void onSuccess(File file) {
				mPDialog.dismiss();
				install(file);
			}

			@Override
			public void onFailure(Throwable e, File response) {
				mPDialog.dismiss();
				response.deleteOnExit();
				Logger.e(TAG, "下载apk文件出错：" + e.getMessage());
				ToastUtil.makeText(mApp, R.string.download_apk_error);
				loadMainActivity();
			}
		});
	}

	/**
	 * 加载应用程序主界面
	 */
	private void loadMainActivity() {
		startActivity(new Intent(mApp, MainActivity.class));
		finish();
	}

	/**
	 * 安装应用
	 * 
	 * @param apkFile
	 *            apk文件
	 */
	private void install(File apkFile) {
		startActivity(mApp.getInstallApkIntent(apkFile));
		// 销毁当前应用
		finish();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}
}
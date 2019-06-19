package cn.cbdi.savecap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.baidu.ai.edge.core.util.FileUtil;
import com.baidu.aip.manager.FaceSDKManager;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.components.RxActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import cn.cbdi.savecap.Function.Func_Face.mvp.presenter.FacePresenter;
import cn.cbdi.savecap.Tools.ActivityCollector;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class TheSplashActivity extends RxActivity {
    String TAG = SplashActivity.class.getSimpleName();

    private String name = "";
    private String version = "";
    private String ak;
    private String sk;
    private String apiUrl;
    private String soc;
    private int type;
    private ArrayList<String> socList = new ArrayList<>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarUtils.hideStatusBar(this);
        setContentView(R.layout.activity_splash);
        ActivityCollector.addActivity(this);
        try {
            File key = new File(Environment.getExternalStorageDirectory() + File.separator + "key.txt");
            copyToClipboard(AppInit.getContext(), FileIOUtils.readFile2String(key));
        } catch (Exception e) {
            e.printStackTrace();
        }
        initConfig();

        FacePresenter.getInstance().FaceInit(this, new FaceSDKManager.SdkInitListener() {
            @Override
            public void initStart() {
                Log.e(TAG, "sdk init start");
            }

            @Override
            public void initSuccess() {
                Log.e(TAG, "sdk init success");
                Observable.timer(3, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .compose(TheSplashActivity.this.<Long>bindUntilEvent(ActivityEvent.DESTROY))
                        .subscribe(new Observer<Long>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(Long aLong) {
                                if (checkChip()) {
//                                    startUICameraActivity();
                                    ConfigSave();
                                }
                                Intent intent = new Intent(TheSplashActivity.this,AnalysisActivity.class);
                                startActivity(intent);

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }

            @Override
            public void initFail(int errorCode, String msg) {
                ToastUtils.showLong("sdk加载失败");
                Log.e(TAG, "sdk init fail:" + msg);
            }
        });


    }

    public static void copyToClipboard(Context context, String text) {
        ClipboardManager systemService = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        systemService.setPrimaryClip(ClipData.newPlainText("text", text));
    }

    private void initConfig() {
        try {
            String configJson = FileUtil.readAssetFileUtf8String(getAssets(), "demo/config.json");
            JSONObject jsonObject = new JSONObject(configJson);
            name = jsonObject.getString("model_name");
            version = jsonObject.getString("model_version");
            type = jsonObject.getInt("model_type");

            if (jsonObject.has("apiUrl")) {
                apiUrl = jsonObject.getString("apiUrl");
                ak = jsonObject.getString("ak");
                sk = jsonObject.getString("sk");
            }

            JSONArray jsonArray = jsonObject.getJSONArray("soc");
            for (int i = 0; i < jsonArray.length(); i++) {
                String s = jsonArray.getString(i);
                socList.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean checkChip() {
        if (socList.contains("dsp") && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            soc = "dsp";
            return true;
        }
        if (socList.contains("npu") && (Build.HARDWARE.contains("kirin970") || Build.HARDWARE.contains("kirin980"))) {
            soc = "npu";
            return true;
        }
        if (socList.contains("arm")) {
            soc = "arm";
            return true;
        }
        return false;
    }

    SPUtils config = SPUtils.getInstance("ana_config");

    private void ConfigSave() {
        config.put("name", name);
        config.put("model_type", type);
        if (apiUrl != "null") {
            config.put("apiUrl", apiUrl);
            config.put("ak", ak);
            config.put("sk", sk);
        }
    }
    private void startUICameraActivity() {
        Intent intent = new Intent(TheSplashActivity.this, AnalysisActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("model_type", type);

        if (apiUrl != "null") {
            intent.putExtra("apiUrl", apiUrl);
            intent.putExtra("ak", ak);
            intent.putExtra("sk", sk);
        }

        intent.putExtra("soc", soc);
        startActivityForResult(intent, 1);
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }
}

package cn.cbdi.savecap;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Layout;

import com.baidu.ai.edge.core.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class SplashActivity extends Activity {

    private String name = "";
    private String version = "";
    private String ak;
    private String sk;
    private String apiUrl;
    private String soc;
    private int type;
    private ArrayList<String> socList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initConfig();

        Observable.timer(3,TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (checkChip()) {
                            startUICameraActivity();
                        }
                    }
                });

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

    private void startUICameraActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
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


}

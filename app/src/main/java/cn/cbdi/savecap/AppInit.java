package cn.cbdi.savecap;

import android.app.Application;
import android.content.Context;

import com.blankj.utilcode.util.Utils;

public class AppInit extends Application {
    protected static AppInit instance;
    public static AppInit getInstance() {
        return instance;
    }

    public static Context getContext() {
        return getInstance().getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Utils.init(getContext());

    }
}

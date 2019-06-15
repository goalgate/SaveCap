package cn.cbdi.savecap;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceView;

import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.classify.ClassifyException;
import com.baidu.ai.edge.core.classify.ClassifyInterface;
import com.baidu.ai.edge.core.classify.ClassifyOnline;
import com.baidu.ai.edge.core.ddk.DDKConfig;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.detect.DetectInterface;
import com.baidu.ai.edge.core.detect.DetectOnline;
import com.baidu.ai.edge.core.infer.InferConfig;
import com.baidu.ai.edge.core.infer.InferInterface;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.snpe.SnpeConfig;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import com.baidu.ai.edge.core.util.Util;
import com.blankj.utilcode.util.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.cbdi.savecap.Function.Func_Camera.mvp.presenter.PhotoPresenter;
import cn.cbdi.savecap.Function.Func_Camera.mvp.view.IPhotoView;

import static com.baidu.ai.edge.ui.activity.MainActivity.MODEL_CLASSIFY;
import static com.baidu.ai.edge.ui.activity.MainActivity.MODEL_DETECT;

public class MainActivity extends Activity implements IPhotoView {

    private static final String SERIAL_NUM = "A9C2-6117-6712-073F";

    ClassifyInterface mClassifyDLManager;
    ClassifyInterface mOnlineClassify;
    InferInterface mInferManager;
    DetectInterface mOnlineDetect;
    public static final int TYPE_INFER = 0;
    public static final int TYPE_DDK = 1;
    public static final int TYPE_SNPE = 2;

    private static final int CODE_FOR_WRITE_PERMISSION = 0;

    private int platform = TYPE_DDK;

    protected int model;

    private boolean isInitializing = false;

    private boolean hasOnlineApi = false;
    // 模型加载状态
    private boolean modelLoadStatus = false;

    protected boolean canAutoRun = false;

    @BindView(R.id.surfaceView)
    SurfaceView surfaceView;

    @BindView(R.id.TransparentView)
    SurfaceView surfaceViewForDrawing;

    PhotoPresenter pp = PhotoPresenter.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);
        ButterKnife.bind(this);
        model = getIntent().getIntExtra("model_type", MODEL_DETECT);
        pp.init(surfaceView,surfaceViewForDrawing);
        onActivityCreate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pp.PhotoPresenterSetView(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pp.PhotoPresenterSetView(null);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseEasyDL();
    }

    @Override
    public void onPhotoBack(String msg, Bitmap bitmap) {

    }


    public void onActivityCreate() {
        int hasWriteStoragePermission =
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteStoragePermission == PackageManager.PERMISSION_GRANTED) {
            start();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    CODE_FOR_WRITE_PERMISSION);
        }
        choosePlatform();
    }

    private void choosePlatform() {
        String soc = getIntent().getStringExtra("soc");
        switch (soc) {
            case "dsp":
                platform = TYPE_SNPE;
                break;
            case "npu":
                platform = TYPE_DDK;
                break;
            default:
            case "arm":
                platform = TYPE_INFER;
        }
    }

    private void start() {
        // 可能会内存泄漏
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                initManager();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                // 初始化模型结束
                if ((model == MODEL_DETECT && mInferManager != null) ||
                        (model == MODEL_CLASSIFY && mClassifyDLManager != null)) {
                    modelLoadStatus = true;
                    pp.setMoelStatus(modelLoadStatus,model,mClassifyDLManager,mInferManager);
                }
            }
        }.execute();
    }

    private void initManager() {
        String apiUrl = getIntent().getStringExtra("apiUrl");
        String ak = getIntent().getStringExtra("ak");
        String sk = getIntent().getStringExtra("sk");

        if (apiUrl != null) {
            hasOnlineApi = true;
        }

        if (model == MODEL_DETECT) {
            if (hasOnlineApi) {
                mOnlineDetect = new DetectOnline(apiUrl, ak, sk, this);
            }
            try {
                switch (platform) {
                    case TYPE_SNPE:
                        SnpeConfig mSnpeClassifyConfig = new SnpeConfig(this.getAssets(),
                                "snpe-detect/config.json");
                        mInferManager = new SnpeManager(this, mSnpeClassifyConfig, SERIAL_NUM);
                        break;
                    case TYPE_INFER:
                    default:
                        InferConfig mInferConfig = new InferConfig(getAssets(), "infer-detect/config.json");
                        // 可修改ARM推断使用的CPU核数
                        mInferConfig.setThread(Util.getInferCores());
                        mInferManager = new InferManager(this, mInferConfig, SERIAL_NUM);
                        break;
                }
                canAutoRun = true;
                isInitializing = true;
            } catch (BaseException e) {
                Log.e("BaseException",e.toString());
            }
        }
        if (model == MODEL_CLASSIFY) {
            if (hasOnlineApi) {
                mOnlineClassify = new ClassifyOnline(apiUrl, ak, sk, this);
            }
            try {
                switch (platform) {
                    case TYPE_DDK:
                        DDKConfig mClassifyConfig = new DDKConfig(getAssets(),
                                "ddk-classify/config.json");

                        mClassifyDLManager = new DDKManager(this, mClassifyConfig, SERIAL_NUM);
                        break;

                    case TYPE_SNPE:
                        SnpeConfig mSnpeClassifyConfig = new SnpeConfig(this.getAssets(),
                                "snpe-classify/config.json");
                        mClassifyDLManager = new SnpeManager(this, mSnpeClassifyConfig, SERIAL_NUM);
                        break;
                    case TYPE_INFER:
                    default:
                        InferConfig mInferConfig = new InferConfig(getAssets(),
                                "infer-classify/config.json");
                        // 可修改ARM推断使用的CPU核数，目前只有EasyDL模型支持多线程
                        if (mInferConfig.getProduct().equals("EasyDL")) {
                            mInferConfig.setThread(Util.getInferCores());
                        }
                        mClassifyDLManager = new InferManager(this, mInferConfig, SERIAL_NUM);
                        break;
                }

                canAutoRun = true;
                isInitializing = true;
            } catch (BaseException e) {
                Log.e("BaseException",e.toString());
                Log.e("CameraActivity", e.getClass().getSimpleName() + ":" + e.getErrorCode() + ":" + e.getMessage());
            }
        }
    }


    private void releaseEasyDL() {
        if (model == MODEL_DETECT) {
            if (mInferManager != null) {
                try {
                    mInferManager.destroy();
                } catch (BaseException e) {
                    Log.e("BaseException", e.toString());
                }
            }
        }
        if (model == MODEL_CLASSIFY) {
            if (mClassifyDLManager != null) {
                try {
                    mClassifyDLManager.destroy();
                } catch (ClassifyException e) {
                    Log.e("BaseException", e.toString());
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

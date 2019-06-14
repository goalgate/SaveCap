package cn.cbdi.savecap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.classify.ClassificationResultModel;
import com.baidu.ai.edge.core.classify.ClassifyException;
import com.baidu.ai.edge.core.classify.ClassifyInterface;
import com.baidu.ai.edge.core.classify.ClassifyOnline;
import com.baidu.ai.edge.core.ddk.DDKConfig;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.detect.DetectInterface;
import com.baidu.ai.edge.core.detect.DetectOnline;
import com.baidu.ai.edge.core.detect.DetectionResultModel;
import com.baidu.ai.edge.core.infer.InferConfig;
import com.baidu.ai.edge.core.infer.InferInterface;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.snpe.SnpeConfig;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import com.baidu.ai.edge.core.util.HttpUtil;
import com.baidu.ai.edge.core.util.Util;
import com.baidu.ai.edge.ui.activity.MainActivity;
import com.baidu.ai.edge.ui.activity.ResultListener;
import com.baidu.ai.edge.ui.view.model.ClassifyResultModel;
import com.baidu.ai.edge.ui.view.model.DetectResultModel;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruanshimin on 2018/10/31.
 */

public class CameraActivity extends MainActivity {

    // 请替换为您的序列号
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

    private boolean isInitializing = false;

    private boolean hasOnlineApi = false;
    // 模型加载状态
    private boolean modelLoadStatus = false;

    @Override
    /**
     * onCreate中调用
     */
    public void onActivityCreate() {
        int hasWriteStoragePermission =
                ActivityCompat.checkSelfPermission(getApplication(),
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
                    updateTakePictureButtonStatus();
                }
            }
        }.execute();
    }

    private void updateTakePictureButtonStatus() {
        if (modelLoadStatus || isOnline) {
            setTakePictureButtonAvailable(true);
        }
        if (!isOnline && !modelLoadStatus) {
            setTakePictureButtonAvailable(false);
        }
    }

    /**
     * 此处简化，建议一个mDetectManager对象在同一线程中调用
     */
    @Override
    public void onActivityDestory() {
        releaseEasyDL();
    }


    /**
     * 新线程中调用 ，从照相机中获取bitmap
     *
     * @param bitmap     RGBA格式
     * @param confidence [0-1）
     * @return
     */
    @Override
    public void onDetectBitmap(Bitmap bitmap, float confidence,
                               final ResultListener.DetectListener listener) {

        if (isOnline) {
            mOnlineDetect.detect(bitmap, confidence,
                    new DetectInterface.OnResultListener() {
                        @Override
                        public void onResult(List<DetectionResultModel> result) {
                            listener.onResult(fillDetectionResultModel(result));
                        }

                        @Override
                        public void onError(BaseException ex) {
                            listener.onResult(null);
                            showError(ex);
                        }
                    });
            return;
        }

        if (mInferManager == null) {
            showMessage("模型初始化中，请稍后");
            listener.onResult(null);
            return;
        }
        try {
            List<DetectionResultModel> modelList = mInferManager.detect(bitmap, confidence);
            listener.onResult(fillDetectionResultModel(modelList));
        } catch (BaseException e) {
            showError(e);
            listener.onResult(null);
        }
    }

    private List<DetectResultModel> fillDetectionResultModel(
            List<DetectionResultModel> modelList) {
        List<DetectResultModel> results = new ArrayList<>();
        for (int i = 0; i < modelList.size(); i++) {
            DetectionResultModel mDetectionResultModel = modelList.get(i);
            DetectResultModel mDetectResultModel = new DetectResultModel();
            mDetectResultModel.setIndex(i + 1);
            mDetectResultModel.setConfidence(mDetectionResultModel.getConfidence());
            mDetectResultModel.setName(mDetectionResultModel.getLabel());
            mDetectResultModel.setBounds(mDetectionResultModel.getBounds());
            results.add(mDetectResultModel);
        }
        return results;
    }

    @Override
    public void onClassifyBitmap(Bitmap bitmap, float confidence,
                                 final ResultListener.ClassifyListener listener) {
        if (isOnline) {
            mOnlineClassify.classify(bitmap, new ClassifyInterface.OnResultListener() {
                @Override
                public void onResult(List<ClassificationResultModel> result) {
                    listener.onResult(fillClassificationResultModel(result));
                }

                @Override
                public void onError(BaseException ex) {
                    listener.onResult(null);
                    showError(ex);
                }
            });
            return;
        }

        if (mClassifyDLManager == null) {
            showMessage("模型初始化中，请稍后");
            listener.onResult(null);
            return;
        }
        try {
            List<ClassificationResultModel> modelList = mClassifyDLManager.classify(bitmap);
            listener.onResult(fillClassificationResultModel(modelList));
        } catch (BaseException e) {
            showError(e);
            listener.onResult(null);
        }
    }

    private List<ClassifyResultModel> fillClassificationResultModel(
            List<ClassificationResultModel> modelList) {
        List<ClassifyResultModel> results = new ArrayList<>();
        for (int i = 0; i < modelList.size(); i++) {
            ClassificationResultModel mClassificationResultModel = modelList.get(i);
            ClassifyResultModel mClassifyResultModel = new ClassifyResultModel();
            mClassifyResultModel.setIndex(i + 1);
            mClassifyResultModel.setConfidence(mClassificationResultModel.getConfidence());
            mClassifyResultModel.setName(mClassificationResultModel.getLabel());
            results.add(mClassifyResultModel);
        }
        return results;
    }

    @Override
    public void dumpDetectResult(List<DetectResultModel> model, Bitmap bitmap, float min) {

    }

    @Override
    public void dumpClassifyResult(List<ClassifyResultModel> model, Bitmap bitmap, float min) {

    }

    private void showError(BaseException e) {
        showMessage(e.getErrorCode(), e.getMessage());
        Log.e("CameraActivity", e.getMessage(), e);
    }

    private void releaseEasyDL() {
        if (model == MODEL_DETECT) {
            if (mInferManager != null) {
                try {
                    mInferManager.destroy();
                } catch (BaseException e) {
                    showError(e);
                }
            }
        }
        if (model == MODEL_CLASSIFY) {
            if (mClassifyDLManager != null) {
                try {
                    mClassifyDLManager.destroy();
                } catch (ClassifyException e) {
                    showError(e);
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (pageCode == PAGE_CAMERA && !isInitializing) {
            showMessage("模型未初始化");
        } else {
            super.onBackPressed();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_FOR_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start();
            } else {
                Toast.makeText(getApplicationContext(),
                        "需要android.permission.READ_PHONE_STATE", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 是离线还是在线模式
    private boolean isOnline = false;

    @Override
    protected void onSetMenu(PopupMenu actionBarMenu) {
        if (!HttpUtil.isOnline(this) || !hasOnlineApi) {
            actionBarMenu.getMenu().findItem(R.id.online_mode).setEnabled(false);
        }
        if (isOnline) {
            actionBarMenu.getMenu().findItem(R.id.online_mode).setChecked(true);
        } else {
            actionBarMenu.getMenu().findItem(R.id.offline_mode).setChecked(true);
        }
    }

    @Override
    protected void onSetMenuItem(boolean isOnline) {
        if (this.isOnline == isOnline) {
            return;
        }
        this.isOnline = isOnline;
        updateTakePictureButtonStatus();
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
                showError(e);
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
                showError(e);
                Log.e("CameraActivity", e.getClass().getSimpleName() + ":" + e.getErrorCode() + ":" + e.getMessage());
            }
        }
    }
}



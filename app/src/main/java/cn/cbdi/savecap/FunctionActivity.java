package cn.cbdi.savecap;

import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.ImageView;

import com.baidu.aip.face.AutoTexturePreviewView;
import com.blankj.utilcode.util.BarUtils;
import com.trello.rxlifecycle2.components.RxActivity;

import butterknife.BindView;

import cn.cbdi.savecap.Function.Func_Face.mvp.presenter.FacePresenter;
import cn.cbdi.savecap.Function.Func_Face.mvp.view.IFaceView;

import static cn.cbdi.savecap.Function.Func_Face.mvp.Module.FaceImpl2.FEATURE_DATAS_UNREADY;

public abstract class FunctionActivity extends RxActivity implements IFaceView,ChooseWindow.OptionTypeListener {

    private String TAG = FaceCallActivity.class.getSimpleName();

    @BindView(R.id.preview_view)
    AutoTexturePreviewView previewView;

    @BindView(R.id.texture_view)
    TextureView textureView;

    @BindView(R.id.iv_result)
    ImageView iv_result;

    FacePresenter fp = FacePresenter.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarUtils.hideStatusBar(this);
        Log.e(TAG, "onCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        fp.FacePresenterSetView(this);
        fp.FaceIdentifyReady();
        fp.FaceIdentify();

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        fp.FaceSetNoAction();
        fp.setIdentifyStatus(FEATURE_DATAS_UNREADY);
        fp.PreviewCease();
        fp.FacePresenterSetView(null);

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");

    }

    @Override
    public void onBackPressed() {

    }





}

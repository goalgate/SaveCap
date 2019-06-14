package cn.cbdi.savecap.Function.Func_Camera.mvp.module;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.baidu.ai.edge.core.classify.ClassifyInterface;
import com.baidu.ai.edge.core.infer.InferInterface;

/**
 * Created by zbsz on 2017/5/19.
 */


public interface IPhotoModule {

    void Init(SurfaceView surfaceView, IOnSetListener listener);

    void setDisplay(SurfaceHolder sHolder);

    void capture();

    void getOneShut();

    void modelPrepare(boolean status, int detectOrClassify, ClassifyInterface mClassifyDLManager, InferInterface mInferManager);


    interface IOnSetListener {
        void onPhotoBack(String msg,Bitmap bitmap);

    }

}
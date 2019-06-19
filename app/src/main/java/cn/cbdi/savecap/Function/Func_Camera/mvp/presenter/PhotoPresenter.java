package cn.cbdi.savecap.Function.Func_Camera.mvp.presenter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.baidu.ai.edge.core.classify.ClassifyInterface;
import com.baidu.ai.edge.core.infer.InferInterface;

import cn.cbdi.savecap.Function.Func_Camera.mvp.module.IPhotoModule;
import cn.cbdi.savecap.Function.Func_Camera.mvp.module.PhotoModuleImpl;
import cn.cbdi.savecap.Function.Func_Camera.mvp.view.IPhotoView;


/**
 * Created by zbsz on 2017/6/9.
 */

public class PhotoPresenter {

    private IPhotoView view;

    private static PhotoPresenter instance = null;

    private PhotoPresenter() {
    }

    public static PhotoPresenter getInstance() {
        if (instance == null)
            instance = new PhotoPresenter();
        return instance;
    }

    public void PhotoPresenterSetView(IPhotoView view) {
        this.view = view;
    }

    IPhotoModule photoModule = new PhotoModuleImpl();

    public void init(SurfaceView surfaceView) {
        try {
            photoModule.Init(surfaceView,new IPhotoModule.IOnSetListener() {
                        @Override
                        public void onPhotoBack(String msg, Bitmap bitmap) {
                            view.onPhotoBack(msg, bitmap);
                        }
                    }
            );
        } catch (NullPointerException e) {
            Log.e("initCamera", e.toString());
        }
    }

    public void init(SurfaceView surfaceView,ImageView drawView) {
        try {
            photoModule.Init(surfaceView,drawView, new IPhotoModule.IOnSetListener() {
                        @Override
                        public void onPhotoBack(String msg, Bitmap bitmap) {
                            view.onPhotoBack(msg, bitmap);
                        }
                    }
            );
        } catch (NullPointerException e) {
            Log.e("initCamera", e.toString());
        }
    }

    public void init(SurfaceView surfaceView,SurfaceView drawRectView) {
        try {
            photoModule.Init(surfaceView,drawRectView, new IPhotoModule.IOnSetListener() {
                        @Override
                        public void onPhotoBack(String msg, Bitmap bitmap) {
                            view.onPhotoBack(msg, bitmap);
                        }
                    }
            );
        } catch (NullPointerException e) {
            Log.e("initCamera", e.toString());
        }
    }

    public void setMoelStatus(boolean status, int detectOrClassify, ClassifyInterface mClassifyDLManager, InferInterface mInferManager){
        photoModule.modelPrepare(status,detectOrClassify,mClassifyDLManager,mInferManager);
    }

    public void setDisplay(SurfaceHolder surfaceHolder) {
        try {
            photoModule.setDisplay(surfaceHolder);
        } catch (NullPointerException e) {
            Log.e("setDisplay", e.toString());
        }
    }

    public void capture() {
        try {
            photoModule.capture();
        } catch (NullPointerException e) {
            Log.e("capture", e.toString());
        }

    }

    public void getOneShut() {
        try {
            photoModule.getOneShut();
        } catch (NullPointerException e) {
            Log.e("getOneShut", e.toString());
        }
    }
    public void onActivityDestroy(){
        try {
            photoModule.onActivityDestroy();
        } catch (NullPointerException e) {
            Log.e("getOneShut", e.toString());
        }    }
}


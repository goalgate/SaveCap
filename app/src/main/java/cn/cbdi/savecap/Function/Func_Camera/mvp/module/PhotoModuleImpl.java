package cn.cbdi.savecap.Function.Func_Camera.mvp.module;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

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
import com.baidu.ai.edge.core.util.Util;
import com.baidu.ai.edge.ui.activity.ResultListener;
import com.baidu.ai.edge.ui.view.model.DetectResultModel;
import com.blankj.utilcode.util.ToastUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.cbdi.savecap.AppInit;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.baidu.ai.edge.ui.activity.MainActivity.MODEL_CLASSIFY;
import static com.baidu.ai.edge.ui.activity.MainActivity.MODEL_DETECT;

public class PhotoModuleImpl implements IPhotoModule, Camera.PreviewCallback {
    final static String ApplicationName = PhotoModuleImpl.class.getSimpleName();
    private Handler handler = new Handler(Looper.getMainLooper());

    boolean modelLoadStatus = false;
    float mConfidence = (float) 0.30;

    ClassifyInterface mClassifyDLManager;

    InferInterface mInferManager;

    SurfaceHolder cameraHolder;

    SurfaceHolder drawRectHolder;

    ImageView drawView;

    Camera camera;

    IPhotoModule.IOnSetListener callback;

    byte[] global_bytes;

    int detectOrClassify = 0;

    int width, height;

    int camWidth = 800;

    int camHeight = 600;

    private int surfaceWidth;

    private int surfaceHeight;

    float width_param;

    float height_param;

    private static final String TAG = PhotoModuleImpl.class.getSimpleName();


    @Override
    public void Init(SurfaceView surfaceView, IOnSetListener listener) {
        this.callback = listener;
        this.cameraHolder = surfaceView.getHolder();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                safeCameraOpen(1);
                setCameraParemeter();
                setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                releaseCamera();

            }
        });

    }

    @Override
    public void Init(SurfaceView surfaceView, SurfaceView drawRectView, IOnSetListener listener) {
        this.callback = listener;
        this.cameraHolder = surfaceView.getHolder();
        this.drawRectHolder = drawRectView.getHolder();
        this.cameraHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                safeCameraOpen(1);
                setCameraParemeter();
                setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                releaseCamera();

            }
        });
        this.drawRectHolder.setFormat(PixelFormat.TRANSLUCENT);
        drawRectView.setZOrderMediaOverlay(true);
    }


    @Override
    public void Init(SurfaceView surfaceView, ImageView drawView, IOnSetListener listener) {
        this.callback = listener;
        this.cameraHolder = surfaceView.getHolder();
        this.drawView = drawView;
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                safeCameraOpen(1);
                setCameraParemeter();
                setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                releaseCamera();

            }
        });
    }

    @Override
    public void setDisplay(SurfaceHolder sHolder) {
        try {
            if (camera != null) {
                camera.setPreviewDisplay(sHolder);
                camera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void capture() {
        camera.takePicture(new Camera.ShutterCallback() {
            public void onShutter() {
                // 按下快门瞬间会执行此处代码
            }
        }, new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera c) {
                // 此处代码可以决定是否需要保存原始照片信息
            }
        }, myJpegCallback);
    }

    @Override
    public void modelPrepare(boolean status, int detectOrClassify, ClassifyInterface mClassifyDLManager, InferInterface mInferManager) {
        modelLoadStatus = status;
        this.detectOrClassify = detectOrClassify;
        this.mClassifyDLManager = mClassifyDLManager;
        this.mInferManager = mInferManager;
    }

    Camera.PictureCallback myJpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, final Camera camera) {
            camera.stopPreview();
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            callback.onPhotoBack("拍照成功", bmp);
        }
    };

    private void safeCameraOpen(int id) {
        try {
            camera = Camera.open(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    @Override
    public void getOneShut() {
        Observable.just(global_bytes)
                .subscribeOn(Schedulers.computation())
                .unsubscribeOn(Schedulers.computation())
                .flatMap(new Function<byte[], ObservableSource<Bitmap>>() {
                    @Override
                    public ObservableSource<Bitmap> apply(byte[] bytes) throws Exception {
                        YuvImage image = new YuvImage(global_bytes, ImageFormat.NV21, width, height, null);
                        ByteArrayOutputStream os = new ByteArrayOutputStream(global_bytes.length);
                        if (!image.compressToJpeg(new Rect(0, 0, width, height), 100, os)) {
                            return null;
                        }
                        byte[] tmp = os.toByteArray();
                        Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
                        return Observable.just(bmp);
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Bitmap>() {
                    @Override
                    public void accept(Bitmap bitmap) throws Exception {
                        callback.onPhotoBack("ScreenShot", bitmap);
                    }
                });
    }


    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
        CalTaskThreadExecutor.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                if (modelLoadStatus) {
                    if (detectOrClassify == MODEL_DETECT) {
                        onDetectBitmap(convertPreviewDataToBitmap(data), mConfidence);
                    } else if (detectOrClassify == MODEL_CLASSIFY) {
                        onClassifyBitmap(convertPreviewDataToBitmap(data), mConfidence);
                    }
                }
            }
        });
    }

    private void setCameraParemeter() {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(camWidth, camHeight);
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.set("jpeg-quality", 100);
        surfaceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;  //屏幕宽度 1920
        surfaceHeight = Resources.getSystem().getDisplayMetrics().heightPixels;  //984
        camera.setParameters(parameters);
        camera.setDisplayOrientation(0);
        camera.setPreviewCallbackWithBuffer(PhotoModuleImpl.this);
        camera.addCallbackBuffer(new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8]);
        camera.setPreviewCallback(PhotoModuleImpl.this);
//        width_param = surfaceWidth / camWidth;
//        height_param = surfaceHeight / camHeight;
        width_param = 1;
        height_param = 1;
    }

    public void onDetectBitmap(Bitmap bitmap, float confidence) {
        try {
            List<DetectionResultModel> modelList = mInferManager.detect(bitmap, confidence);
            if (modelList.size() > 0) {
                Log.e("ListSize", String.valueOf(modelList.size()));
            }
            drawRectView(modelList, bitmap);
        } catch (BaseException e) {
            Log.e("BaseException", e.toString());
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

    public void onClassifyBitmap(Bitmap bitmap, float confidence) {
        try {
            List<ClassificationResultModel> modelList = mClassifyDLManager.classify(bitmap);
            Log.e("ListSize", String.valueOf(modelList.size()));
        } catch (BaseException e) {
            Log.e("BaseException", e.toString());
        }
    }

    private Bitmap convertPreviewDataToBitmap(byte[] data) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        YuvImage img = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
        ByteArrayOutputStream os = null;
        os = new ByteArrayOutputStream(data.length);
        img.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, os);
        byte[] jpeg = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return bitmap;
        }
    }

    Canvas canvas = new Canvas();
    Paint paint = new Paint();

    {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);   // 画笔粗度
    }

    Paint paintText = new Paint();

    {
        paintText.setTextSize(40);
        paintText.setColor(Color.BLUE);
        paintText.setTextAlign(Paint.Align.LEFT);
    }

    private void drawRectView(List<DetectionResultModel> listD, Bitmap bitmap) {
        final Bitmap mbitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.setBitmap(mbitmap);
        for (DetectionResultModel dr : listD) {

            Rect rec = dr.getBounds();
            if (rec.left < 0) {
                rec.left = 0;
            }
            if (rec.right < 0) {
                rec.right = 0;
            }
            if (rec.top < 0) {
                rec.top = 0;
            }
            if (rec.bottom < 0) {
                rec.bottom = 0;
            }
            rec.left = (int) (rec.left * width_param);
            rec.top = (int) (rec.top * height_param);
            rec.right = (int) (rec.right * width_param);
            rec.bottom = (int) (rec.bottom * height_param);
//            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawRect(rec, paint);
            canvas.drawText(dr.getLabel(), rec.left, rec.top+30, paintText);
        }
        canvas.save();
        handler.post(new Runnable() {
            @Override
            public void run() {
                drawView.setImageBitmap(mbitmap);
            }
        });


    }


    private void drawBoundingBox(List<DetectionResultModel> listD) {
        canvas = drawRectHolder.lockCanvas(null);
        if (canvas == null) return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        for (DetectionResultModel dr : listD) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(5);   // 画笔粗度
            Rect rec = dr.getBounds();
            if (rec.left < 0) {
                rec.left = rec.left + camWidth;
            }
            if (rec.right < 0) {
                rec.right = rec.right + camWidth;
            }
            if (rec.top < 0) {
                rec.top = rec.top + camHeight;
            }
            if (rec.bottom < 0) {
                rec.bottom = rec.bottom + camWidth;
            }
            rec.left = (int) (rec.left * width_param);
            rec.top = (int) (rec.top * height_param);
            rec.right = (int) (rec.right * width_param);
            rec.bottom = (int) (rec.bottom * height_param);
            canvas.drawRect(rec, paint);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setTextSize(40);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.LEFT);
        }
        drawRectHolder.unlockCanvasAndPost(canvas);
    }
}

class CalTaskThreadExecutor {
    private static final ExecutorService instance = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<Runnable>(2),
            new ThreadFactory() {
                private final AtomicInteger mCount = new AtomicInteger(1);

                public Thread newThread(Runnable r) {
                    return new Thread(r, "SingleTaskPoolThread #" + mCount.getAndIncrement());
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    Log.e("TAG", "超了");
                    executor.remove(r);
                }
            });

    public static ExecutorService getInstance() {
        return instance;
    }


}

package cn.cbdi.savecap;

import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

import com.baidu.aip.api.FaceApi;
import com.baidu.aip.entity.User;
import com.bigkoo.alertview.AlertView;
import com.bigkoo.alertview.OnItemClickListener;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.cbdi.savecap.Bean.ICardInfo;
import cn.cbdi.savecap.Function.Func_Face.mvp.presenter.FacePresenter;
import cn.cbdi.savecap.Tools.ActivityCollector;
import cn.cbdi.savecap.greendao.DaoSession;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import jxl.Sheet;
import jxl.Workbook;

import static cn.cbdi.savecap.Function.Func_Face.mvp.Module.FaceImpl2.FEATURE_DATAS_UNREADY;

public class FaceCallActivity extends FunctionActivity {
    DaoSession mdaoSession = AppInit.getInstance().getDaoSession();

    ChooseWindow chooseWindow;

    AlertView personListView;

    @BindView(R.id.gestures_overlay)
    GestureOverlayView gestures;

    GestureLibrary mGestureLib;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facecall);
        ButterKnife.bind(this);
        ActivityCollector.addActivity(this);
        getXlsData(Environment.getExternalStorageDirectory() + File.separator + "videocall" + File.separator + "视频点名名单.xls");
        UIPrepare();
    }


    @Override
    public void onStart() {
        super.onStart();
        fp.CameraPreview(AppInit.getContext(), previewView, textureView);
    }

    @Override
    public void onOptionType(Button view, int type) {
        chooseWindow.dismiss();
        if (type == 1) {
            fp.FaceIdentifyReady();
            fp.FaceIdentify();
        } else if (type == 2) {
            personListView.show();
        }else if (type == 3) {
            Intent intent = new Intent(FaceCallActivity.this,AnalysisActivity.class);
            startActivity(intent);
            this.finish();
        }
    }

    @Override
    public void onUser(FacePresenter.FaceResultType resultType, User user) {
        if (resultType.equals(FacePresenter.FaceResultType.Reg_success)) {
            if (FaceApi.getInstance().userAdd(user)) {
                ToastUtils.showLong(user.getUserInfo() + "人脸数据登记成功");
            }
        } else if (resultType.equals(FacePresenter.FaceResultType.Identify)) {
            ToastUtils.showLong(user.getUserInfo() + "已记录");

        }
    }

    @Override
    public void onBitmap(FacePresenter.FaceResultType resultType, Bitmap bitmap) {
        if (resultType.equals(FacePresenter.FaceResultType.Reg_success) || resultType.equals(FacePresenter.FaceResultType.Identify)) {
            iv_result.setImageBitmap(bitmap);
            iv_result.setVisibility(View.VISIBLE);
            Observable.timer(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(FaceCallActivity.this.<Long>bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribe(new Observer<Long>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Long aLong) {
                            iv_result.setVisibility(View.GONE);

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });

        }

    }

    @Override
    public void onText(FacePresenter.FaceResultType resultType, String text) {

    }

    private void getXlsData(String xlsName) {
        Observable.just(xlsName).flatMap(new Function<String, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(String s) throws Exception {
                try {
                    Workbook workbook = Workbook.getWorkbook(new File(s));
                    Sheet sheet = workbook.getSheet(0);
                    int sheetRows = sheet.getRows();
                    for (int i = 2; i < sheetRows; i++) {
                        try {
                            mdaoSession.queryRaw(ICardInfo.class, "where CARD_ID = '" + sheet.getCell(1, i).getContents().toUpperCase() + "'").get(0);
                        } catch (IndexOutOfBoundsException e) {
                            ICardInfo cardInfo = new ICardInfo();
                            cardInfo.setName(sheet.getCell(0, i).getContents());
                            cardInfo.setCardId(sheet.getCell(1, i).getContents());
                            mdaoSession.insert(cardInfo);
                        }
                    }
                    workbook.close();
                    return Observable.just("数据读写完毕");
                } catch (Exception e) {
                    return Observable.just("数据读写失败");
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        ToastUtils.showLong(s);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void UIPrepare() {
        final List<ICardInfo> cardInfoList = AppInit.getInstance().getDaoSession().loadAll(ICardInfo.class);
        String[] cardInfos = new String[cardInfoList.size()];
        for (int i = 0; i < cardInfoList.size(); i++) {
            cardInfos[i] = cardInfoList.get(i).getName();
        }
        personListView = new AlertView("请挑选要录入人脸数据的人员", null, "取消",
                cardInfos, null,
                FaceCallActivity.this, AlertView.Style.ActionSheet, new OnItemClickListener() {
            @Override
            public void onItemClick(Object o, final int position) {
                if (position != -1) {
                    final int optionPOS = position;
                    Observable.timer(1, TimeUnit.SECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .compose(FaceCallActivity.this.<Long>bindUntilEvent(ActivityEvent.DESTROY))
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) throws Exception {
                                    new AlertView("请选择接下来的操作", null, "取消", new String[]{"人员录入人脸数据", "删除人员"}, null, FaceCallActivity.this, AlertView.Style.Alert, new OnItemClickListener() {
                                        @Override
                                        public void onItemClick(Object o, int position) {
                                            if(position==0){
                                                fp.FaceSetNoAction();
                                                fp.setIdentifyStatus(FEATURE_DATAS_UNREADY);
                                                fp.FaceReg(cardInfoList.get(optionPOS), null);
                                                ToastUtils.showLong("等待人脸数据返回");
                                            }else if(position==1){
                                                fp.FaceSetNoAction();
                                                fp.setIdentifyStatus(FEATURE_DATAS_UNREADY);
                                                Observable.timer(1, TimeUnit.SECONDS)
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .compose(FaceCallActivity.this.<Long>bindUntilEvent(ActivityEvent.DESTROY))
                                                        .subscribe(new Consumer<Long>() {
                                                            @Override
                                                            public void accept(@NonNull Long aLong) throws Exception {
                                                                new AlertView("是否确定删除" + cardInfoList.get(optionPOS).getName(), null, "取消", new String[]{"确定"}, null, FaceCallActivity.this, AlertView.Style.Alert, new OnItemClickListener() {
                                                                    @Override
                                                                    public void onItemClick(Object o, int position) {
                                                                        if (position == 0) {
                                                                            FaceApi.getInstance().userDelete(cardInfoList.get(optionPOS).getCardId(),"1");
                                                                            mdaoSession.delete(cardInfoList.get(optionPOS));
                                                                            ToastUtils.showLong(cardInfoList.get(optionPOS).getName()+"已被删除");
                                                                        }
                                                                    }
                                                                }).show();
                                                            }
                                                        });

                                            }

                                        }
                                    }).show();
                                }
                            });

                }
            }
        });
        setGesture();

    }

    private void setGesture() {
        gestures.setGestureStrokeType(GestureOverlayView.GESTURE_STROKE_TYPE_MULTIPLE);
        gestures.setGestureVisible(false);
        gestures.addOnGesturePerformedListener(new GestureOverlayView.OnGesturePerformedListener() {
            @Override
            public void onGesturePerformed(GestureOverlayView overlay,
                                           Gesture gesture) {
                ArrayList<Prediction> predictions = mGestureLib.recognize(gesture);
                if (predictions.size() > 0) {
                    Prediction prediction = (Prediction) predictions.get(0);
                    // 匹配的手势
                    if (prediction.score > 1.0) { // 越匹配score的值越大，最大为10
                        if (prediction.name.equals("setting")) {
                            chooseWindow = new ChooseWindow(FaceCallActivity.this);
                            chooseWindow.setOptionTypeListener(FaceCallActivity.this);
                            chooseWindow.showAtLocation(getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

                        }
                    }
                }
            }
        });
        if (mGestureLib == null) {
            mGestureLib = GestureLibraries.fromRawResource(this, R.raw.gestures);
            mGestureLib.load();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCollector.finishAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }
}

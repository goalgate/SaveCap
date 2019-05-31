package com.baidu.ai.edge.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.baidu.ai.edge.ui.util.StringUtil;
import com.baidu.ai.edge.ui.view.model.DetectResultModel;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by ruanshimin on 2018/5/15.
 */

public class ResultMaskView extends View {
    private float sizeRatio;
    private List<DetectResultModel> mDetectResultModelList;
    private Point originPt = new Point();
    private int imgWidth;
    private int imgHeight;

    private Handler handler;

    public void setHandler(Handler mHandler) {
        handler = mHandler;
    }

    public ResultMaskView(Context context) {
        super(context);
    }

    public ResultMaskView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ResultMaskView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }


    public void setRectListInfo(List<DetectResultModel> modelList, int width, int height) {
        imgWidth = width;
        imgHeight = height;
        mDetectResultModelList = modelList;
        handler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    public void clear() {
        mDetectResultModelList = null;
        handler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    private void preCaculate() {
        float ratio = (float) getMeasuredWidth() / (float) getMeasuredHeight();
        float ratioBitmap = (float) imgWidth / (float) imgHeight;
        // | |#####| |模式
        if (ratioBitmap < ratio) {
            sizeRatio = (float) getMeasuredHeight() / (float) imgHeight;
            int x = (int) (getMeasuredWidth() - sizeRatio * imgWidth) / 2;
            originPt.set(x, 0);
        } else {
            // ------------
            //
            // ------------
            // ############
            // ------------
            //
            // ------------
            sizeRatio = (float) getMeasuredWidth() / (float) imgWidth;
            int y = (int) (getMeasuredHeight() - sizeRatio * imgHeight) / 2;
            originPt.set(0, y);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 实时识别的时候第一次渲染
        if (mDetectResultModelList == null) {
            super.onDraw(canvas);
            return;
        }

        preCaculate();

        int stokeWidth = 5;

        int fontSize = 38;
        int labelPadding = 5;
        int labelHeight = 46 + 2 * labelPadding;
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#3B85F5"));
        paint.setStrokeWidth(stokeWidth);
        paint.setStyle(Paint.Style.STROKE);

        Paint paintFillAlpha = new Paint();
        paintFillAlpha.setStyle(Paint.Style.FILL);
        paintFillAlpha.setColor(Color.parseColor("#3B85F5"));
        paintFillAlpha.setAlpha(50);

        Paint paintFill = new Paint();
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(Color.parseColor("#3B85F5"));

        Paint paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(fontSize);
        DecimalFormat df = new DecimalFormat("0.00");
        for (int i = 0; i < mDetectResultModelList.size(); i++) {
            DetectResultModel model = mDetectResultModelList.get(i);
            Rect rect = model.getBounds(sizeRatio, originPt);
            canvas.drawRect(rect, paint);
            canvas.drawRect(rect, paintFillAlpha);
            canvas.drawRect(new Rect(rect.left, rect.top, rect.right,
                    rect.top + labelHeight), paintFill);
            canvas.drawText(model.getName() + " " + StringUtil.formatFloatString(model.getConfidence()),
                    rect.left + labelPadding,
                    rect.top + fontSize + labelPadding, paintText);
        }

        super.onDraw(canvas);
    }
}

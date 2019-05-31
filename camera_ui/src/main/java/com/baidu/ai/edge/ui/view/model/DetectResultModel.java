package com.baidu.ai.edge.ui.view.model;

import android.graphics.Point;
import android.graphics.Rect;

/**
 * Created by ruanshimin on 2018/5/13.
 */

public class DetectResultModel extends ClassifyResultModel {
    private int index;
    private String name;
    private Rect bounds;

    public Rect getBounds() {
        return bounds;
    }

    public Rect getBounds(float ratio, Point origin) {
        return new Rect((int) (origin.x + bounds.left * ratio),
                (int) (origin.y + bounds.top * ratio),
                (int) (origin.x + bounds.right * ratio),
                (int) (origin.y + bounds.bottom * ratio));
    }

    public void setBounds(Rect bounds) {
        this.bounds = bounds;
    }

    public DetectResultModel() {

    }

    public DetectResultModel(int index, String name, float confidence, Rect bounds) {
        this.index = index;
        this.name = name;
        this.confidence = confidence;
        this.bounds = bounds;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    private float confidence;
}

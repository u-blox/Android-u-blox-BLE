package com.ublox.BLE.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.ublox.BLE.R;

import java.util.ArrayList;
import java.util.List;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class SimpleGraph extends View {
    private static final float DEFAULT_STROKE_WIDTH = 0.0f;
    private static final float DEFAULT_STARTING_BOUND = 0.0f;

    private Paint graphPaint;
    private Paint backPaint;
    private Paint textPaint;
    private float graphThickness;
    private boolean fillUnder;
    private String yAxisLabel, xAxisLabel;
    private float yStartingMin, yStartingMax;
    private float xStartingMin, xStartingMax;
    private float yCurrentMin, yCurrentMax;
    private float xCurrentMin, xCurrentMax;
    private List<Float> xValues;
    private List<Float> yValues;

    public SimpleGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        graphPaint = new Paint();
        backPaint = new Paint();
        backPaint.setColor(Color.WHITE);
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        xValues = new ArrayList<>();
        yValues = new ArrayList<>();

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
            attrs, R.styleable.SimpleGraph, 0, 0
        );
        try {
            graphPaint.setColor(attributes.getColor(R.styleable.SimpleGraph_graphColor, Color.BLACK));
            graphThickness = attributes.getFloat(R.styleable.SimpleGraph_graphThickness, DEFAULT_STROKE_WIDTH);
            graphThickness = Math.max(graphThickness, DEFAULT_STROKE_WIDTH);
            fillUnder = attributes.getBoolean(R.styleable.SimpleGraph_fillUnder, false);
            graphPaint.setStrokeWidth(fillUnder ? DEFAULT_STROKE_WIDTH : graphThickness);

            yAxisLabel = attributes.getString(R.styleable.SimpleGraph_yAxisLabel);
            if (yAxisLabel == null) yAxisLabel = "";
            xAxisLabel = attributes.getString(R.styleable.SimpleGraph_xAxisLabel);
            if (xAxisLabel == null) xAxisLabel = "";

            yCurrentMin = yStartingMin = attributes.getFloat(R.styleable.SimpleGraph_yStartingMin, DEFAULT_STARTING_BOUND);
            yCurrentMax = yStartingMax = attributes.getFloat(R.styleable.SimpleGraph_yStartingMax, DEFAULT_STARTING_BOUND);
            xCurrentMin = xStartingMin = attributes.getFloat(R.styleable.SimpleGraph_xStartingMin, DEFAULT_STARTING_BOUND);
            xCurrentMax = xStartingMax = attributes.getFloat(R.styleable.SimpleGraph_xStartingMax, DEFAULT_STARTING_BOUND);
        } finally {
            attributes.recycle();
        }
    }

    public int getGraphColor() {
        return graphPaint.getColor();
    }

    public void setGraphColor(int graphColor) {
        if (graphPaint.getColor() == graphColor) return;
        graphPaint.setColor(graphColor);
        invalidate();
    }

    public float getGraphThickness() {
        return graphThickness;
    }

    public void setGraphThickness(float graphThickness) {
        if (this.graphThickness == graphThickness) return;
        this.graphThickness = Math.max(graphThickness, DEFAULT_STROKE_WIDTH);
        if (!fillUnder) {
            graphPaint.setStrokeWidth(graphThickness);
        }
        invalidate();
    }

    public boolean getFillUnder() {
        return fillUnder;
    }

    public void setFillUnder(boolean fillUnder) {
        if (this.fillUnder == fillUnder) return;
        this.fillUnder = fillUnder;
        graphPaint.setStrokeWidth(fillUnder ? DEFAULT_STROKE_WIDTH : graphThickness);
        invalidate();
    }

    public String getyAxisLabel() {
        return yAxisLabel;
    }

    public void setyAxisLabel(String yAxisLabel) {
        if (this.yAxisLabel.equals(yAxisLabel)) return;
        this.yAxisLabel = yAxisLabel != null ? yAxisLabel : "";
        invalidate();
    }

    public String getxAxisLabel() {
        return xAxisLabel;
    }

    public void setxAxisLabel(String xAxisLabel) {
        if (this.xAxisLabel.equals(xAxisLabel)) return;
        this.xAxisLabel = xAxisLabel != null ? xAxisLabel : "";
        invalidate();
    }

    public float getyStartingMin() {
        return yStartingMin;
    }

    public void setyStartingMin(float min) {
        if (min == yStartingMin) return;
        yStartingMin = min;
        if (yStartingMin >= yCurrentMin) return;
        yCurrentMin = yStartingMin;
        invalidate();
    }

    public float getyStartingMax() {
        return yStartingMax;
    }

    public void setyStartingMax(float max) {
        if (max == yStartingMax) return;
        yStartingMax = max;
        if (yStartingMax <= yCurrentMax) return;
        yCurrentMax = yStartingMax;
        invalidate();
    }

    public float getxStartingMin() {
        return xStartingMin;
    }

    public void setxStartingMin(float min) {
        if (min == xStartingMin) return;
        xStartingMin = min;
        if (xStartingMin >= xCurrentMin) return;
        xCurrentMin = xStartingMin;
        invalidate();
    }

    public float getxStartingMax() {
        return xStartingMax;
    }

    public void setxStartingMax(float max) {
        if (max == xStartingMax) return;
        xStartingMax = max;
        if (xStartingMax <= xCurrentMax) return;
        xCurrentMax = xStartingMax;
        invalidate();
    }

    public void setValues(List<Float> xValues, List<Float> yValues) {
        if (xValues == null) xValues = new ArrayList<>();
        if (yValues == null) yValues = new ArrayList<>();
        if (xValues.size() != yValues.size()) throw new IllegalArgumentException("x and y values must be of same length");

        this.xValues.clear();
        this.yValues.clear();

        xCurrentMin = xStartingMin;
        xCurrentMax = xStartingMax;
        for (float x: xValues) {
            xCurrentMin = Math.min(x, xCurrentMin);
            xCurrentMax = Math.max(x, xCurrentMax);
            this.xValues.add(x);
        }

        yCurrentMin = yStartingMin;
        yCurrentMax = yStartingMax;
        for (float y: yValues) {
            yCurrentMin = Math.min(y, yCurrentMin);
            yCurrentMax = Math.max(y, yCurrentMax);
            this.yValues.add(y);
        }

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        canvas.drawRect(0, 0, width, height, backPaint);

        if (xValues.size() >= 2) {
            float xScale = width / (xCurrentMax - xCurrentMin);
            float yScale = height / (yCurrentMax - yCurrentMin);
            float x0 = (xValues.get(0) - xCurrentMin) * xScale;
            float y0 = height - (yValues.get(0) - yCurrentMin) * yScale;

            Path fillunderpath = new Path();
            if (fillUnder) {
                fillunderpath.moveTo(0, height);
                fillunderpath.lineTo(x0, y0);
            }

            for (int i = 1; i < xValues.size(); i++) {
                float x1 = (xValues.get(i) - xCurrentMin) * xScale;
                float y1 = height - (yValues.get(i) - yCurrentMin) * yScale;

                if (fillUnder) {
                    fillunderpath.lineTo(x1, y1);
                } else {
                    canvas.drawLine(x0, y0, x1, y1, graphPaint);
                }

                x0 = x1;
                y0 = y1;
            }

            if (fillUnder) {
                fillunderpath.lineTo(x0, height);
                fillunderpath.close();
                canvas.drawPath(fillunderpath, graphPaint);
            }
        }

        textPaint.setTextSize(0.1f * height);

        String yTopLabel = combineLabel(yCurrentMax, yAxisLabel);
        String yBottomLabel = combineLabel(yCurrentMin, yAxisLabel);
        String xLeftLabel = combineLabel(xCurrentMin, xAxisLabel);
        String xRightLabel = combineLabel(xCurrentMax, xAxisLabel);

        Rect yTopBounds = getTextBounds(yTopLabel);
        Rect xLeftBounds = getTextBounds(xLeftLabel);
        Rect xRightBounds = getTextBounds(xRightLabel);

        canvas.drawText(yTopLabel, 0, -yTopBounds.top, textPaint);
        canvas.drawText(yBottomLabel, 0, height + xLeftBounds.top, textPaint);
        canvas.drawText(xLeftLabel, 0, height, textPaint);
        canvas.drawText(xRightLabel, width - xRightBounds.right, height, textPaint);
    }

    private String combineLabel(float value, String partialLabel) {
        return String.valueOf(value) + ' ' + partialLabel;
    }

    private Rect getTextBounds(String text) {
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }
}

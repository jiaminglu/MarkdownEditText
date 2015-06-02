package com.jiaminglu.markdownedittext;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;

/**
 * Created by jiaminglu on 15/6/2.
 */
public class DotShape extends Shape {

    public void setRadius(float radius) {
        this.radius = radius;
    }

    float radius;

    public DotShape(float radius) {
        this.radius = radius;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, paint);
    }
}

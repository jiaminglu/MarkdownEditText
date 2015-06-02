package com.jiaminglu.markdownedittext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import java.lang.ref.WeakReference;

/**
 * Created by jiaminglu on 15/6/2.
 */
public class CenteredImageSpan extends ImageSpan {

    // Extra variables used to redefine the Font Metrics when an ImageSpan is added
    private int initialDescent = 0;
    private int extraSpace = 0;

    public CenteredImageSpan(Drawable b) {
        super(b);
    }

    public CenteredImageSpan(Drawable b, int verticalAlignment) {
        super(b, verticalAlignment);
    }

    public CenteredImageSpan(Context context, int resourceId, int verticalAlignment) {
        super(context, resourceId, verticalAlignment);
    }

    public CenteredImageSpan(Context context, int resourceId) {
        super(context, resourceId);
    }

    @Override
    public int getSize(Paint paint, CharSequence text,
                       int start, int end,
                       Paint.FontMetricsInt fm) {
        return super.getSize(paint, text, start, end, fm) + spacing;
    }

    int spacing;

    public CenteredImageSpan setSpacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {
        Drawable b = getCachedDrawable();
        canvas.save();

        int bCenter = b.getIntrinsicHeight() / 2;
        int fontTop = paint.getFontMetricsInt().top;
        int fontBottom = paint.getFontMetricsInt().bottom;
        int transY = (bottom - b.getBounds().bottom) -
                (((fontBottom - fontTop) / 2) - bCenter);


        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }

    private Drawable getCachedDrawable() {
        WeakReference<Drawable> wr = mDrawableRef;
        Drawable d = null;

        if (wr != null)
            d = wr.get();

        if (d == null) {
            d = getDrawable();
            mDrawableRef = new WeakReference<Drawable>(d);
        }

        return d;
    }

    private WeakReference<Drawable> mDrawableRef;
}

package com.jiaminglu.markdownedittext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.style.ImageSpan;

import java.lang.ref.WeakReference;

/**
 * Created by jiaminglu on 15/6/2.
 */
public class CenteredImageSpan extends ImageSpan {

    public CenteredImageSpan(Drawable b) {
        super(b, android.text.style.DynamicDrawableSpan.ALIGN_BASELINE);
    }

    public CenteredImageSpan(Context context, int resourceId) {
        super(context, resourceId, android.text.style.DynamicDrawableSpan.ALIGN_BASELINE);
    }

    @Override
    public int getSize(Paint paint, CharSequence text,
                       int start, int end,
                       Paint.FontMetricsInt fm) {
        Drawable d = getCachedDrawable();

        if (fm != null) {
            Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
            fm.ascent = fmi.ascent;
            fm.descent = fmi.descent;

            fm.top = fmi.top;
            fm.bottom = fmi.bottom;
        }

        return d.getBounds().right + spacing;
    }

    private int spacing;
    private int offset;

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        Drawable b = getCachedDrawable();
        canvas.save();

        int bCenter = b.getIntrinsicHeight() / 2;
        int fontTop = paint.getFontMetricsInt().ascent;
        int fontBottom = paint.getFontMetricsInt().descent;
        int transY = (bottom - b.getBounds().bottom) -
                (((fontBottom - fontTop) / 2) - bCenter) - offset;


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
            mDrawableRef = new WeakReference<>(d);
        }

        return d;
    }

    private WeakReference<Drawable> mDrawableRef;
}

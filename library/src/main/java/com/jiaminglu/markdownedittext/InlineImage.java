package com.jiaminglu.markdownedittext;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.style.DynamicDrawableSpan;

import java.util.regex.Matcher;

/**
 * Created by jiaming on 15-6-12.
 */
public class InlineImage extends DynamicDrawableSpan {
    private MarkdownEditText markdownEditText;
    Spannable spannable;

    InlineImage(MarkdownEditText markdownEditText, Drawable drawable) {
        super(ALIGN_BASELINE);
        this.drawable = drawable;
        this.markdownEditText = markdownEditText;
    }

    public void setDrawable(Drawable drawable) {
        if (this.drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) this.drawable).getBitmap();
            if (bitmap != null)
                bitmap.recycle();
        }
        this.drawable = drawable;
        markdownEditText.getText().setSpan(this,
                markdownEditText.getText().getSpanStart(this),
                markdownEditText.getText().getSpanEnd(this),
                markdownEditText.getText().getSpanFlags(this));
    }

    public String getSrc() {
        assert spannable == null; // Must be called after image inserted
        int start = markdownEditText.getText().getSpanStart(this);
        int end = markdownEditText.getText().getSpanEnd(this);
        Matcher matcher = markdownEditText.imagePattern.matcher(markdownEditText.getText().subSequence(start, end));
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    public void setSrc(String src) {
        assert spannable == null; // Must be called after image inserted
        int start = markdownEditText.getText().getSpanStart(this);
        int end = markdownEditText.getText().getSpanEnd(this);
        Matcher matcher = markdownEditText.imagePattern.matcher(markdownEditText.getText().subSequence(start, end));
        if (matcher.matches()) {
            markdownEditText.getText().replace(start + matcher.start(2), start + matcher.end(2), src);
            if (markdownEditText.imageHandler != null) {
                markdownEditText.imageHandler.fetch(this, src);
            }
        }
    }

    public void removeSelf() {
        int start = markdownEditText.getText().getSpanStart(this);
        int end = markdownEditText.getText().getSpanEnd(this);
        markdownEditText.getText().removeSpan(this);
        markdownEditText.getText().replace(start, end, "");
    }

    Drawable drawable;

    @Override
    public Drawable getDrawable() {
        return drawable;
    }

    /*
     * Copy-paste of super.getSize(...) but use getDrawable() to get the image/frame to calculate the size,
     * instead of the cached drawable.
     */
    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
            fm.ascent = -rect.bottom;
            fm.descent = 0;

            fm.top = fm.ascent;
            fm.bottom = 0;
        }

        return rect.right;
    }

    /*
     * Copy-paste of super.draw(...) but use getDrawable() to get the image/frame to draw, instead of
     * the cached drawable.
     */
    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Drawable b = getDrawable();
        canvas.save();

        int transY = bottom - b.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        }

        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();

    }
}

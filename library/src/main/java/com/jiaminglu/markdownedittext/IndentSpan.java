package com.jiaminglu.markdownedittext;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

/**
 * Created by jiaminglu on 15/6/2.
 */
public class IndentSpan implements LeadingMarginSpan {

    int level;
    int width;

    public IndentSpan(int level, int width) {
        this.level = level;
        this.width = width;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return level * width;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {

    }
}

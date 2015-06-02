package com.jiaminglu.markdownedittext;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.style.StyleSpan;

/**
 * Created by jiaminglu on 15/6/2.
 */
public class ItalicSpan extends StyleSpan {
    public ItalicSpan() {
        super(Typeface.ITALIC);
    }
    public ItalicSpan(Parcel src) {
        super(src);
    }
}

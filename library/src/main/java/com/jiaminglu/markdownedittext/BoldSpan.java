package com.jiaminglu.markdownedittext;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.style.StyleSpan;

/**
 * Created by jiaminglu on 15/6/2.
 */
public class BoldSpan extends StyleSpan {
    public BoldSpan() {
        super(Typeface.BOLD);
    }
    public BoldSpan(Parcel src) {
        super(src);
    }
}

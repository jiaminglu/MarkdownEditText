package com.jiaminglu.markdownedittext.style;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.style.StyleSpan;

/**
 * Created by jiaminglu on 15/6/13.
 */
public class BoldStyle extends StyleSpan implements Style {
    public BoldStyle() {
        super(Typeface.BOLD);
    }
    public BoldStyle(Parcel src) {
        super(src);
    }
}

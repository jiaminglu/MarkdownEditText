package com.jiaminglu.markdownedittext.style;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.style.StyleSpan;

/**
 * Created by jiaminglu on 15/6/13.
 */
public class ItalicStyle extends StyleSpan implements Style {
    public ItalicStyle() {
        super(Typeface.ITALIC);
    }
    public ItalicStyle(Parcel src) {
        super(src);
    }
}

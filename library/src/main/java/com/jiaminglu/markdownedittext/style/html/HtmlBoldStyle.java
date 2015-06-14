package com.jiaminglu.markdownedittext.style.html;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.style.StyleSpan;

import com.jiaminglu.markdownedittext.style.Style;

/**
 * Created by jiaminglu on 15/6/13.
 */
public class HtmlBoldStyle extends StyleSpan implements Style {
    public HtmlBoldStyle() {
        super(Typeface.BOLD);
    }
    public HtmlBoldStyle(Parcel src) {
        super(src);
    }
    @Override
    public String getStartTag() {
        return "<strong>";
    }

    @Override
    public String getEndTag() {
        return "</strong>";
    }
}

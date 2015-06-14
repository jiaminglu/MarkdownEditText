package com.jiaminglu.markdownedittext.style.html;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.style.StyleSpan;

import com.jiaminglu.markdownedittext.style.Style;

/**
 * Created by jiaminglu on 15/6/13.
 */
public class HtmlItalicStyle extends StyleSpan implements Style {
    public HtmlItalicStyle() {
        super(Typeface.ITALIC);
    }
    public HtmlItalicStyle(Parcel src) {
        super(src);
    }
    @Override
    public String getStartTag() {
        return "<em>";
    }

    @Override
    public String getEndTag() {
        return "</em>";
    }
}

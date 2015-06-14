package com.jiaminglu.markdownedittext.style.html;

import android.text.style.UnderlineSpan;

import com.jiaminglu.markdownedittext.style.Style;

/**
 * Created by jiaminglu on 15/6/13.
 */
public class HtmlUnderlineStyle extends UnderlineSpan implements Style {
    @Override
    public String getStartTag() {
        return "<u>";
    }

    @Override
    public String getEndTag() {
        return "</u>";
    }
}

package com.jiaminglu.markdownedittext.style.html;

import android.text.style.StrikethroughSpan;

import com.jiaminglu.markdownedittext.style.Style;

/**
 * Created by jiaminglu on 15/6/13.
 */
public class HtmlStrikethroughStyle extends StrikethroughSpan implements Style {
    @Override
    public String getStartTag() {
        return "<del>";
    }

    @Override
    public String getEndTag() {
        return "</del>";
    }
}

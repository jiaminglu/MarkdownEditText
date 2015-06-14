package com.jiaminglu.markdownedittext.syntax;

import com.jiaminglu.markdownedittext.style.Style;

/**
 * Created by jiaminglu on 15/6/13.
 */
public interface Syntax {
    void parse(CharSequence paragraph, OnTag onTag);
    String getStartTag(Style style);
    String getEndTag(Style style);

    interface OnTag {
        void onTag(int position, Tag tag);
    }

    interface Tag {
        Style getStyle();

        boolean isOpening();

        boolean isClosing();

        int getSize();
    }
}

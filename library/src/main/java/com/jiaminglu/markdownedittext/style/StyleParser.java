package com.jiaminglu.markdownedittext.style;

import com.jiaminglu.markdownedittext.MarkdownEditText;

/**
 * Created by jiaminglu on 15/6/13.
 */
public interface StyleParser {
    void parse(CharSequence paragraph, OnTag onTag);

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

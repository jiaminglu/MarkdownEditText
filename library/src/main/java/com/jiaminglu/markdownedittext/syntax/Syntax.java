package com.jiaminglu.markdownedittext.syntax;

import android.text.style.CharacterStyle;

/**
 * Created by jiaminglu on 15/6/13.
 */
public interface Syntax {
    void parse(CharSequence paragraph, OnTag onTag);
    String getStartTag(CharacterStyle characterStyle);
    String getEndTag(CharacterStyle characterStyle);

    interface OnTag {
        void onTag(int position, Tag tag);
    }

    interface Tag {
        CharacterStyle getStyle();

        boolean isOpening();

        boolean isClosing();

        int getSize();
    }
}

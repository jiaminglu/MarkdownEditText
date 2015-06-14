package com.jiaminglu.markdownedittext.syntax;

import android.graphics.Typeface;
import android.text.style.CharacterStyle;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jiaminglu on 15/6/13.
 */
public class HtmlSyntax implements Syntax {
    Pattern styleTag = Pattern.compile("<(/?)(em|strong|del|u)>");
    Matcher tagMatcher;

    @Override
    public void parse(CharSequence paragraph, OnTag onTag) {
        tagMatcher = styleTag.matcher(paragraph);
        while (tagMatcher.find()) {
            onTag.onTag(tagMatcher.start(), new Tag(tagMatcher.group()));
        }
    }

    @Override
    public String getStartTag(CharacterStyle characterStyle) {
        if (characterStyle instanceof StyleSpan) {
            if ((((StyleSpan) characterStyle).getStyle() & Typeface.BOLD_ITALIC) == Typeface.BOLD_ITALIC) {
                return "<strong><em>";
            }
            if ((((StyleSpan) characterStyle).getStyle() & Typeface.BOLD) != 0) {
                return "<strong>";
            }
            if ((((StyleSpan) characterStyle).getStyle() & Typeface.ITALIC) != 0) {
                return "<em>";
            }
        }
        if (characterStyle instanceof StrikethroughSpan)
            return "<strike>";
        if (characterStyle instanceof UnderlineSpan)
            return "<u>";
        return "";
    }

    @Override
    public String getEndTag(CharacterStyle characterStyle) {
        if (characterStyle instanceof StyleSpan) {
            if ((((StyleSpan) characterStyle).getStyle() & Typeface.BOLD_ITALIC) == Typeface.BOLD_ITALIC) {
                return "</em></strong>";
            }
            if ((((StyleSpan) characterStyle).getStyle() & Typeface.BOLD) != 0) {
                return "</strong>";
            }
            if ((((StyleSpan) characterStyle).getStyle() & Typeface.ITALIC) != 0) {
                return "</em>";
            }
        }
        if (characterStyle instanceof StrikethroughSpan)
            return "</strike>";
        if (characterStyle instanceof UnderlineSpan)
            return "</u>";
        return "";
    }

    public static class Tag implements Syntax.Tag {
        String tag;

        public Tag(String tag) {
            this.tag = tag;
        }

        @Override
        public CharacterStyle getStyle() {
            String name = tag.substring(isOpening() ? 1 : 2, tag.length() - 1);
            if (name.equals("em"))
                return new StyleSpan(Typeface.ITALIC);
            if (name.equals("strong"))
                return new StyleSpan(Typeface.BOLD);
            if (name.equals("strike"))
                return new StrikethroughSpan();
            if (name.equals("u"))
                return new UnderlineSpan();
            return null;
        }

        @Override
        public boolean isOpening() {
            return tag.charAt(1) != '/';
        }

        @Override
        public boolean isClosing() {
            return tag.charAt(1) == '/';
        }

        @Override
        public int getSize() {
            return tag.length();
        }
    }
}

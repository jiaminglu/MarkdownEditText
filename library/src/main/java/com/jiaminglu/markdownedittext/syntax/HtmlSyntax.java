package com.jiaminglu.markdownedittext.syntax;

import com.jiaminglu.markdownedittext.style.BoldStyle;
import com.jiaminglu.markdownedittext.style.ItalicStyle;
import com.jiaminglu.markdownedittext.style.StrikethroughStyle;
import com.jiaminglu.markdownedittext.style.Style;
import com.jiaminglu.markdownedittext.style.UnderlineStyle;

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
    public String getStartTag(Style style) {
        if (style instanceof BoldStyle)
            return "<strong>";
        if (style instanceof ItalicStyle)
            return "<em>";
        if (style instanceof StrikethroughStyle)
            return "<del>";
        if (style instanceof UnderlineStyle)
            return "<u>";
        return null;
    }

    @Override
    public String getEndTag(Style style) {
        if (style instanceof BoldStyle)
            return "</strong>";
        if (style instanceof ItalicStyle)
            return "</em>";
        if (style instanceof StrikethroughStyle)
            return "</del>";
        if (style instanceof UnderlineStyle)
            return "</u>";
        return null;
    }

    public static class Tag implements Syntax.Tag {
        String tag;

        public Tag(String tag) {
            this.tag = tag;
        }

        @Override
        public Style getStyle() {
            String name = tag.substring(isOpening() ? 1 : 2, tag.length() - 1);
            if (name.equals("em"))
                return new ItalicStyle();
            if (name.equals("strong"))
                return new BoldStyle();
            if (name.equals("del"))
                return new StrikethroughStyle();
            if (name.equals("u"))
                return new UnderlineStyle();
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

package com.jiaminglu.markdownedittext.style.html;

import com.jiaminglu.markdownedittext.style.Style;
import com.jiaminglu.markdownedittext.style.StyleParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jiaminglu on 15/6/13.
 */
public class HtmlStyleParser implements StyleParser {
    Pattern styleTag = Pattern.compile("<(/?)(em|strong|del|u)>");
    Matcher tagMatcher;

    @Override
    public void parse(CharSequence paragraph, OnTag onTag) {
        tagMatcher = styleTag.matcher(paragraph);
        while (tagMatcher.find()) {
            onTag.onTag(tagMatcher.start(), new Tag(tagMatcher.group()));
        }
    }

    public static class Tag implements StyleParser.Tag {
        String tag;

        public Tag(String tag) {
            this.tag = tag;
        }

        @Override
        public Style getStyle() {
            String name = tag.substring(isOpening() ? 1 : 2, tag.length() - 1);
            if (name.equals("em"))
                return new HtmlItalicStyle();
            if (name.equals("strong"))
                return new HtmlBoldStyle();
            if (name.equals("del"))
                return new HtmlStrikethroughStyle();
            if (name.equals("u"))
                return new HtmlUnderlineStyle();
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

package com.jiaminglu.markdownedittext;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.jiaminglu.markdownedittext.syntax.Syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

/**
 * Created by jiaminglu on 15/6/2.
 */
public class MarkdownEditText extends EditText {
    public MarkdownEditText(Context context) {
        super(context);
        init();
    }

    public MarkdownEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyXmlAttrs(attrs, 0, 0);
        init();
    }

    public MarkdownEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyXmlAttrs(attrs, defStyleAttr, 0);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MarkdownEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        applyXmlAttrs(attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void applyXmlAttrs(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MarkdownEditText, defStyleAttr, defStyleRes);

        Drawable checkboxDrawable = a.getDrawable(R.styleable.MarkdownEditText_checkboxDrawable);
        setCheckbox(checkboxDrawable != null ? checkboxDrawable : getContext().getResources().getDrawable(R.drawable.ic_checkbox_blank_outline_black_18dp));

        Drawable checkboxCheckedDrawable = a.getDrawable(R.styleable.MarkdownEditText_checkboxCheckedDrawable);
        setCheckboxChecked(checkboxCheckedDrawable != null ? checkboxCheckedDrawable : getContext().getResources().getDrawable(R.drawable.ic_checkbox_marked_black_18dp));

        String markdown = a.getString(R.styleable.MarkdownEditText_markdown);
        if (!TextUtils.isEmpty(markdown))
            setMarkdown(markdown);
        if (a.getBoolean(R.styleable.MarkdownEditText_viewMode, false))
            enterViewMode();

        imageOffset = (int) a.getDimension(R.styleable.MarkdownEditText_lineSpacingExtra, 0);
        bulletSize = a.getDimension(R.styleable.MarkdownEditText_bulletSize, getTextSize() / 8);
        itemPaddingStart = (int) a.getDimension(R.styleable.MarkdownEditText_itemPaddingStart, getTextSize() / 2);
        tabSize = (int) a.getDimension(R.styleable.MarkdownEditText_tabSize, getTextSize());
        initBulletShape();

        a.recycle();
    }

    private float bulletSize;
    private int itemPaddingStart;
    private int imageOffset;
    private int tabSize;

    @Override
    public void setLineSpacing(float a, float b) {
        super.setLineSpacing(a,b);
        imageOffset = (int) a;
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {
        boolean changed = false;
        if (selStart > 2 && selStart <= length() && getText().charAt(selStart - 2) == '\n' && getText().charAt(selStart - 1) == ' ' && (selStart == length() || getText().charAt(selStart) == '\n')) {
            selStart --;
            changed = true;
        } else {
            LinePrefixImageSpan[] spans = getText().getSpans(selStart, selStart, LinePrefixImageSpan.class);
            if (spans.length > 0) {
                int end = getText().getSpanEnd(spans[0]);
                if (selStart != end) {
                    selStart = end;
                    changed = true;
                }
            } else if (selStart > 0 && selStart < length() && getText().charAt(selStart) == ' ' && prevWordIsNumber(selStart) != -1) {
                selStart --;
                changed = true;
            }

        }
        if (selEnd > 2 && selEnd <= length() && getText().charAt(selEnd - 2) == '\n' && getText().charAt(selEnd - 1) == ' ' && (selEnd == length() || getText().charAt(selEnd) == '\n')) {
            selEnd --;
            changed = true;
        } else {
            LinePrefixImageSpan[] spans = getText().getSpans(selEnd, selEnd, LinePrefixImageSpan.class);
            if (spans.length > 0) {
                int end = getText().getSpanEnd(spans[0]);
                if (selEnd != end) {
                    selEnd = end;
                    changed = true;
                }
            } else if (selEnd > 0 && selEnd < length() && getText().charAt(selEnd) == ' ' && prevWordIsNumber(selEnd) != -1) {
                selEnd --;
                changed = true;
            }
        }
        if (!changed)
            super.onSelectionChanged(selStart, selEnd);
        else
            setSelection(selStart, selEnd);
    }

    private boolean viewSource = false;

    public void setFormatterDisabled(boolean formatterDisabled) {
        this.formatterDisabled = formatterDisabled;
    }

    private boolean formatterDisabled = false;

    private class RemoveSpan {
    }

    private class InsertSpan {
        Spannable toBeInserted;

        public InsertSpan(Spannable toBeInserted) {
            this.toBeInserted = toBeInserted;
        }
    }

    private int prevWordIsNumber(int start) {
        if (start <= 0 || start > getText().length() || getText().charAt(start - 1) != '.')
            return -1;
        start--;
        while (start > 0 && Character.isDigit(getText().charAt(start - 1)))
            start--;
        if (start == 0 || getText().charAt(start - 1) == '\n')
            return start;
        return -1;
    }

    private String bulletMarkdown = "* ";
    private String checkboxMarkdown = "- [ ] ";
    private String checkboxCheckedMarkdown = "- [x] ";
    Pattern linePrefixPattern = Pattern.compile(String.format("^(%s|%s|%s|(\\d+)\\. )",
            Pattern.quote(checkboxMarkdown), Pattern.quote(checkboxCheckedMarkdown), Pattern.quote(bulletMarkdown)));
    Pattern checkboxParagraphPattern = Pattern.compile(String.format("(?m)^(%s|%s).*$",
            Pattern.quote(checkboxMarkdown), Pattern.quote(checkboxCheckedMarkdown)));
    Pattern imagePattern = Pattern.compile("(?m)!\\[(.*?)\\]\\((.*?)\\)");
    Pattern tabPattern = Pattern.compile("(?m)^([\\t ]*)(.*)$");

    TextWatcher watcher;
    private void init() {
        addTextChangedListener(watcher = new TextFormatter());
        initRichText();
        setLinksClickable(true);
        setMovementMethod(ClickableArrowKeyMovementMethod.getInstance());
    }

    private Spannable getBulletSpannable() {
        SpannableString spannableString = new SpannableString(bulletMarkdown);
        spannableString.setSpan(getBulletImageSpan(), 0, bulletMarkdown.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private Spannable getCheckboxSpannable() {
        SpannableString spannableString = new SpannableString(checkboxMarkdown);
        spannableString.setSpan(getCheckboxImageSpan(), 0, checkboxMarkdown.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        setCheckboxClickable(spannableString, 0, checkboxMarkdown.length(), false);
        return spannableString;
    }

    private Spannable getCheckboxCheckedSpannable() {
        SpannableString spannableString = new SpannableString(checkboxCheckedMarkdown);
        spannableString.setSpan(getCheckboxCheckedImageSpan(), 0, checkboxCheckedMarkdown.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        setCheckboxClickable(spannableString, 0, checkboxCheckedMarkdown.length(), true);
        return spannableString;
    }

    private void toggleStyleSpan(CharacterStyle newSpan, int start, int end) {
        if (start < 0 || end > getText().length())
            return;
        CharacterStyle[] spans = getText().getSpans(start - 1, end + 1, CharacterStyle.class);
        if (spans.length == 1)  {
            int oldstart = getText().getSpanStart(spans[0]);
            int oldend = getText().getSpanEnd(spans[0]);
            if (oldstart <= start && oldend >= end) {
                getText().removeSpan(spans[0]);
                if (oldstart < start)
                    getText().setSpan(spans[0], oldstart, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                if (end < oldend)
                    getText().setSpan(newSpan, end, oldend, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                return;
            }
        }
        for (CharacterStyle span : spans) {
            start = Math.min(start, getText().getSpanStart(span));
            end = Math.max(end, getText().getSpanEnd(span));
            getText().removeSpan(span);
        }
        getText().setSpan(newSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
    }

    public void toggleStyleSpan(CharacterStyle newSpan) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        while (start < end) {
            if (start == 0 || getText().charAt(start - 1) == '\n') {
                Matcher matcher = linePrefixPattern.matcher(getText().subSequence(start, getText().length()));
                if (matcher.find()) {
                    start += matcher.end();
                }
            }
            if (start >= end)
                break;
            int nl = start;
            while (nl < end && getText().charAt(nl) != '\n')
                nl++;
            if (start != nl)
                toggleStyleSpan(copy(newSpan), start, nl);
            start = nl + 1;
        }
    }

    private static CharacterStyle copy(CharacterStyle style) {
        if (style instanceof StyleSpan)
            return new StyleSpan(((StyleSpan) style).getStyle());
        if (style instanceof UnderlineSpan)
            return new UnderlineSpan();
        if (style instanceof StrikethroughSpan)
            return new StrikethroughSpan();
        return null;
    }

    public interface LineOperation {
        void operateOn(int lineStart);
    }
    public void operationOnLines(LineOperation lineOperation) {
        int start = getSelectionStart();
        while (start > 0 && getText().charAt(start - 1) != '\n') {
            start --;
        }
        while (true) {
            lineOperation.operateOn(start);
            start ++;
            while (start < getText().length() && (getText().charAt(start - 1) != '\n'))
                start ++;
            if (start >= getSelectionEnd())
                break;
        }
    }

    private class TabSpan extends LeadingMarginSpan.Standard {
        public TabSpan() {
            super(tabSize);
        }
    }

    public void indentIncrease() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                int end = lineStart;
                while (end < getText().length() && ((end == lineStart && getText().charAt(lineStart) != '\n') || getText().charAt(end) != '\n'))
                    end ++;
                getText().setSpan(new TabSpan(), lineStart, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                if ((getNumberingAtLine(lineStart)) != 0) {
                    setLineNumbered();
                }
            }
        });
    }

    public void indentDecrease() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                int end = lineStart;
                while (end < getText().length() && (getText().charAt(end) != '\n'))
                    end ++;
                TabSpan[] spans = getText().getSpans(lineStart, end, TabSpan.class);
                if (spans.length > 0)
                    getText().removeSpan(spans[0]);
                if ((getNumberingAtLine(lineStart)) != 0) {
                    setLineNumbered();
                }
            }
        });
    }

    public void setCheckbox(Drawable checkbox) {
        checkbox.setBounds(0, 0,
                checkbox.getIntrinsicWidth(),
                checkbox.getIntrinsicHeight());
        this.checkbox = checkbox;
    }

    public void setCheckboxChecked(Drawable checkboxChecked) {
        checkboxChecked.setBounds(0, 0,
                checkboxChecked.getIntrinsicWidth(),
                checkboxChecked.getIntrinsicHeight());
        this.checkboxChecked = checkboxChecked;
    }

    private Drawable checkbox;
    private Drawable checkboxChecked;

    public void removeLinePrefixes(int lineStart) {
        for (LinePrefixImageSpan span : getText().getSpans(lineStart, lineStart, LinePrefixImageSpan.class)) {
            getText().removeSpan(span);
        }
        for (LinkSpan span : getText().getSpans(lineStart, lineStart, LinkSpan.class)) {
            getText().removeSpan(span);
        }
        Matcher matcher = linePrefixPattern.matcher(getText().subSequence(lineStart, getText().length()));
        if (matcher.find()) {
            getText().delete(lineStart, lineStart + matcher.end());
            return;
        }
    }

    private ShapeDrawable bullet;
    private void initBulletShape() {
        bullet = new ShapeDrawable(new DotShape(4));
        bullet.getPaint().setColor(Color.BLACK);
        ((DotShape)bullet.getShape()).setRadius(bulletSize);
        bullet.getShape().resize(bulletSize * 4, bulletSize * 2);
        bullet.setBounds(0, 0, (int) (bulletSize * 4), (int) (bulletSize * 4));
    }

    private class LinePrefixImageSpan extends CenteredImageSpan {
        public LinePrefixImageSpan(Drawable b) {
            super(b);
        }

        public LinePrefixImageSpan(int resourceId) {
            super(getContext(), resourceId);
        }
    }

    private LinePrefixImageSpan getBulletImageSpan() {
        LinePrefixImageSpan span = new LinePrefixImageSpan(bullet);
        span.setSpacing(itemPaddingStart);
        span.setOffset(imageOffset);
        return span;
    }

    private LinePrefixImageSpan getCheckboxImageSpan() {
        LinePrefixImageSpan span = new LinePrefixImageSpan(checkbox);
        span.setSpacing(itemPaddingStart);
        span.setOffset(imageOffset);
        return span;
    }

    private LinePrefixImageSpan getCheckboxCheckedImageSpan() {
        LinePrefixImageSpan span = new LinePrefixImageSpan(checkboxChecked);
        span.setSpacing(itemPaddingStart);
        span.setOffset(imageOffset);
        return span;
    }

    class MarginSpan extends LeadingMarginSpan.Standard {
        public MarginSpan(int rest) {
            super(0, rest);
        }
    }

    private void setMargin(int lineStart, Drawable drawable) {
        for (MarginSpan span : getText().getSpans(lineStart, lineStart, MarginSpan.class)) {
            getText().removeSpan(span);
        }
        int margin = drawable.getBounds().right + itemPaddingStart;
        int end = lineStart;
        while (end < getText().length() && ((end == lineStart && getText().charAt(lineStart) != '\n') || getText().charAt(end) != '\n'))
            end ++;
        getText().setSpan(new MarginSpan(margin), lineStart, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    public void setLineBulleted() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                getText().insert(lineStart, getBulletSpannable());
                setMargin(lineStart, bullet);
            }
        });
    }

    private int getNumberingAtLine(int lineStart) {
        int numEnd = lineStart;
        while (numEnd >= 0 && numEnd < getText().length() && Character.isDigit(getText().charAt(numEnd))) {
            numEnd ++;
        }
        if (numEnd >= 0 && numEnd < getText().length() && getText().charAt(numEnd) == '.') {
            try {
                return Integer.parseInt(getText().subSequence(lineStart, numEnd).toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public void setLineNumbered() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                int prevLineStart = lineStart - 1;
                int indentLevel = getText().getSpans(lineStart, lineStart, TabSpan.class).length;
                int number = 1;
                while (true) {
                    while (prevLineStart > 0 && getText().charAt(prevLineStart - 1) != '\n')
                        prevLineStart--;
                    if (prevLineStart < 0)
                        break;
                    int prevIndentLevel = getText().getSpans(prevLineStart, prevLineStart, TabSpan.class).length;
                    if (prevIndentLevel < indentLevel)
                        break;
                    if (prevIndentLevel == indentLevel) {
                        number += getNumberingAtLine(prevLineStart);
                        break;
                    }
                    prevLineStart--;
                }
                String numbering = String.valueOf(number) + ". ";
                getText().insert(lineStart, numbering);
            }
        });
    }

    public void setLineCheckbox() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                getText().insert(lineStart, getCheckboxSpannable());
                setMargin(lineStart, checkbox);
            }
        });
    }

    public void setLineCheckboxChecked() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                getText().insert(lineStart, getCheckboxCheckedSpannable());
                setMargin(lineStart, checkboxChecked);
            }
        });
    }

    private class SpanTag {
        int position;
        Object tag;
        int type;

        static final int TYPE_PARAGRAPH = 0;
        static final int TYPE_CLOSING = 1;
        static final int TYPE_OPENING = 2;

        public SpanTag(int position, Object tag, int type) {
            this.position = position;
            this.tag = tag;
            this.type = type;
        }

        @Override
        public String toString() {
            if (type == TYPE_OPENING) {
                return syntax.getStartTag((CharacterStyle) tag);
            }
            if (type == TYPE_CLOSING) {
                return syntax.getEndTag((CharacterStyle) tag);
            }
            return tag.toString();
        }
    }

    public CharSequence getMarkdown() {
        CharacterStyle[] spans = getText().getSpans(0, length(), CharacterStyle.class);
        TabSpan[] tabs = getText().getSpans(0, length(), TabSpan.class);

        ArrayList<SpanTag> tags = new ArrayList<>(spans.length * 2 + tabs.length);

        for (TabSpan span : tabs) {
            tags.add(new SpanTag(getText().getSpanStart(span), "\t", SpanTag.TYPE_PARAGRAPH));
        }
        if (syntax != null) {
            for (CharacterStyle span : spans) {
                int start = getText().getSpanStart(span);
                int end = getText().getSpanEnd(span);
                if (start == end)
                    continue;
                tags.add(new SpanTag(start, span, SpanTag.TYPE_OPENING));
                tags.add(new SpanTag(end, span, SpanTag.TYPE_CLOSING));
            }
        }

        Collections.sort(tags, new Comparator<SpanTag>() {
            @Override
            public int compare(SpanTag lhs, SpanTag rhs) {
                int result = lhs.position < rhs.position ? -1 : lhs.position > rhs.position ? 1
                        : lhs.type < rhs.type ? -1 : lhs.type > rhs.type ? 1 : 0;
                if (result != 0)
                    return result;
                if (lhs.type == SpanTag.TYPE_OPENING)
                    return getText().getSpanEnd(lhs.tag) > getText().getSpanEnd(rhs.tag) ? -1 : getText().getSpanEnd(lhs.tag) < getText().getSpanEnd(rhs.tag) ? 1
                            : syntax.getStartTag((CharacterStyle) lhs.tag).compareTo(syntax.getStartTag((CharacterStyle) rhs.tag));
                if (lhs.type == SpanTag.TYPE_CLOSING)
                    return getText().getSpanStart(lhs.tag) > getText().getSpanStart(rhs.tag) ? -1 : getText().getSpanStart(lhs.tag) < getText().getSpanStart(rhs.tag) ? 1
                            : -syntax.getStartTag((CharacterStyle) lhs.tag).compareTo(syntax.getStartTag((CharacterStyle) rhs.tag));
                return 0;
            }
        });

        StringBuilder builder = new StringBuilder();

        Stack<Object> tagStack = new Stack<>();
        int start = 0;
        for (int i = 0; i < tags.size(); i ++) {
            if (tags.get(i).position > start)
                builder.append(getText().subSequence(start, tags.get(i).position));
            if (tags.get(i).type >= SpanTag.TYPE_CLOSING) {
                if (tags.get(i).type == SpanTag.TYPE_CLOSING) {
                    Stack<Object> closingStack = new Stack<>();
                    while (!tagStack.empty() && tags.get(i).tag != tagStack.peek()) {
                        builder.append(syntax.getEndTag((CharacterStyle)tagStack.peek()));
                        closingStack.push(tagStack.peek());
                        tagStack.pop();
                    }
                    builder.append(syntax.getEndTag((CharacterStyle) tagStack.peek()));
                    tagStack.pop();
                    while (!closingStack.empty()) {
                        builder.append(syntax.getStartTag((CharacterStyle) closingStack.peek()));
                        tagStack.push(closingStack.peek());
                        closingStack.pop();
                    }
                    start = tags.get(i).position;
                } else {
                    tagStack.push(tags.get(i).tag);
                    builder.append(tags.get(i).toString());
                    start = tags.get(i).position;
                }
            } else {
                builder.append(tags.get(i).toString());
                start = tags.get(i).position;
            }
        }

        if (start < length())
            builder.append(getText().subSequence(start, length()));

        StringBuffer result = new StringBuffer();

        Pattern emptyLines = Pattern.compile("(?m) +$");
        Matcher matcher = emptyLines.matcher(builder);
        while (matcher.find()) {
            matcher.appendReplacement(result, "");
        }
        matcher.appendTail(result);

        return result;
    }

    public void setMarkdown(CharSequence markdown) {
        setText("");
        setText(convertToRichText(markdown));
        setMovementMethod(ClickableArrowKeyMovementMethod.getInstance());
    }

    private class SpanPosition {
        int from;
        int to;
        Object span;
        int type;

        public SpanPosition(int from, int to, Object span, int type) {
            this.from = from;
            this.to = to;
            this.span = span;
            this.type = type;
        }
    }

    static class TabParser {
        public int parse(String tabs) {
            return tabs.length();
        }
    }

    TabParser tabParser = new TabParser();

    public Syntax getSyntax() {
        return syntax;
    }

    public void setSyntax(Syntax syntax) {
        this.syntax = syntax;
    }

    private Syntax syntax;

    private class Span {
        int position;
        Object tag;

        public Span(int position, Object tag) {
            this.position = position;
            this.tag = tag;
        }
    }

    // must be used inside setText()
    private Spannable convertToRichText(CharSequence string) {
        StringBuffer result = new StringBuffer();
        Pattern addLeadingSpace = Pattern.compile("(?m)\\t*(- \\[(x| )]|\\*|\\d+\\.)(?! )");
        Matcher addSpaceMatcher = addLeadingSpace.matcher(string);
        while (addSpaceMatcher.find()) {
            addSpaceMatcher.appendReplacement(result, addSpaceMatcher.group());
            result.append(' ');
        }
        addSpaceMatcher.appendTail(result);

        StringBuilder output = new StringBuilder();

        final Matcher matcher = tabPattern.matcher(result);
        final ArrayList<SpanPosition> spans = new ArrayList<>();
        int charDiff = 0;
        int lastEnd = 0;
        while (matcher.find()) {

            final CharSequence paragraph = matcher.group(2);
            CharSequence outParagraph;

            final int[] charDiffInParagraph = {matcher.group(1).length()};

            if (syntax != null) {
                final int paragraphStart = matcher.start() - charDiff;
                final StringBuilder paragraphBuilder = new StringBuilder();

                final int[] outLast = {0};
                syntax.parse(paragraph, new Syntax.OnTag() {
                    Stack<Span> tagStack = new Stack<>();
                    int tabs = matcher.group(1).length();

                    @Override
                    public void onTag(int position, Syntax.Tag tag) {
                        if (tag.isOpening()) {
                            tagStack.push(new Span(paragraphStart + position + tabs - charDiffInParagraph[0], tag.getStyle()));
                        } else {
                            while (!tagStack.empty()) {
                                spans.add(new SpanPosition(tagStack.peek().position, paragraphStart + position + tabs - charDiffInParagraph[0], tagStack.peek().tag, Spannable.SPAN_INCLUSIVE_INCLUSIVE));
                                if (tagStack.peek().tag.equals(tag.getStyle())) {
                                    tagStack.pop();
                                    break;
                                }
                                tagStack.pop();
                            }
                        }
                        charDiffInParagraph[0] += tag.getSize();
                        paragraphBuilder.append(paragraph.subSequence(outLast[0], position));
                        outLast[0] = position + tag.getSize();
                    }
                });
                paragraphBuilder.append(paragraph.subSequence(outLast[0], paragraph.length()));
                outParagraph = paragraphBuilder;
            } else {
                outParagraph = paragraph;
            }

            if (outParagraph.length() == 0) {
                outParagraph = " ";
                charDiffInParagraph[0] --;
            }

            int tabs = tabParser.parse(matcher.group(1));

            for (int i = 0; i < tabs; i++)
                spans.add(new SpanPosition(matcher.start() - charDiff, matcher.end() - charDiff - charDiffInParagraph[0], new TabSpan(), Spanned.SPAN_INCLUSIVE_INCLUSIVE));

            charDiff += charDiffInParagraph[0];
            output.append(result.subSequence(lastEnd, matcher.start()));
            output.append(outParagraph);
            lastEnd = matcher.end();
        }
        output.append(result.subSequence(lastEnd, result.length()));

        SpannableString s = new SpannableString(output);
        for (SpanPosition spanPosition : spans) {
            s.setSpan(spanPosition.span, spanPosition.from, spanPosition.to, spanPosition.type);
        }

        return s;
    }

    boolean firstTimeSetText = true;
    void initRichText() {
        firstTimeSetText = false;
        int i = 0;
        while (i < getText().length()) {
            Matcher matcher = linePrefixPattern.matcher(getText().subSequence(i, getText().length()));
            if (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String str = matcher.group();
                Object span;
                if (str.equals(bulletMarkdown)) {
                    span = getBulletImageSpan();
                    setMargin(i + start, bullet);
                } else if (str.equals(checkboxMarkdown)) {
                    span = getCheckboxImageSpan();
                    setMargin(i + start, checkbox);
                } else if (str.equals(checkboxCheckedMarkdown)) {
                    span = getCheckboxCheckedImageSpan();
                    setMargin(i + start, checkboxChecked);
                } else {
                    continue;
                }
                getText().setSpan(span, i + start, i + end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                setCheckboxClickable(getText(), i + start, i + end, false);
            }
            while (i < getText().length() && getText().charAt(i) != '\n')
                i++;
            i++;
        }

        if (imageHandler != null) {
            Matcher imageMacher = imagePattern.matcher(getText());
            while (imageMacher.find()) {
                int start = imageMacher.start();
                int end = imageMacher.end();
                final InlineImage span = setImageThumbnail(getText(), start, end, bullet);
                setImageLink(getText(), start, end, span);
                imageHandler.fetch(span, imageMacher.group(2));
            }
        }
    }

    public interface ImageHandler {
        void fetch(InlineImage image, String uri);
        void onClick(InlineImage image, String uri);
    }

    public void setImageHandler(ImageHandler imageHandler) {
        this.imageHandler = imageHandler;
    }

    ImageHandler imageHandler;

    private abstract class LinkSpan extends ClickableSpan {
        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
        }
    }

    public interface CheckboxClickListener {
        void onCheckboxClicked(int checkboxOffset, boolean checked);
    }

    private CheckboxClickListener checkboxClickListener;

    public void setCheckboxClickListener(CheckboxClickListener checkboxClickListener) {
        this.checkboxClickListener = checkboxClickListener;
    }

    private void setCheckboxClickable(Spannable spannable, final int start, final int end, final boolean checked) {
        spannable.setSpan(new LinkSpan() {
            @Override
            public void onClick(View widget) {
                int spanStart = getText().getSpanStart(this);
                int spanEnd = getText().getSpanEnd(this);
                if (!checked)
                    getText().replace(spanStart, spanStart + checkboxMarkdown.length(), getCheckboxCheckedSpannable());
                else
                    getText().replace(spanStart, spanStart + checkboxCheckedMarkdown.length(), getCheckboxSpannable());
                if (checkboxClickListener != null)
                    checkboxClickListener.onCheckboxClicked(spanStart, !checked);
                getText().removeSpan(this);
                setCheckboxClickable(getText(), spanStart, spanEnd, !checked);
            }
        }, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    public void enterViewMode() {
        if (viewSource) {
            viewSource = false;
            setMarkdown(getText().toString());
        }
        setFocusable(false);
        setLinksClickable(true);
        setCursorVisible(false);

        Matcher matcher = checkboxParagraphPattern.matcher(getText());
        while (matcher.find()) {
            int linestart = matcher.start();
            int lineend = matcher.end();
            setCheckboxClickable(getText(), linestart, lineend, getText().subSequence(linestart, linestart + checkboxCheckedMarkdown.length()).equals(checkboxCheckedMarkdown));
        }
    }

    public void exitViewMode() {
        CharSequence md = getMarkdown();
        viewSource = false;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setLinksClickable(false);
        setCursorVisible(true);
        setMarkdown(md);
    }

    public void toggleViewSource() {
        if (viewSource) {
            viewSource = false;
            setFocusable(true);
            setFocusableInTouchMode(true);
            setMarkdown(getText());
        } else {
            viewSource = true;
            setFocusable(false);
            setText(getMarkdown());
        }
    }

    private InlineImage setImageThumbnail(Spannable spannable, int st, int en, Drawable image) {
        InlineImage span = new InlineImage(this, image);
        spannable.setSpan(span, st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }

    private void setImageLink(final Spannable spannable, int st, int en, final InlineImage image) {
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                if (imageHandler != null) {
                    Matcher matcher = imagePattern.matcher(getText().subSequence(getText().getSpanStart(image), getText().getSpanEnd(image)));
                    if (matcher.matches()) {
                        imageHandler.onClick(image, matcher.group(2));
                    }
                }
            }
        }, st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public InlineImage insertImage(int position, String alt, String uri) {
        formatterDisabled = true;
        SpannableString string = new SpannableString(String.format("![%s](%s)", alt, uri));
        InlineImage image = setImageThumbnail(string, 0, string.length(), bullet);
        setImageLink(string, 0, string.length(), image);
        getText().insert(position, string);
        imageHandler.fetch(image, uri);
        formatterDisabled = false;
        return image;
    }

    private class TextFormatter implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int ostart, int count, int after) {
            if (viewSource)
                return;
            if (s.length() == 0)
                firstTimeSetText = true;
            int start = ostart;
            while (count > 0) {
                if (s.charAt(start) == '\n' && start + 1 <= getText().length()) {
                    if (start > 0 && s.charAt(start - 1) == ' ')
                        remove(start - 1, start);
                    for (TabSpan span : getText().getSpans(start + 1, start + count, TabSpan.class))
                        getText().removeSpan(span);
                    for (MarginSpan span : getText().getSpans(start + 1, start + count, MarginSpan.class))
                        getText().removeSpan(span);
                    break;
                }
                count--;
                start++;
            }
        }

        void insertBefore(int pos, Spannable str) {
            getText().setSpan(new InsertSpan(str), pos, pos, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        void remove(int st, int en) {
            getText().setSpan(new RemoveSpan(), st, en, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (viewSource)
                return;
            if (firstTimeSetText)
                return;
            if (start > 0 && getText().charAt(start - 1) == '\n' && (start == length() || getText().charAt(start) == '\n'))
                insertBefore(start, new SpannableString(" "));
            while (count >= 0 && start <= s.length()) {
                if (start + 1 < getText().length() && getText().charAt(start) != '\n'
                        && getText().charAt(start + 1) == ' '
                        && (start + 2 == length() || getText().charAt(start + 2) == '\n')
                        && (getText().getSpans(start, start, LinePrefixImageSpan.class).length == 0)
                        && (prevWordIsNumber(start + 1) == -1)) {
                    remove(start + 1, start + 2);
                }
                if (start == 0 || getText().charAt(start - 1) == '\n') {
                    Matcher matcher = linePrefixPattern.matcher(getText().subSequence(start, getText().length()));
                    if (matcher.find()) {
                        for (CharacterStyle characterStyle : getText().getSpans(start + matcher.start(), start + matcher.end(), CharacterStyle.class)) {
                            if (characterStyle instanceof InlineImage || characterStyle instanceof LinePrefixImageSpan || characterStyle instanceof ClickableSpan)
                                continue;
                            toggleStyleSpan(copy(characterStyle), start + matcher.start(), start + matcher.end());
                        }
                    }
                }
                if (count > 0 && start < getText().length() && getText().charAt(start) == '\n') {
                    int linestart = start + 1;
                    for (TabSpan span : getText().getSpans(start, start, TabSpan.class)) {
                        int oldStart = getText().getSpanStart(span);
                        int oldEnd = getText().getSpanEnd(span);
                        if (oldStart < start && oldEnd > start) {
                            getText().setSpan(span, oldStart, start, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            getText().setSpan(new TabSpan(), linestart, oldEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        }
                    }
                    for (MarginSpan span : getText().getSpans(start, start, MarginSpan.class)) {
                        int oldStart = getText().getSpanStart(span);
                        int oldEnd = getText().getSpanEnd(span);
                        if (oldStart < start && oldEnd > start) {
                            getText().setSpan(span, oldStart, start, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        }
                    }
                    for (CharacterStyle span : getText().getSpans(start, start, CharacterStyle.class)) {
                        int oldStart = getText().getSpanStart(span);
                        int oldEnd = getText().getSpanEnd(span);
                        if (oldStart < start && oldEnd > start) {
                            getText().setSpan(span, oldStart, start, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            if (oldEnd > linestart) {
                                getText().setSpan(copy(span), linestart, oldEnd, SPAN_INCLUSIVE_INCLUSIVE);
                            }
                        }
                    }

                    if (!formatterDisabled) {
                        Matcher matcher = linePrefixPattern.matcher(getText().subSequence(linestart, getText().length()));

                        if (matcher.find()) {
                            String str = matcher.group();
                            if (str.equals(bulletMarkdown)) {
                                getText().setSpan(getBulletImageSpan(), linestart, linestart + bulletMarkdown.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                setMargin(start + 1, bullet);
                            } else if (str.equals(checkboxMarkdown)) {
                                getText().setSpan(getCheckboxImageSpan(), linestart, linestart + checkboxMarkdown.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                setCheckboxClickable(getText(),linestart, linestart + checkboxMarkdown.length(), false);
                                setMargin(linestart, checkbox);
                            } else if (str.equals(checkboxCheckedMarkdown)) {
                                getText().setSpan(getCheckboxCheckedImageSpan(), linestart, linestart + checkboxCheckedMarkdown.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                setCheckboxClickable(getText(),linestart, linestart + checkboxCheckedMarkdown.length(), true);
                                setMargin(linestart, checkboxChecked);
                            }
                        } else {
                            int prevStart = start - 1;
                            if (!(prevStart >= 0 && getText().charAt(prevStart) == '\n')) {
                                while (prevStart > 0 && getText().charAt(prevStart - 1) != '\n')
                                    prevStart--;
                            }
                            matcher = linePrefixPattern.matcher(getText().subSequence(prevStart, getText().length()));
                            if (matcher.find()) {
                                String str = matcher.group();
                                if (str.equals(bulletMarkdown)) {
                                    insertBefore(linestart, getBulletSpannable());
                                    setMargin(linestart, bullet);
                                } else if (str.equals(checkboxMarkdown) || str.equals(checkboxCheckedMarkdown)) {
                                    insertBefore(linestart, getCheckboxSpannable());
                                    setMargin(linestart, checkbox);
                                } else {
                                    insertBefore(linestart, new SpannableString(String.valueOf(Integer.valueOf(matcher.group(2)) + 1) + ". "));
                                }
                            } else {
                                if (linestart == s.length() || s.charAt(linestart) == '\n')
                                    insertBefore(linestart, new SpannableString(" "));
                            }
                        }
                    } else {
                        if (linestart == s.length() || s.charAt(linestart) == '\n')
                            insertBefore(linestart, new SpannableString(" "));
                    }
                }
                start++;
                before--;
                count--;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (firstTimeSetText) {
                initRichText();
                return;
            }
            for (RemoveSpan span : s.getSpans(0, s.length(), RemoveSpan.class)) {
                int start = s.getSpanStart(span);
                int end = s.getSpanEnd(span);
                s.removeSpan(span);
                if (start >= 0 && end <= s.length())
                    s.delete(start, end);
            }
            for (InsertSpan span : s.getSpans(0, s.length(), InsertSpan.class)) {
                int start = s.getSpanStart(span);
                s.removeSpan(span);
                if (start >= 0 && start <= s.length())
                    s.insert(start, span.toBeInserted);
            }
        }
    }
}

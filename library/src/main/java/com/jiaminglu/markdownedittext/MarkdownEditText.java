package com.jiaminglu.markdownedittext;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
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
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        setCharacterStyleEnabled(a.getBoolean(R.styleable.MarkdownEditText_characterStyleEnabled, false));

        Drawable checkboxDrawable = a.getDrawable(R.styleable.MarkdownEditText_checkboxDrawable);
        setCheckbox(checkboxDrawable != null ? checkboxDrawable : getContext().getResources().getDrawable(R.drawable.ic_checkbox_blank_outline_black_18dp));

        Drawable checkboxCheckedDrawable = a.getDrawable(R.styleable.MarkdownEditText_checkboxCheckedDrawable);
        setCheckboxChecked(checkboxCheckedDrawable != null ? checkboxCheckedDrawable : getContext().getResources().getDrawable(R.drawable.ic_checkbox_marked_black_18dp));

        String markdown = a.getString(R.styleable.MarkdownEditText_markdown);
        if (!TextUtils.isEmpty(markdown))
            setMarkdown(markdown);
        if (a.getBoolean(R.styleable.MarkdownEditText_viewMode, false))
            enterViewMode();
        a.recycle();
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {
        boolean changed = false;
        if (selStart > 2 && selStart <= length() && getText().charAt(selStart - 2) == '\n' && getText().charAt(selStart - 1) == ' ') {
            selStart --;
            changed = true;
        } else {
            LinePrefixImageSpan[] spans = getText().getSpans(selStart, selStart, LinePrefixImageSpan.class);
            if (spans.length > 0) {
                int end = getText().getSpanEnd(spans[0]) + 1;
                if (selStart != end) {
                    selStart = end;
                    changed = true;
                }
            } else if (selStart > 0 && selStart < length() && getText().charAt(selStart) == ' ' && prevWordIsNumber(selStart) != -1) {
                selStart --;
                changed = true;
            }

        }
        if (selEnd > 2 && selEnd <= length() && getText().charAt(selEnd - 2) == '\n' && getText().charAt(selEnd - 1) == ' ') {
            selEnd --;
            changed = true;
        } else {
            LinePrefixImageSpan[] spans = getText().getSpans(selEnd, selEnd, LinePrefixImageSpan.class);
            if (spans.length > 0) {
                int end = getText().getSpanEnd(spans[0]) + 1;
                if (selEnd != end) {
                    selEnd = end;
                    changed = true;
                }
            } else if (selStart > 0 && selStart < length() && getText().charAt(selStart) == ' ' && prevWordIsNumber(selEnd) != -1) {
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

    public void setCharacterStyleEnabled(boolean enableCharacterStyle) {
        this.enableCharacterStyle = enableCharacterStyle;
    }

    private boolean enableCharacterStyle = false;

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

    private boolean prevWordIs(int start, String str) {
        int i = str.length() - 1;
        while (i >= 0 && start > 0) {
            if (getText().charAt(start - 1) == str.charAt(i--))
                start--;
            else
                return false;
        }
        return i < 0 && (start == 0 || getText().charAt(start - 1) == '\n');
    }

    TextWatcher watcher;
    private void init() {
        setLinksClickable(true);
        setMovementMethod(LinkMovementMethod.getInstance());
        addTextChangedListener(watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int ostart, int count, int after) {
                if (viewSource)
                    return;
                int start = ostart;
                while (count > 0) {
                    if (s.charAt(start) == '\n' && start + 1 <= getText().length()) {
                        for (TabSpan span : getText().getSpans(start + 1, start + count, TabSpan.class))
                            getText().removeSpan(span);
                        break;
                    }
                    int numbering;
                    if (s.charAt(start) == ' ') {
                        if (prevWordIs(start, "- [ ]")) {
                            clearLinePrefix(start - 5, ostart);
                        } else if (prevWordIs(start, "- [x]")) {
                            clearLinePrefix(start - 5, ostart);
                        } else if (prevWordIs(start, "*")) {
                            clearLinePrefix(start - 1, ostart);
                        } else if ((numbering = prevWordIsNumber(start)) != -1) {
                            clearLinePrefix(numbering, ostart);
                        }
                    }
                    count--;
                    start++;
                }
            }

            private void clearLinePrefix(int st, int en) {
                if (st >= en)
                    return;
                for (LinePrefixImageSpan span : getText().getSpans(st, st, LinePrefixImageSpan.class)) {
                    getText().removeSpan(span);
                }
                for (LinkSpan span : getText().getSpans(st, st, LinkSpan.class)) {
                    getText().removeSpan(span);
                }
                getText().setSpan(new RemoveSpan(), st, en, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
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
                int numbering;
                if (start > 0 && getText().charAt(start - 1) == '\n' && (start == length() || getText().charAt(start) == '\n'))
                    insertBefore(start, new SpannableString(" "));
                while (count >= 0 && start <= s.length()) {
                    if (start + 1 < getText().length() && getText().charAt(start) != '\n'
                            && getText().charAt(start + 1) == ' '
                            && (start + 2 == length() || getText().charAt(start + 2) == '\n')
                            && (!prevWordIs(start + 1, "*"))
                            && (!prevWordIs(start + 1, "- [ ]"))
                            && (!prevWordIs(start + 1, "- [x]"))
                            && (prevWordIsNumber(start+1) == -1)) {
                        remove(start + 1, start + 2);
                        count--;
                    }
                    if (start < getText().length() && getText().charAt(start) == '\n') {
                        if (start + 1 == s.length() || s.charAt(start + 1) == '\n')
                            insertBefore(start + 1, new SpannableString(" "));
                        for (TabSpan span : getText().getSpans(start, start, TabSpan.class)) {
                            int oldStart = getText().getSpanStart(span);
                            int oldEnd = getText().getSpanEnd(span);
                            if (oldStart < start && oldEnd > start) {
                                getText().removeSpan(span);
                                getText().setSpan(span, oldStart, start, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                getText().setSpan(new TabSpan(), start + 1, oldEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            }
                        }
                        for (CharacterStyle span : getText().getSpans(start, start, CharacterStyle.class)) {
                            if (!(span instanceof PrivateStyleSpan))
                                continue;
                            int oldStart = getText().getSpanStart(span);
                            int oldEnd = getText().getSpanEnd(span);
                            if (oldStart < start && oldEnd > start) {
                                getText().removeSpan(span);
                                getText().setSpan(span, oldStart, start, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                if (oldEnd > start + 1) {
                                    try {
                                        getText().setSpan(span.getClass().newInstance(), start + 1, oldEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                    } catch (InstantiationException e) {
                                        e.printStackTrace();
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        if (start + 7 <= getText().length() && getText().subSequence(start + 1, start + 7).toString().equals("- [ ] ")) {
                            getText().replace(start + 1, start + 7, getCheckboxSpannable());
                        } else if (start + 7 <= getText().length() && getText().subSequence(start + 1, start + 7).toString().equals("- [x] ")) {
                            getText().replace(start + 1, start + 7, getCheckboxCheckedSpannable());
                        } else if (start + 3 <= getText().length() && getText().subSequence(start + 1, start + 3).toString().equals("* ")) {
                            getText().replace(start + 1, start + 3, getBulletSpannable());
                        } else if ((numbering = getNumberingAtLine(start + 1)) != 0) {
                        } else if (count > 0 && start != 0) {
                            int prevStart = start - 1;
                            if (!(prevStart >= 0 && getText().charAt(prevStart) == '\n')) {
                                while (prevStart > 0 && getText().charAt(prevStart - 1) != '\n')
                                    prevStart--;
                            }
                            if (prevStart + 5 <= getText().length()) {
                                String prevLinePrefix = getText().subSequence(prevStart, prevStart + 5).toString();
                                if (prevLinePrefix.startsWith("- [ ]") || prevLinePrefix.startsWith("- [x]")) {
                                    insertBefore(start + 1, getCheckboxSpannable());
                                }
                            }
                            if (prevStart + 1 <= getText().length()) {
                                String prevLinePrefix = getText().subSequence(prevStart, prevStart + 1).toString();
                                if (prevLinePrefix.startsWith("*")) {
                                    insertBefore(start + 1, getBulletSpannable());
                                }
                            }
                            if ((numbering = getNumberingAtLine(prevStart)) != 0) {
                                insertBefore(start + 1, new SpannableString(String.valueOf(numbering + 1) + ". "));
                            }
                        }
                    }
                    start++;
                    before--;
                    count--;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
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
        });
    }

    private Spannable getBulletSpannable() {
        SpannableString spannableString = new SpannableString("* ");
        spannableString.setSpan(getBulletImageSpan(), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private Spannable getCheckboxSpannable() {
        SpannableString spannableString = new SpannableString("- [ ] ");
        spannableString.setSpan(getCheckboxImageSpan(), 0, 5, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        setCheckboxClickable(spannableString, 0, 5, false);
        return spannableString;
    }

    private Spannable getCheckboxCheckedSpannable() {
        SpannableString spannableString = new SpannableString("- [x] ");
        spannableString.setSpan(getCheckboxCheckedImageSpan(), 0, 5, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        setCheckboxClickable(spannableString, 0, 5, true);
        return spannableString;
    }

    private void toggleStyleSpan(CharacterStyle newSpan, int start, int end) {
        if (start < 0 || end > getText().length())
            return;
        CharacterStyle[] spans = getText().getSpans(start - 1, end + 1, newSpan.getClass());
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

    private void toggleStyleSpan(CharacterStyle newSpan) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        while (start < end) {
            int nl = start;
            while (nl < end && getText().charAt(nl) != '\n')
                nl++;
            try {
                if (start != nl)
                    toggleStyleSpan(newSpan.getClass().newInstance(), start, nl);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            start = nl + 1;
        }
    }

    private interface PrivateStyleSpan {}
    private class PrivateBoldSpan extends BoldSpan implements PrivateStyleSpan {}
    private class PrivateItalicSpan extends ItalicSpan implements PrivateStyleSpan {}
    private class PrivateUnderlineSpan extends UnderlineSpan implements PrivateStyleSpan {}
    private class PrivateStrikethroughSpan extends StrikethroughSpan implements PrivateStyleSpan {}

    public void toggleBold() {
        toggleStyleSpan(new PrivateBoldSpan());
    }

    public void toggleItalic() {
        toggleStyleSpan(new PrivateItalicSpan());
    }

    public void toggleUnderline() {
        toggleStyleSpan(new PrivateUnderlineSpan());
    }

    public void toggleStrikethrough() {
        toggleStyleSpan(new PrivateStrikethroughSpan());
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
            super((int) getTextSize());
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

    private void removeLinePrefixes(int lineStart) {
        for (LinePrefixImageSpan span : getText().getSpans(lineStart, lineStart, LinePrefixImageSpan.class)) {
            getText().removeSpan(span);
        }
        for (LinkSpan span : getText().getSpans(lineStart, lineStart, LinkSpan.class)) {
            getText().removeSpan(span);
        }
        if (lineStart + 6 <= getText().length()) {
            String linePrefix = getText().subSequence(lineStart, lineStart + 6).toString();
            if (linePrefix.equals("- [ ] ") || linePrefix.equals("- [x] ")) {
                getText().delete(lineStart, lineStart + 6);
                return;
            }
        }
        if (lineStart + 2 <= getText().length()) {
            String linePrefix = getText().subSequence(lineStart, lineStart + 2).toString();
            if (linePrefix.equals("* ")) {
                getText().delete(lineStart, lineStart + 2);
                return;
            }
        }
        int numbering = getNumberingAtLine(lineStart);
        if (numbering != 0) {
            String numStr = String.valueOf(numbering);
            getText().delete(lineStart, lineStart + numStr.length() + 2);
        }
    }

    private ShapeDrawable bullet;
    {
        bullet = new ShapeDrawable(new DotShape(4));
        bullet.getPaint().setColor(Color.BLACK);
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
        ((DotShape)bullet.getShape()).setRadius(getTextSize() / 8);
        bullet.getShape().resize(getTextSize() / 2, getTextSize() / 4);
        bullet.setBounds(0, 0, (int) (getTextSize() / 2), (int) (getTextSize() / 4));
        LinePrefixImageSpan span = new LinePrefixImageSpan(bullet);
        span.setSpacing((int)getTextSize() / 4);
        return span;
    }

    private LinePrefixImageSpan getCheckboxImageSpan() {
        LinePrefixImageSpan span = new LinePrefixImageSpan(checkbox);
        span.setSpacing((int)getTextSize() / 4);
        return span;
    }

    private LinePrefixImageSpan getCheckboxCheckedImageSpan() {
        LinePrefixImageSpan span = new LinePrefixImageSpan(checkboxChecked);
        span.setSpacing((int) getTextSize() / 4);
        return span;
    }

    public void setLineBulleted() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                getText().insert(lineStart, getBulletSpannable());
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
            }
        });
    }

    public void setLineCheckboxChecked() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                getText().insert(lineStart, getCheckboxCheckedSpannable());
            }
        });
    }

    private class SpanTag {
        int position;
        String tag;
        int type;

        static final int TYPE_PARAGRAPH = 0;
        static final int TYPE_CLOSING = 1;
        static final int TYPE_OPENING = 2;

        public SpanTag(int position, String tag, int type) {
            this.position = position;
            this.tag = tag;
            this.type = type;
        }
    }

    public CharSequence getMarkdown() {
        Object[] spans = getText().getSpans(0, length(), Object.class);

        ArrayList<SpanTag> tags = new ArrayList<>(spans.length * 2);

        for (Object span : spans) {
            int start = getText().getSpanStart(span);
            int end = getText().getSpanEnd(span);
            if (span instanceof TabSpan) {
                tags.add(new SpanTag(start, "\t", SpanTag.TYPE_PARAGRAPH));
            } else if (enableCharacterStyle) {
                if (span instanceof BoldSpan) {
                    tags.add(new SpanTag(start, "<strong>", SpanTag.TYPE_OPENING));
                    tags.add(new SpanTag(end, "</strong>", SpanTag.TYPE_CLOSING));
                } else if (span instanceof ItalicSpan) {
                    tags.add(new SpanTag(start, "<em>", SpanTag.TYPE_OPENING));
                    tags.add(new SpanTag(end, "</em>", SpanTag.TYPE_CLOSING));
                } else if (span instanceof UnderlineSpan) {
                    tags.add(new SpanTag(start, "<u>", SpanTag.TYPE_OPENING));
                    tags.add(new SpanTag(end, "</u>", SpanTag.TYPE_CLOSING));
                } else if (span instanceof StrikethroughSpan) {
                    tags.add(new SpanTag(start, "<del>", SpanTag.TYPE_OPENING));
                    tags.add(new SpanTag(end, "</del>", SpanTag.TYPE_CLOSING));
                }
            }
        }

        Collections.sort(tags, new Comparator<SpanTag>() {
            @Override
            public int compare(SpanTag lhs, SpanTag rhs) {
                return lhs.position < rhs.position ? -1 : lhs.position > rhs.position ? 1
                        : lhs.type < rhs.type ? -1 : lhs.type > rhs.type ? 1 : 0;
            }
        });

        StringBuilder builder = new StringBuilder();

        Stack<String> tagStack = new Stack<>();
        int start = 0;
        for (int i = 0; i < tags.size(); i ++) {
            if (tags.get(i).position > start)
                builder.append(getText().subSequence(start, tags.get(i).position));
            if (tags.get(i).type >= SpanTag.TYPE_CLOSING) {
                if (tags.get(i).type == SpanTag.TYPE_CLOSING) {
                    int n = 0;
                    while (!tags.get(i + n).tag.substring(2).equals(tagStack.peek().substring(1))) {
                        n++;
                    }
                    if (n != 0) {
                        SpanTag tmp = tags.get(i);
                        tags.set(i, tags.get(i + n));
                        tags.set(i + n, tmp);
                    }
                    tagStack.pop();
                } else {
                    tagStack.push(tags.get(i).tag);
                }
            }
            builder.append(tags.get(i).tag);
            start = tags.get(i).position;
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
        removeTextChangedListener(watcher);
        setText(convertToRichText(markdown));
        addTextChangedListener(watcher);
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

    private CharacterStyle getStyleSpan(String tag) {
        if (tag.equals("em"))
            return new ItalicSpan();
        if (tag.equals("strong"))
            return new BoldSpan();
        if (tag.equals("del"))
            return new StrikethroughSpan();
        if (tag.equals("u"))
            return new UnderlineSpan();
        return null;
    }

    private Spannable convertToRichText(CharSequence string) {
        StringBuffer result = new StringBuffer();
        Pattern addLeadingSpace = Pattern.compile("(?m)\\t*(- \\[(x| )]|\\*|\\d+\\.)(?! )");
        Matcher addSpaceMatcher = addLeadingSpace.matcher(string);
        while (addSpaceMatcher.find()) {
            addSpaceMatcher.appendReplacement(result, addSpaceMatcher.group());
            result.append(' ');
        }
        addSpaceMatcher.appendTail(result);

        StringBuffer output = new StringBuffer();

        Pattern tab = Pattern.compile("(?m)^(\\t*)(.*)$");
        Pattern styleTag = Pattern.compile("<(/?)(em|strong|del|u)>");
        Matcher matcher = tab.matcher(result);
        ArrayList<SpanPosition> spans = new ArrayList<>();
        int charDiff = 0;
        while (matcher.find()) {
            int tabs = matcher.group(1).length();

            String paragraph = matcher.group(2);

            int charDiffInParagraph = tabs;

            if (enableCharacterStyle) {
                Stack<SpanTag> tagStack = new Stack<>();
                int paragraphStart = matcher.start() - charDiff;
                StringBuffer paraOut = new StringBuffer();
                Matcher tagMatcher = styleTag.matcher(paragraph);
                while (tagMatcher.find()) {
                    if (tagMatcher.group(1).isEmpty()) {
                        tagStack.push(new SpanTag(paragraphStart + tagMatcher.start() + tabs - charDiffInParagraph, tagMatcher.group(2), 0));
                    } else {
                        while (!tagStack.empty()) {
                            spans.add(new SpanPosition(tagStack.peek().position, paragraphStart + tagMatcher.start() + tabs - charDiffInParagraph, getStyleSpan(tagStack.peek().tag), Spannable.SPAN_INCLUSIVE_INCLUSIVE));
                            if (tagStack.peek().tag.equals(tagMatcher.group(2))) {
                                tagStack.pop();
                                break;
                            }
                            tagStack.pop();
                        }
                    }
                    charDiffInParagraph += tagMatcher.end() - tagMatcher.start();
                    tagMatcher.appendReplacement(paraOut, "");
                }
                tagMatcher.appendTail(paraOut);
                paragraph = paraOut.toString();
            }

            if (paragraph.isEmpty()) {
                paragraph = " ";
                charDiffInParagraph --;
            }

            for (int i = 0; i < tabs; i++)
                spans.add(new SpanPosition(matcher.start() - charDiff, matcher.end() - charDiff - charDiffInParagraph, new TabSpan(), Spanned.SPAN_INCLUSIVE_INCLUSIVE));

            charDiff += charDiffInParagraph;
            matcher.appendReplacement(output, paragraph);
        }
        matcher.appendTail(output);

        SpannableString s = new SpannableString(output);
        for (SpanPosition spanPosition : spans) {
            s.setSpan(spanPosition.span, spanPosition.from, spanPosition.to, spanPosition.type);
        }

        for (int i = 0; i < s.length(); i++) {
            if (i == 0 || s.charAt(i - 1) == '\n') {
                if (i + 5 <= s.length() && s.subSequence(i, i + 5).toString().equals("- [ ]")) {
                    s.setSpan(getCheckboxImageSpan(), i, i+5, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    setCheckboxClickable(s, i, i+5, false);
                } else if (i + 5 <= s.length() && s.subSequence(i, i + 5).toString().equals("- [x]")) {
                    s.setSpan(getCheckboxCheckedImageSpan(), i, i+5, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    setCheckboxClickable(s, i, i+5, true);
                } else if (i + 1 <= s.length() && s.subSequence(i, i + 1).toString().equals("*")) {
                    s.setSpan(getBulletImageSpan(), i, i+1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return s;
    }

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
                    getText().replace(spanStart, spanStart + 6, getCheckboxCheckedSpannable());
                else
                    getText().replace(spanStart, spanStart + 6, getCheckboxSpannable());
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
        setMovementMethod(LinkMovementMethod.getInstance());

        Pattern checkboxes = Pattern.compile("(?m)^- \\[( |x)\\].*$");
        Matcher matcher = checkboxes.matcher(getText());
        while (matcher.find()) {
            int linestart = matcher.start();
            int lineend = matcher.end();
            setCheckboxClickable(getText(), linestart, lineend, getText().charAt(linestart + 3) == 'x');
        }
    }

    public void exitViewMode() {
        if (viewSource) {
            viewSource = false;
            setMarkdown(getText().toString());
        }
        for (LinkSpan span : getText().getSpans(0, length(), LinkSpan.class)) {
            getText().removeSpan(span);
        }
        setFocusable(true);
        setFocusableInTouchMode(true);
        setLinksClickable(false);
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
}

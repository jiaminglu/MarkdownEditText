package com.jiaminglu.markdownedittext;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.DynamicDrawableSpan;
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
        init();
    }

    public MarkdownEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MarkdownEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {
        boolean changed = false;
        if (selStart > 2 && selStart <= length() && getText().charAt(selStart - 2) == '\n' && getText().charAt(selStart - 1) == ' ') {
            selStart = selStart - 1;
            changed = true;
        }
        if (selEnd > 2 && selEnd <= length() && getText().charAt(selEnd - 2) == '\n' && getText().charAt(selEnd - 1) == ' ') {
            selEnd = selEnd - 1;
            changed = true;
        }
        if (!changed)
            super.onSelectionChanged(selStart, selEnd);
        else
            setSelection(selStart, selEnd);
    }

    boolean viewSource = false;

    public void setCharacterStyleEnabled(boolean enableCharacterStyle) {
        this.enableCharacterStyle = enableCharacterStyle;
    }

    boolean enableCharacterStyle = false;

    class RemoveSpan {
    }

    class InsertSpan {
        Spannable toBeInserted;

        public InsertSpan(Spannable toBeInserted) {
            this.toBeInserted = toBeInserted;
        }
    }

    private void init() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (viewSource)
                    return;
                while (count > 0) {
                    if (s.charAt(start) == '\n' && start + 1 <= getText().length()) {
                        for (LeadingMarginSpan span : getText().getSpans(start + 1, start + count, LeadingMarginSpan.class))
                            getText().removeSpan(span);
                        break;
                    }
                    if (s.charAt(start) == ' ') {
                        if (thisLineStartsWith(getText(), start, "[ ]")) {
                            clearLinePrefix(start - 3, start);
                        } else if (thisLineStartsWith(getText(), start, "[x]")) {
                            clearLinePrefix(start - 3, start);
                        } else if (thisLineStartsWith(getText(), start, "*")) {
                            clearLinePrefix(start - 1, start);
                        }
                    }
                    count--;
                    start++;
                }
            }

            private boolean thisLineStartsWith(Spannable spannable, int position, String string) {
                int start = position - string.length();
                return ((start > 0 && spannable.charAt(start - 1) == '\n') || start == 0) && spannable.subSequence(start, position).toString().equals(string);
            }

            private void clearLinePrefix(int st, int en) {
                for (ImageSpan span : getText().getSpans(st, en, ImageSpan.class)) {
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
                while (count >= 0 && start <= s.length()) {
                    if (start + 1 < getText().length() && getText().charAt(start) != '\n'
                            && (! (getText().charAt(start) == '*' && (start == 0 || getText().charAt(start - 1) == '\n')))
                            && (! (getText().charAt(start) == ']' && start >= 2 && (getText().charAt(start - 1) == ' ' || getText().charAt(start - 1) == 'x')
                                   && getText().charAt(start - 2) == '[' && (start == 2 || getText().charAt(start - 2) == '\n')))
                            && getText().charAt(start + 1) == ' '
                            && (start + 2 == length() || getText().charAt(start + 2) == '\n')) {
                        remove(start + 1, start + 2);
                        count--;
                    }
                    if (start < getText().length() && getText().charAt(start) == '\n') {
                        if (start + 1 == s.length() || s.charAt(start + 1) == '\n')
                            insertBefore(start + 1, new SpannableString(" "));
                        for (LeadingMarginSpan span : getText().getSpans(start, start, LeadingMarginSpan.class)) {
                            int oldStart = getText().getSpanStart(span);
                            int oldEnd = getText().getSpanEnd(span);
                            if (oldStart < start && oldEnd > start) {
                                getText().removeSpan(span);
                                getText().setSpan(span, oldStart, start, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                getText().setSpan(new LeadingMarginSpan.Standard((int) getTextSize()), start + 1, oldEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            }
                        }
                        for (CharacterStyle span : getText().getSpans(start, start, CharacterStyle.class)) {
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
                        if (start + 4 <= getText().length() && getText().subSequence(start + 1, start + 4).toString().equals("[ ]")) {
                            getText().setSpan(getCheckboxImageSpan(), start + 1, start + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else if (start + 4 <= getText().length() && getText().subSequence(start + 1, start + 4).toString().equals("[x]")) {
                            getText().setSpan(getCheckboxCheckedImageSpan(), start + 1, start + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else if (start + 2 <= getText().length() && getText().subSequence(start + 1, start + 2).toString().equals("*")) {
                            getText().setSpan(getBulletImageSpan(), start + 1, start + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else if ((numbering = getNumberingAtLine(start + 1)) != 0) {
                        } else if (count > 0 && start != 0) {
                            int prevStart = start - 1;
                            if (!(prevStart >= 0 && getText().charAt(prevStart) == '\n')) {
                                while (prevStart > 0 && getText().charAt(prevStart - 1) != '\n')
                                    prevStart--;
                            }
                            if (prevStart + 3 <= getText().length()) {
                                String prevLinePrefix = getText().subSequence(prevStart, prevStart + 3).toString();
                                if (prevLinePrefix.startsWith("[ ]") || prevLinePrefix.startsWith("[x]")) {
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
                    if (start > 0 && end <= s.length())
                        s.delete(start, end);
                }
                for (InsertSpan span : s.getSpans(0, s.length(), InsertSpan.class)) {
                    int start = s.getSpanStart(span);
                    s.removeSpan(span);
                    if (start >= 0 && start <= s.length()) {
                        s.insert(start, span.toBeInserted);
                    }
                }
            }
        });
    }

    private Spannable getBulletSpannable() {
        SpannableString spannableString = new SpannableString("* ");
        spannableString.setSpan(getBulletImageSpan(), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private Spannable getCheckboxSpannable() {
        SpannableString spannableString = new SpannableString("[ ] ");
        spannableString.setSpan(getCheckboxImageSpan(), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private Spannable getCheckboxCheckedSpannable() {
        SpannableString spannableString = new SpannableString("[x] ");
        spannableString.setSpan(getCheckboxCheckedImageSpan(), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private void toggleStyleSpan(CharacterStyle newSpan, int start, int end) {
        if (start < 0 || end > getText().length())
            return;
        CharacterStyle[] spans = getText().getSpans(start-1, end+1, newSpan.getClass());
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

    public void toggleBold() {
        toggleStyleSpan(new BoldSpan());
    }

    public void toggleItalic() {
        toggleStyleSpan(new ItalicSpan());
    }

    public void toggleUnderline() {
        toggleStyleSpan(new UnderlineSpan());
    }

    public void toggleStrikethrough() {
        toggleStyleSpan(new StrikethroughSpan());
    }

    interface LineOperation {
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

    public void indentIncrease() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                int end = lineStart;
                while (end < getText().length() && ((end == lineStart && getText().charAt(lineStart) != '\n') || getText().charAt(end) != '\n'))
                    end ++;
                getText().setSpan(new LeadingMarginSpan.Standard((int) getTextSize()), lineStart, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
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
                LeadingMarginSpan[] spans = getText().getSpans(lineStart, end, LeadingMarginSpan.class);
                if (spans.length > 0)
                    getText().removeSpan(spans[0]);
            }
        });
    }

    public void setCheckboxRes(int checkboxRes) {
        this.checkboxRes = checkboxRes;
    }

    public void setCheckboxCheckedRes(int checkboxCheckedRes) {
        this.checkboxCheckedRes = checkboxCheckedRes;
    }

    int checkboxRes;
    int checkboxCheckedRes;

    private void removeLinePrefixes(int lineStart) {
        if (lineStart + 4 <= getText().length()) {
            String linePrefix = getText().subSequence(lineStart, lineStart + 4).toString();
            if (linePrefix.equals("[ ] ") || linePrefix.equals("[x] ")) {
                getText().delete(lineStart, lineStart + 4);
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

    ShapeDrawable bullet;
    {
        bullet = new ShapeDrawable(new DotShape(4));
        bullet.getPaint().setColor(Color.BLACK);
    }

    private ImageSpan getBulletImageSpan() {
        ((DotShape)bullet.getShape()).setRadius(getTextSize() / 8);
        bullet.getShape().resize(getTextSize() / 2, getTextSize() / 4);
        bullet.setBounds(0,0, (int) (getTextSize() / 2), (int) (getTextSize() / 4));
        return new CenteredImageSpan(bullet, DynamicDrawableSpan.ALIGN_BASELINE).setSpacing((int)getTextSize() / 4);
    }

    private ImageSpan getCheckboxImageSpan() {
        return new CenteredImageSpan(getContext(), checkboxRes, DynamicDrawableSpan.ALIGN_BASELINE).setSpacing((int)getTextSize() / 4);
    }

    private ImageSpan getCheckboxCheckedImageSpan() {
        return new CenteredImageSpan(getContext(), checkboxCheckedRes, DynamicDrawableSpan.ALIGN_BASELINE).setSpacing((int)getTextSize() / 4);
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
            return Integer.parseInt(getText().subSequence(lineStart, numEnd).toString());
        }
        return 0;
    }

    public void setLineNumbered() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                int prevLineStart = lineStart - 1;
                while (prevLineStart > 0 && getText().charAt(prevLineStart - 1) != '\n')
                    prevLineStart--;
                int number = getNumberingAtLine(prevLineStart) + 1;
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

    class SpanTag {
        int position;
        String tag;

        public SpanTag(int position, String tag) {
            this.position = position;
            this.tag = tag;
        }
    }

    public String convertToMarkdown() {
        Object[] spans = getText().getSpans(0, length(), Object.class);

        ArrayList<SpanTag> tags = new ArrayList<>(spans.length * 2);

        for (Object span : spans) {
            int start = getText().getSpanStart(span);
            int end = getText().getSpanEnd(span);
            if (span instanceof LeadingMarginSpan) {
                tags.add(new SpanTag(start, "\t"));
            } else if (enableCharacterStyle) {
                if (span instanceof BoldSpan) {
                    tags.add(new SpanTag(start, "<strong>"));
                    tags.add(new SpanTag(end, "</strong>"));
                } else if (span instanceof ItalicSpan) {
                    tags.add(new SpanTag(start, "<em>"));
                    tags.add(new SpanTag(end, "</em>"));
                } else if (span instanceof UnderlineSpan) {
                    tags.add(new SpanTag(start, "<u>"));
                    tags.add(new SpanTag(end, "</u>"));
                } else if (span instanceof StrikethroughSpan) {
                    tags.add(new SpanTag(start, "<del>"));
                    tags.add(new SpanTag(end, "</del>"));
                }
            }
        }

        Collections.sort(tags, new Comparator<SpanTag>() {
            @Override
            public int compare(SpanTag lhs, SpanTag rhs) {
                return lhs.position < rhs.position ? -1 : lhs.position == rhs.position ? 0 : 1;
            }
        });

        StringBuilder builder = new StringBuilder();

        Stack<String> tagStack = new Stack<>();
        int start = 0;
        for (int i = 0; i < tags.size(); i ++) {
            if (tags.get(i).position > start)
                builder.append(getText().subSequence(start, tags.get(i).position));
            if (tags.get(i).tag.charAt(0) == '<') {
                if (tags.get(i).tag.charAt(1) == '/') {
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

        return builder.toString();
    }

    class SpanPosition {
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

    public CharacterStyle getStyleSpan(String tag) {
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

    public Spannable convertToRichText(CharSequence string) {
        StringBuffer output = new StringBuffer();

        Pattern tab = Pattern.compile("(?m)^(\\t*)(.*)$");
        Pattern styleTag = Pattern.compile("<(/?)(em|strong|del|u)>");
        Matcher matcher = tab.matcher(string);
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
                        tagStack.push(new SpanTag(paragraphStart + tagMatcher.start() + tabs - charDiffInParagraph, tagMatcher.group(2)));
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

            if (!paragraph.isEmpty()) {
                for (int i = 0; i < tabs; i++)
                    spans.add(new SpanPosition(matcher.start() - charDiff, matcher.end() - charDiff - charDiffInParagraph, new LeadingMarginSpan.Standard((int) getTextSize()), Spanned.SPAN_INCLUSIVE_INCLUSIVE));
            }
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
                if (i + 3 <= s.length() && s.subSequence(i, i + 3).toString().equals("[ ]")) {
                    s.setSpan(getCheckboxImageSpan(), i, i+3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (i + 3 <= s.length() && s.subSequence(i, i + 3).toString().equals("[x]")) {
                    s.setSpan(getCheckboxCheckedImageSpan(), i, i+3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (i + 1 <= s.length() && s.subSequence(i, i + 1).toString().equals("*")) {
                    s.setSpan(getBulletImageSpan(), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return s;
    }

    public void setMarkdown(String markdown) {
        setText(convertToRichText(markdown));
    }

    void setupCheckboxClickable(final int start, final int end, final boolean checked) {
        getText().setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                if (checked)
                    getText().insert(start, getCheckboxSpannable());
                else
                    getText().insert(start, getCheckboxCheckedSpannable());
                getText().delete(start + 4, start + 8);
                getText().removeSpan(this);
                setupCheckboxClickable(start, end, !checked);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void enterViewMode() {
        viewSource = false;
        setFocusable(false);
        setLinksClickable(true);
        setMovementMethod(LinkMovementMethod.getInstance());

        Pattern checkboxes = Pattern.compile("(?m)^\\[( |x)\\].*$");
        Matcher matcher = checkboxes.matcher(getText());
        while (matcher.find()) {
            int linestart = matcher.start();
            int lineend = matcher.end();
            setupCheckboxClickable(linestart, lineend, getText().charAt(linestart + 1) == 'x');
        }
    }

    public void exitViewMode() {
        viewSource = false;
        for (ClickableSpan span : getText().getSpans(0, length(), ClickableSpan.class)) {
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
            setMarkdown(getText().toString());
        } else {
            viewSource = true;
            setFocusable(false);
            setText(convertToMarkdown());
        }
    }

}

package com.jiaminglu.markdownedittext;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.widget.EditText;

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

    private void init() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (count > 0) {
                    for (IndentSpan span : getText().getSpans(start, start + count + 1, IndentSpan.class)) {
                        int spanStart = getText().getSpanStart(span);
                        if (spanStart >= start && spanStart <= start + count)
                            getText().removeSpan(span);
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (start + count < getText().length() && getText().charAt(start + count) == '\n') {
                    for (IndentSpan span : getText().getSpans(start + count, start + count, IndentSpan.class)) {
                        int oldStart = getText().getSpanStart(span);
                        int oldEnd = getText().getSpanEnd(span);
                        if (oldStart == start + count) {
                            getText().removeSpan(span);
                            while (start > 0 && getText().charAt(start - 1) != '\n')
                                start --;
                            getText().setSpan(span, start, oldEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                        }
                        if (oldEnd == start + count + 1 && oldStart < start) {
                            getText().removeSpan(span);
                            getText().setSpan(span, oldStart, start, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void toggleStyleSpan(CharacterStyle newSpan) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
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

    public void changeIndent(int level) {
        int start = getSelectionStart();
        if (start > getText().length())
            return;
        if (start == getText().length()) {
            int selection = getSelectionStart();
            getText().insert(getText().length(), "\n");
            setSelection(selection);
        }
        while (start > 0 && getText().charAt(start - 1) != '\n') {
            start --;
        }
        int end = start;
        while (end <= getSelectionEnd()) {
            while (end < getText().length() && (end == start || getText().charAt(end - 1) != '\n'))
                end ++;
            if (end > getText().length())
                break;
            IndentSpan[] spans = getText().getSpans(start, start + 1, IndentSpan.class);
            int newLevel = level;
            if (spans.length > 0) {
                newLevel += spans[0].getLevel();
                int oldSpanStart = getText().getSpanStart(spans[0]);
                int oldSpanEnd = getText().getSpanEnd(spans[0]);
                getText().removeSpan(spans[0]);
                if (oldSpanEnd > end) {
                    getText().setSpan(new IndentSpan(spans[0].getLevel(), (int) getTextSize()), end, oldSpanEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                }
                if (oldSpanStart < start) {
                    getText().setSpan(new IndentSpan(spans[0].getLevel(), (int) getTextSize()), oldSpanStart, start, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                }
            }
            if (newLevel > 0) {
                getText().setSpan(new IndentSpan(newLevel, (int) getTextSize()), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }
            start = end;
            if (start >= getSelectionEnd())
                break;
        }
    }

    public void indentIncrease() {
        changeIndent(1);
    }

    public void indentDecrease() {
        changeIndent(-1);
    }


}

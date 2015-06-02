package com.jiaminglu.markdownedittext;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import java.lang.ref.WeakReference;

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
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (start < s.length() && s.charAt(start) == '\n') {
                    int prevStart = start - 1;
                    while (prevStart > 0 && s.charAt(prevStart - 1) != '\n')
                        prevStart--;
                    while (s.charAt(prevStart) == '\t') {
                        getText().insert(start + 1, "\t");
                        prevStart ++;
                    }
                    if (prevStart + 3 <= getText().length()) {
                        String prevLinePrefix = s.subSequence(prevStart, prevStart + 3).toString();
                        if (prevLinePrefix.startsWith("[ ]") || prevLinePrefix.startsWith("[x]")) {
                            getText().insert(start + 1, "[ ] ");
                            getText().setSpan(getCheckboxImageSpan(), start + 1, start + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                    if (prevStart + 1 <= getText().length()) {
                        String prevLinePrefix = s.subSequence(prevStart, prevStart + 1).toString();
                        if (prevLinePrefix.startsWith("*")) {
                            getText().insert(start + 1, "* ");
                            getText().setSpan(getBulletImageSpan(), start + 1, start + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
                if (before > 0 && start - 3 >= 0 && start <= s.length() && (start == 3 || s.charAt(start - 4) == '\n')) {
                    String linePrefix = s.subSequence(start - 3, start).toString();
                    if (linePrefix.startsWith("[ ]") || linePrefix.startsWith("[x]")) {
                        for (ImageSpan span : getText().getSpans(start - 3, start, ImageSpan.class)) {
                            getText().removeSpan(span);
                        }
                        getText().delete(start - 3, start);
                    }
                }
                if (before > 0 && start - 1 >= 0 && start <= s.length() && (start == 1 || s.charAt(start - 2) == '\n')) {
                    if (s.charAt(start - 1) == '*') {
                        for (ImageSpan span : getText().getSpans(start - 1, start, ImageSpan.class)) {
                            getText().removeSpan(span);
                        }
                        getText().delete(start - 1, start);
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
                getText().insert(lineStart, "\t");
            }
        });
    }

    public void indentDecrease() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                if (lineStart < getText().length() && getText().charAt(lineStart) == '\t')
                    getText().delete(lineStart, lineStart + 1);
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
            }
        }
        if (lineStart + 2 <= getText().length()) {
            String linePrefix = getText().subSequence(lineStart, lineStart + 2).toString();
            if (linePrefix.equals("* ")) {
                getText().delete(lineStart, lineStart + 2);
            }
        }
    }

    ShapeDrawable bullet;
    {
        bullet = new ShapeDrawable(new OvalShape());
        bullet.getShape().resize(8, 8);
        bullet.getPaint().setColor(Color.BLACK);
    }

    private ImageSpan getBulletImageSpan() {
        return new CenteredImageSpan(bullet, DynamicDrawableSpan.ALIGN_BASELINE);
    }

    private ImageSpan getCheckboxImageSpan() {
        return new CenteredImageSpan(getContext(), checkboxRes, DynamicDrawableSpan.ALIGN_BASELINE);
    }

    private ImageSpan getCheckboxCheckedImageSpan() {
        return new CenteredImageSpan(getContext(), checkboxCheckedRes, DynamicDrawableSpan.ALIGN_BASELINE);
    }

    public void setLineBulleted() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                getText().insert(lineStart, "* ");
                getText().setSpan(getBulletImageSpan(), lineStart, lineStart + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        });
    }

    public void setLineCheckbox() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                getText().insert(lineStart, "[ ] ");
                getText().setSpan(getCheckboxImageSpan(), lineStart, lineStart + 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        });
    }

    public void setLineCheckboxChecked() {
        operationOnLines(new LineOperation() {
            @Override
            public void operateOn(int lineStart) {
                removeLinePrefixes(lineStart);
                getText().insert(lineStart, "[x] ");
                getText().setSpan(getCheckboxCheckedImageSpan(), lineStart, lineStart + 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        });
    }

}

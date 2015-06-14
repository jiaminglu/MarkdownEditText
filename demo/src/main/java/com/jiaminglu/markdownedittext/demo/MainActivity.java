package com.jiaminglu.markdownedittext.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.jiaminglu.markdownedittext.MarkdownEditText;
import com.jiaminglu.markdownedittext.style.BoldStyle;
import com.jiaminglu.markdownedittext.syntax.HtmlSyntax;
import com.jiaminglu.markdownedittext.style.ItalicStyle;
import com.jiaminglu.markdownedittext.style.StrikethroughStyle;
import com.jiaminglu.markdownedittext.style.UnderlineStyle;


public class MainActivity extends AppCompatActivity {

    private MarkdownEditText editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editor = (MarkdownEditText) findViewById(R.id.editor);
        editor.setSyntax(new HtmlSyntax());
    }

    public void changeFormat(View v) {
        switch (v.getId()) {
            case R.id.bold:
                editor.toggleStyleSpan(new BoldStyle());
                break;
            case R.id.italic:
                editor.toggleStyleSpan(new ItalicStyle());
                break;
            case R.id.underline:
                editor.toggleStyleSpan(new UnderlineStyle());
                break;
            case R.id.strikethrough:
                editor.toggleStyleSpan(new StrikethroughStyle());
                break;
            case R.id.indent_in:
                editor.indentIncrease();
                break;
            case R.id.indent_out:
                editor.indentDecrease();
                break;
            case R.id.checkbox:
                editor.setLineCheckbox();
                break;
            case R.id.checkbox_checked:
                editor.setLineCheckboxChecked();
                break;
            case R.id.list_bulleted:
                editor.setLineBulleted();
                break;
            case R.id.list_numbers:
                editor.setLineNumbered();
                break;
            case R.id.show_markdown:
                editor.toggleViewSource();
                break;
            case R.id.toggle_edit:
                if (((Button) v).getText().toString().equals("V")) {
                    ((Button) v).setText("E");
                    editor.enterViewMode();
                } else {
                    ((Button) v).setText("V");
                    editor.exitViewMode();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

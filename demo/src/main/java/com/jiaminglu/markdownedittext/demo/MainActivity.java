package com.jiaminglu.markdownedittext.demo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.jiaminglu.markdownedittext.MarkdownEditText;


public class MainActivity extends ActionBarActivity {

    MarkdownEditText editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editor = (MarkdownEditText) findViewById(R.id.editor);
        editor.setCheckboxRes(R.drawable.ic_checkbox_blank_outline_black_18dp);
        editor.setCheckboxCheckedRes(R.drawable.ic_checkbox_marked_black_18dp);
    }

    public void changeFormat(View v) {
        switch (v.getId()) {
            case R.id.bold:
                editor.toggleBold();
                break;
            case R.id.italic:
                editor.toggleItalic();
                break;
            case R.id.underline:
                editor.toggleUnderline();
                break;
            case R.id.strikethrough:
                editor.toggleStrikethrough();
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
                editor.showMarkdown();
                break;
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

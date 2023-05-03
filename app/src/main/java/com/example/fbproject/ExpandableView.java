package com.example.fbproject;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;

public class ExpandableView extends TextView implements View.OnClickListener {
    String text;
    boolean isExpanded = false;
    public ExpandableView(Context context) {
        super(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.addView(this);

    }

    public ExpandableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpandableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public void setText(String text){
        this.text = text;
        Spannable spannable = null;
        Log.e("length", text);
        if (text.length()>300){
            this.isExpanded = false;
            text = text.substring(0,300);
            text += " Read more...";//"<font color=#0F52BA> Read more...</font>";
            spannable = new SpannableString(text);
            spannable.setSpan(new ForegroundColorSpan(getContext().getColor(R.color.primary_blue)),300,text.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        else {
            spannable = new SpannableString(text);
        }
       Log.e("length", text);
        super.setText(spannable);
    }

    @Override
    public void onClick(View v) {

        Log.e("length", this.text);
        super.setText(this.text);
    }

    public void expand() {
        if (this.isExpanded){
            this.isExpanded = false;
            this.setText(this.text);
        }else {
            this.isExpanded = true;
            super.setText(this.text);
        }
        Log.e("length", this.text);
    }
}

package com.veha.util;

import android.app.Activity;
import android.app.AlertDialog;
import com.veha.activity.R;

public class VehaLoader extends AlertDialog {
    private Activity activity;
    public static int count = 0;

    public VehaLoader(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    public void show() {
        count++;
        this.setView(activity.getLayoutInflater().inflate(R.layout.loader, null));
        super.show();
    }

    public void dismiss() {
        count--;
        if (count < 1) {
            super.dismiss();
            count = 0;
        }
    }
}

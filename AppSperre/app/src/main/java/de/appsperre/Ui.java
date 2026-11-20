package de.appsperre;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Kleine Helfer, um die Oberflaeche direkt im Code aufzubauen. */
final class Ui {

    private Ui() {
    }

    static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setFitsSystemWindows(true);
        int p = dp(c, 20);
        l.setPadding(p, p, p, p);
        return l;
    }

    static LinearLayout.LayoutParams spaced(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 12);
        return lp;
    }

    static TextView text(Context c, String s, float sizeSp) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sizeSp);
        return t;
    }

    static EditText passwordField(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }

    static EditText numberField(Context c, String hint, String value) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(value);
        return e;
    }

    static Button button(Context c, String s, View.OnClickListener listener) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        return b;
    }
}

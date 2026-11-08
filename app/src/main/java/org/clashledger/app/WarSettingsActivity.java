package org.clashledger.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Settings for the optional war strip. Open it from the app icon's long-press menu or by tapping the strip. */
public final class WarSettingsActivity extends Activity {
    private static final int BG = 0xFF0C0E13, INK = 0xFFF2F4F8, MUTED = 0xFF8A93A5, ACCENT = 0xFFFFC15E, FIELD = 0xFF161A22;
    private TextView status;
    private CheckBox enable;
    private EditText key;

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private void add(LinearLayout parent, android.view.View v, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(topDp);
        parent.addView(v, p);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        final int side = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(side, dp(40), side, dp(28));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(side, insets.getSystemWindowInsetTop() + dp(16), side, insets.getSystemWindowInsetBottom() + dp(28));
            return insets;
        });

        add(root, text("War & clan data", 22, INK, true), 0);
        add(root, text("Shows your clan's war or CWL round on the widget, with a countdown to the start or the end. "
            + "Off by default. When it is on, your player tag and clan tag are sent to a public proxy of the official "
            + "Clash of Clans API. Your village export is never sent. The clan's war log must be public for regular wars.", 13, MUTED, false), 8);

        enable = new CheckBox(this);
        enable.setText("Show war and CWL on the widget");
        enable.setTextColor(INK);
        enable.setChecked(War.enabled(this));
        add(root, enable, 16);

        add(root, text("Your own API key (optional)", 12, MUTED, true), 16);
        key = new EditText(this);
        key.setSingleLine(true);
        key.setHint("Paste a key from developer.clashofclans.com");
        key.setHintTextColor(MUTED);
        key.setTextColor(INK);
        key.setBackgroundColor(FIELD);
        key.setPadding(dp(12), dp(12), dp(12), dp(12));
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        key.setText(War.apiKey(this));
        add(root, key, 6);
        add(root, text("Without a key, the free ClashKing proxy is used. With a key, it goes through RoyaleAPI's proxy, "
            + "so add the IP address 45.79.218.79 to the key's allowed addresses. The key is stored only on this phone.", 12, MUTED, false), 6);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> { save(); status.setText("Saved."); });
        Button check = new Button(this);
        check.setText("Check now");
        check.setOnClickListener(v -> checkNow());
        buttons.addView(save);
        buttons.addView(check);
        add(root, buttons, 16);

        status = text("", 13, ACCENT, false);
        add(root, status, 12);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#0C0E13")));
    }

    @Override protected void onResume() {
        super.onResume();
        if (War.enabled(this)) status.setText(War.describe(War.status(this)));
    }

    private void save() {
        War.save(this, enable.isChecked(), key.getText().toString());
        UpgradeWidget.refresh(this);
    }

    private void checkNow() {
        save();
        if (!enable.isChecked()) { status.setText("Turn on the switch first."); return; }
        status.setText("Checking…");
        War.syncAsync(this, true, changed -> runOnUiThread(() -> {
            status.setText(War.describe(War.status(this)));
            UpgradeWidget.refresh(this);
        }));
    }
}

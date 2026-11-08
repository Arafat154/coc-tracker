package org.clashledger.app;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A launcher-hosted scrolling collection keeps every upgrade accessible at any widget size. */
public final class WidgetRowsService extends RemoteViewsService {
    private static final int SEGMENTS = 12;
    private static final int[] SEG_IDS = {
        R.id.seg0, R.id.seg1, R.id.seg2, R.id.seg3, R.id.seg4, R.id.seg5,
        R.id.seg6, R.id.seg7, R.id.seg8, R.id.seg9, R.id.seg10, R.id.seg11
    };
    private static final long LOW_MS = 3600000L;
    private static final int WHITE = 0xFFF2F4F8, MUTED = 0xFF8A93A5, RED = 0xFFFF6B6B, GREEN = 0xFF5EE08A;

    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            final List<RemoteViews> rows = new ArrayList<>();
            @Override public void onCreate() { }
            @Override public void onDataSetChanged() {
                rows.clear(); rows.addAll(buildRows(WidgetRowsService.this));
            }
            @Override public void onDestroy() { rows.clear(); }
            @Override public int getCount() { return rows.size(); }
            @Override public RemoteViews getViewAt(int position) { return position >= 0 && position < rows.size() ? rows.get(position) : null; }
            @Override public RemoteViews getLoadingView() { return null; }
            @Override public int getViewTypeCount() { return 2; }
            @Override public long getItemId(int position) { return position; }
            @Override public boolean hasStableIds() { return false; }
        };
    }

    /** Accent drawable (a rounded block in the section color) for each upgrade group. */
    private static int accentDrawable(String group) {
        switch (group) {
            case "Laboratory": return R.drawable.seg_violet;
            case "Pets": return R.drawable.seg_teal;
            case "Builder Base":
            case "Star Laboratory": return R.drawable.seg_blue;
            default: return R.drawable.seg_amber;
        }
    }

    /** Same palette as the drawables, as a plain color for text. */
    private static int accentColor(String group) {
        switch (group) {
            case "Laboratory": return 0xFFA99BFF;
            case "Pets": return 0xFF5EEAD4;
            case "Builder Base":
            case "Star Laboratory": return 0xFF6CC4FF;
            default: return 0xFFFFC15E;
        }
    }

    public static List<RemoteViews> buildRows(android.content.Context context) {
        List<RemoteViews> rows = new ArrayList<>();
        Ledger ledger = new Ledger(context);
        long now = System.currentTimeMillis(), elapsed = SystemClock.elapsedRealtime();
        String pkg = context.getPackageName();
        String style = context.getSharedPreferences("display", 0).getString("timerStyle", "compact");
        List<Upgrade> ordered = new ArrayList<>(ledger.upgrades);
        ordered.sort(java.util.Comparator.comparingLong(u -> u.wallRemaining(now, ledger.boostUntil)));
        for (String group : Upgrade.GROUPS) {
            int accent = accentDrawable(group);
            RemoteViews section = new RemoteViews(pkg, R.layout.widget_section);
            section.setTextViewText(R.id.section, group.toUpperCase(Locale.ROOT));
            section.setTextColor(R.id.section_dot, accentColor(group));
            if (group.equals("Builders")) {
                int busy = ledger.busy(now);
                int free = Math.max(0, ledger.builders - busy);
                String right = (free > 0 ? free + " free · " : "") + busy + "/" + ledger.builders + " busy"
                    + (ledger.goblins(now) > 0 ? " + Goblin" : "");
                section.setTextViewText(R.id.section_right, right);
                section.setTextColor(R.id.section_right, free > 0 ? GREEN : MUTED);
            }
            section.setOnClickFillInIntent(R.id.section_root, new Intent());
            section.setOnClickFillInIntent(R.id.section, new Intent());
            rows.add(section);
            boolean any = false;
            for (Upgrade u : ordered) {
                if (!u.group.equals(group)) continue;
                any = true;
                RemoteViews row = new RemoteViews(pkg, R.layout.widget_row);
                long left = u.wallRemaining(now, ledger.boostUntil);
                boolean low = left > 0 && left <= LOW_MS;
                row.setTextViewText(R.id.name, u.name + (u.goblin ? " · G" : "") + (u.helperLevel > 0 ? " · H" + u.helperLevel : ""));
                row.setTextViewText(R.id.level, u.isForge() ? "Gold" : "→ " + u.level);
                row.setImageViewResource(R.id.artwork, Artwork.forUpgrade(context, u));
                row.setOnClickFillInIntent(R.id.widget_row, new Intent());
                row.setChronometerCountDown(R.id.countdown, true);
                row.setChronometer(R.id.countdown, elapsed + left, null, left > 0 && style.equals("clock"));
                if (!style.equals("clock") || left == 0) row.setTextViewText(R.id.countdown, TimerFormat.format(left, style));
                row.setTextColor(R.id.countdown, low ? RED : WHITE);
                int filled = left == 0 ? SEGMENTS
                    : Math.max(1, Math.min(SEGMENTS, Math.round(u.progress(now, ledger.boostUntil) * SEGMENTS / 1000f)));
                int on = low ? R.drawable.seg_red : accent;
                for (int i = 0; i < SEGMENTS; i++) {
                    row.setInt(SEG_IDS[i], "setBackgroundResource", i < filled ? on : R.drawable.seg_off);
                }
                String note = left == 0 ? "Tap to manage" : u.helperLabel(now);
                if (note.isEmpty()) {
                    row.setViewVisibility(R.id.finish, View.GONE);
                } else {
                    row.setViewVisibility(R.id.finish, View.VISIBLE);
                    row.setTextViewText(R.id.finish, note);
                }
                row.setOnClickFillInIntent(R.id.name, new Intent());
                row.setOnClickFillInIntent(R.id.countdown, new Intent());
                rows.add(row);
            }
            if (!any) {
                RemoteViews empty = new RemoteViews(pkg, R.layout.widget_section);
                empty.setTextViewText(R.id.section, "No upgrades · tap to add");
                empty.setViewVisibility(R.id.section_dot, View.GONE);
                empty.setOnClickFillInIntent(R.id.section_root, new Intent());
                empty.setOnClickFillInIntent(R.id.section, new Intent());
                rows.add(empty);
            }
        }
        return rows;
    }
}

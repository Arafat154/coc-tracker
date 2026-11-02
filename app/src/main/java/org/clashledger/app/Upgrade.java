package org.clashledger.app;

/** Remaining work is measured in milliseconds at normal speed, never wall time. */
public final class Upgrade {
    public static final String[] GROUPS = {"Builders", "Laboratory", "Pets", "Builder Base", "Star Laboratory"};
    public boolean goblin;
    public String id, name, group;
    public int level;
    public long work, total, anchor;

    public Upgrade(String id, String name, String group, int level, long work, long anchor) {
        this.id = id; this.name = name; this.group = group; this.level = level;
        this.work = work; this.total = work; this.anchor = anchor;
    }
    public boolean isBuilder() { return group.equals("Builders"); }
    public long remaining(long now, long boostUntil) {
        long elapsed = Math.max(0, now - anchor);
        long boosted = isBuilder() ? Math.max(0, Math.min(now, boostUntil) - anchor) : 0;
        return Math.max(0, work - elapsed - 9 * boosted);
    }
    public long wallRemaining(long now, long boostUntil) {
        long left = remaining(now, boostUntil);
        long boost = isBuilder() ? Math.max(0, boostUntil - now) : 0;
        if (left <= boost * 10) return (left + 9) / 10;
        return left - boost * 9;
    }
    public void settle(long now, long boostUntil) {
        work = remaining(now, boostUntil); anchor = now;
    }
    public int progress(long now, long boostUntil) {
        return total <= 0 ? 1000 : (int) Math.max(0, Math.min(1000, 1000d * (1d - (double) remaining(now, boostUntil) / total)));
    }
    public static String duration(long ms) {
        if (ms <= 0) return "Ready";
        long minutes = Math.max(1, (ms + 59999) / 60000);
        long days = minutes / 1440, hours = minutes / 60 % 24;
        return (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + minutes % 60 + "m";
    }
}

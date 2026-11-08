package org.clashledger.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Optional war and CWL timer. Off by default. When on, it sends only the player tag and the clan tag
 * to the chosen proxy of the official Clash of Clans API. Village exports never leave the device.
 */
public final class War {
    public static final String CLASHKING = "https://proxy.clashk.ing/v1/";
    public static final String ROYALE = "https://cocproxy.royaleapi.dev/v1/";
    private static final long STALE_MS = 10 * 60_000L, CLAN_MS = 6 * 3_600_000L, CK_DOWN_MS = 30 * 60_000L;
    private static final long[] FRIENDLY_PREP_MIN = {5, 15, 30, 60, 120, 240, 360, 480, 720, 960, 1200, 1440};
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);
    private static final Object LOCK = new Object();

    public interface Done { void run(boolean changed); }

    public static final class Summary {
        public String type = "war", state = "", ours = "?", theirs = "?";
        public long prepStart, start, end;
        public int oursStars, theirsStars, attacksLeft = -1;

        JSONObject json() throws org.json.JSONException {
            return new JSONObject().put("type", type).put("state", state).put("ours", ours).put("theirs", theirs)
                .put("prepStart", prepStart).put("start", start).put("end", end)
                .put("oursStars", oursStars).put("theirsStars", theirsStars).put("attacksLeft", attacksLeft);
        }
        static Summary read(JSONObject o) {
            Summary s = new Summary();
            s.type = o.optString("type", "war"); s.state = o.optString("state"); s.ours = o.optString("ours", "?"); s.theirs = o.optString("theirs", "?");
            s.prepStart = o.optLong("prepStart"); s.start = o.optLong("start"); s.end = o.optLong("end");
            s.oursStars = o.optInt("oursStars"); s.theirsStars = o.optInt("theirsStars"); s.attacksLeft = o.optInt("attacksLeft", -1);
            return s;
        }
    }

    public static final class Status {
        public String playerTag = "", clanTag = "", clanName = "", note = "";
        public long fetched, clanChecked;
        public Summary war;
    }

    private static final class Reply {
        int code = -1;
        JSONObject json;
        String reason = "", via = "";
        boolean ok() { return code == 200 && json != null && !json.has("reason"); }
    }

    private War() { }

    // ---------------------------------------------------------------- settings

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("war", Context.MODE_PRIVATE); }
    public static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", false); }
    public static String apiKey(Context c) { return prefs(c).getString("apiKey", "").trim(); }
    public static void save(Context c, boolean enabled, String key) {
        prefs(c).edit().putBoolean("enabled", enabled).putString("apiKey", key == null ? "" : key.trim()).apply();
    }

    // ------------------------------------------------------------------- cache

    private static Status cached(Context c) {
        String raw;
        synchronized (LOCK) { raw = prefs(c).getString("cache", null); }
        if (raw == null) return null;
        try {
            JSONObject o = new JSONObject(raw);
            Status s = new Status();
            s.fetched = o.optLong("fetched"); s.playerTag = o.optString("playerTag"); s.clanTag = o.optString("clanTag");
            s.clanName = o.optString("clanName"); s.clanChecked = o.optLong("clanChecked"); s.note = o.optString("note");
            JSONObject w = o.optJSONObject("war");
            if (w != null) s.war = Summary.read(w);
            return s;
        } catch (Exception e) { return null; }
    }

    private static void store(Context c, Status s) {
        try {
            JSONObject o = new JSONObject().put("fetched", s.fetched).put("playerTag", s.playerTag).put("clanTag", s.clanTag)
                .put("clanName", s.clanName).put("clanChecked", s.clanChecked).put("note", s.note);
            if (s.war != null) o.put("war", s.war.json());
            synchronized (LOCK) { prefs(c).edit().putString("cache", o.toString()).apply(); }
        } catch (Exception ignored) { }
    }

    public static Status status(Context c) { return cached(c); }

    /** The war to show on the widget right now, or null. */
    public static Summary current(Context c, String playerTag) {
        if (!enabled(c)) return null;
        Status s = cached(c);
        if (s == null || s.war == null || playerTag == null || !playerTag.equals(s.playerTag)) return null;
        if (s.war.end > 0 && s.war.end < System.currentTimeMillis()) return null;
        return s.war;
    }

    // -------------------------------------------------------------------- HTTP

    private static String enc(String tag) { return (tag.startsWith("#") ? tag : "#" + tag).replace("#", "%23"); }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n, total = 0;
        while ((n = in.read(buf)) > 0 && total < 600_000) { out.write(buf, 0, n); total += n; }
        return out.toString("UTF-8");
    }

    private static Reply request(String url, String key) {
        Reply r = new Reply();
        HttpURLConnection h = null;
        try {
            h = (HttpURLConnection) new URL(url).openConnection();
            h.setConnectTimeout(6000);
            h.setReadTimeout(8000);
            h.setRequestProperty("Accept", "application/json");
            if (!key.isEmpty()) h.setRequestProperty("Authorization", "Bearer " + key);
            r.code = h.getResponseCode();
            InputStream in = r.code >= 400 ? h.getErrorStream() : h.getInputStream();
            String body = "";
            if (in != null) { try (InputStream stream = in) { body = read(stream); } }
            try { r.json = new JSONObject(body); } catch (Exception ignored) { }
            if (r.json != null) {
                String reason = r.json.optString("reason", r.json.optString("message", ""));
                r.reason = reason == null ? "" : reason;
            } else if (!body.isEmpty()) {
                r.reason = body.length() > 90 ? body.substring(0, 90) : body;
            }
        } catch (Exception e) {
            r.code = -1;
            String m = String.valueOf(e);
            r.reason = m.length() > 90 ? m.substring(0, 90) : m;
        } finally {
            if (h != null) h.disconnect();
        }
        return r;
    }

    /** Tries your own key (through RoyaleAPI's proxy) first, then ClashKing's free proxy. */
    private static Reply get(Context c, String path) {
        SharedPreferences p = prefs(c);
        String key = apiKey(c);
        List<String[]> sources = new ArrayList<>();
        if (!key.isEmpty()) sources.add(new String[]{ROYALE, key, "your API key"});
        if (System.currentTimeMillis() > p.getLong("ckDownUntil", 0)) sources.add(new String[]{CLASHKING, "", "ClashKing"});
        Reply last = new Reply();
        last.reason = "ClashKing did not respond recently. Add a backup key or try again in 30 minutes.";
        for (String[] src : sources) {
            Reply r = request(src[0] + path, src[1]);
            r.via = src[2];
            if (r.ok()) return r;
            last = r;
            if (r.code == 403 && "accessDenied.invalidIp".equals(r.reason)) r.reason = "Key does not allow IP 45.79.218.79";
            if (r.code == 404 || "accessDenied".equals(r.reason)) return r;
            if (r.code == -1 && "ClashKing".equals(src[2])) {
                p.edit().putLong("ckDownUntil", System.currentTimeMillis() + CK_DOWN_MS).apply();
            }
        }
        return last;
    }

    private static String describe(Reply r) {
        return (r.code > 0 ? r.code + "" : "network") + (r.reason.isEmpty() ? "" : " · " + r.reason) + (r.via.isEmpty() ? "" : " (" + r.via + ")");
    }

    // -------------------------------------------------------------------- data

    private static long time(String s) {
        if (s == null || s.length() < 15) return 0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = f.parse(s.substring(0, 15));
            return d == null ? 0 : d.getTime();
        } catch (Exception e) { return 0; }
    }

    private static String warType(JSONObject w) {
        double prep = (time(w.optString("startTime")) - time(w.optString("preparationStartTime"))) / 60000d;
        for (long m : FRIENDLY_PREP_MIN) if (Math.abs(prep - m) < 1) return "friendly";
        return "war";
    }

    private static Summary summarize(JSONObject w, String type, String clanTag, String playerTag) {
        JSONObject a = w.optJSONObject("clan"), b = w.optJSONObject("opponent");
        boolean mine = a != null && clanTag.equals(a.optString("tag"));
        JSONObject ours = mine ? a : b, theirs = mine ? b : a;
        Summary s = new Summary();
        s.type = type; s.state = w.optString("state");
        s.prepStart = time(w.optString("preparationStartTime")); s.start = time(w.optString("startTime")); s.end = time(w.optString("endTime"));
        if (ours != null) { s.ours = ours.optString("name", "?"); s.oursStars = ours.optInt("stars"); }
        if (theirs != null) { s.theirs = theirs.optString("name", "?"); s.theirsStars = theirs.optInt("stars"); }
        int per = w.optInt("attacksPerMember", 1);
        JSONArray members = ours == null ? null : ours.optJSONArray("members");
        if (members != null) {
            for (int i = 0; i < members.length(); i++) {
                JSONObject m = members.optJSONObject(i);
                if (m == null || !playerTag.equalsIgnoreCase(m.optString("tag"))) continue;
                JSONArray used = m.optJSONArray("attacks");
                s.attacksLeft = Math.max(0, per - (used == null ? 0 : used.length()));
            }
        }
        return s;
    }

    private static void fetchWar(Context c, String clanTag, String playerTag, Status s) {
        Reply cw = get(c, "clans/" + enc(clanTag) + "/currentwar");
        if (cw.ok()) {
            String st = cw.json.optString("state");
            if (st.equals("inWar") || st.equals("preparation")) {
                s.war = summarize(cw.json, warType(cw.json), clanTag, playerTag);
                s.note = "";
                return;
            }
            s.note = "";
        } else if (cw.code == 403 || "accessDenied".equals(cw.reason)) {
            s.note = "War log is private (CWL still checked)";
        } else {
            s.note = "War check failed (" + describe(cw) + ")";
        }
        Reply g = get(c, "clans/" + enc(clanTag) + "/currentwar/leaguegroup");
        JSONArray rounds = g.ok() ? g.json.optJSONArray("rounds") : null;
        String gs = g.ok() ? g.json.optString("state") : "";
        if (rounds == null || !(gs.equals("inWar") || gs.equals("preparation"))) {
            if (!g.ok() && g.code != 404) s.note += (s.note.isEmpty() ? "" : " · ") + "CWL check failed (" + describe(g) + ")";
            else if (s.note.isEmpty()) s.note = "Not in a war";
            return;
        }
        JSONObject live = null, prep = null;
        int calls = 0;
        for (int r = rounds.length() - 1; r >= 0 && live == null; r--) {
            JSONObject round = rounds.optJSONObject(r);
            JSONArray tags = round == null ? null : round.optJSONArray("warTags");
            if (tags == null) continue;
            for (int t = 0; t < tags.length(); t++) {
                String wt = tags.optString(t);
                if (wt.isEmpty() || wt.equals("#0")) continue;
                if (calls++ > 20) break;
                Reply wr = get(c, "clanwarleagues/wars/" + enc(wt));
                if (!wr.ok()) continue;
                JSONObject w = wr.json;
                JSONObject a = w.optJSONObject("clan"), b = w.optJSONObject("opponent");
                if (a == null || b == null) continue;
                if (!clanTag.equals(a.optString("tag")) && !clanTag.equals(b.optString("tag"))) continue;
                String ws = w.optString("state");
                if (ws.equals("inWar")) live = w; else if (ws.equals("preparation") && prep == null) prep = w;
                break;
            }
        }
        JSONObject pick = live != null ? live : prep;
        if (pick != null) { s.war = summarize(pick, "cwl", clanTag, playerTag); s.note = ""; }
        else s.note = "CWL: no active round";
    }

    /** Blocking refresh. Returns true when it contacted the network. Call it off the main thread. */
    public static boolean sync(Context c, boolean force) {
        if (!BUSY.compareAndSet(false, true)) return false;
        try {
            long now = System.currentTimeMillis();
            String player = new Ledger(c).playerTag;
            Status old = cached(c);
            boolean same = old != null && player.equals(old.playerTag);
            boolean ended = same && old.war != null && old.war.end > 0 && old.war.end < now;
            if (!force && same && !ended && now - old.fetched < STALE_MS) return false;
            Status s = new Status();
            s.playerTag = player; s.fetched = now;
            if (player.isEmpty()) {
                s.note = "Import your village first. No player tag is saved yet.";
                store(c, s);
                return true;
            }
            if (same) { s.clanTag = old.clanTag; s.clanName = old.clanName; s.clanChecked = old.clanChecked; }
            if (force || s.clanTag.isEmpty() || now - s.clanChecked > CLAN_MS) {
                Reply p = get(c, "players/" + enc(player));
                if (!p.ok()) {
                    s.note = "Player lookup failed (" + describe(p) + ")";
                    if (same && !ended) s.war = old.war;
                    store(c, s);
                    return true;
                }
                JSONObject clan = p.json.optJSONObject("clan");
                s.clanChecked = now;
                if (clan == null) {
                    s.clanTag = ""; s.clanName = ""; s.note = "This player is not in a clan.";
                    store(c, s);
                    return true;
                }
                s.clanTag = clan.optString("tag"); s.clanName = clan.optString("name");
            }
            fetchWar(c, s.clanTag, player, s);
            store(c, s);
            return true;
        } finally {
            BUSY.set(false);
        }
    }

    public static void syncAsync(Context c, boolean force, Done done) {
        final Context app = c.getApplicationContext();
        if (!enabled(app)) { if (done != null) done.run(false); return; }
        new Thread(() -> {
            boolean changed = false;
            try { changed = sync(app, force); } catch (Exception ignored) { }
            if (done != null) done.run(changed);
        }, "war-sync").start();
    }

    /** Plain-language summary for the settings screen. */
    public static String describe(Status s) {
        if (s == null) return "Not checked yet.";
        StringBuilder b = new StringBuilder();
        if (!s.clanName.isEmpty()) b.append("Clan: ").append(s.clanName).append('\n');
        long now = System.currentTimeMillis();
        Summary w = s.war;
        if (w != null && (w.end == 0 || w.end > now)) {
            boolean prep = w.start > now;
            String kind = w.type.equals("cwl") ? "CWL war" : w.type.equals("friendly") ? "Friendly war" : "War";
            b.append(kind).append(prep ? " · preparation" : " · battle day").append('\n');
            b.append(w.ours).append(" vs ").append(w.theirs);
            if (!prep) b.append("  ★ ").append(w.oursStars).append("–").append(w.theirsStars);
            b.append('\n');
            if (!prep && w.attacksLeft >= 0) b.append(w.attacksLeft).append(" attack(s) left\n");
        } else {
            b.append("No active war or CWL round.\n");
        }
        if (!s.note.isEmpty()) b.append(s.note).append('\n');
        b.append("Checked ").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(s.fetched)));
        return b.toString();
    }
}

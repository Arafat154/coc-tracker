package org.clashledger.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public final class Ledger {
    public final List<Upgrade> upgrades = new ArrayList<>();
    public String village = "My village";
    public int builders = 6, townHall;
    public long boostUntil, importedAt;
    public String playerTag = "";
    private final SharedPreferences prefs;
    public Ledger(Context context) {
        prefs = context.getSharedPreferences("ledger", Context.MODE_PRIVATE);
        village = prefs.getString("village", "My village");
        townHall = prefs.getInt("townHall", 0);
        builders = prefs.getInt("builders", 6);
        importedAt = prefs.getLong("importedAt", 0);
        playerTag = prefs.getString("playerTag", "");
        boostUntil = prefs.getLong("boostUntil", 0);
        try {
            JSONArray rows = new JSONArray(prefs.getString("upgrades", "[]"));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject o = rows.getJSONObject(i);
                Upgrade u = new Upgrade(o.getString("id"), o.getString("name"), o.getString("group"), o.getInt("level"), o.getLong("work"), o.getLong("anchor"));
                u.goblin = o.optBoolean("goblin", false); u.total = o.getLong("total"); upgrades.add(u);
            }
        } catch (JSONException e) { throw new IllegalStateException("Could not read saved upgrades", e); }
    }
    public void save() {
        JSONArray rows = new JSONArray();
        try {
            for (Upgrade u : upgrades) {
                JSONObject o = new JSONObject();
                o.put("id", u.id).put("name", u.name).put("group", u.group).put("level", u.level)
                    .put("goblin", u.goblin).put("work", u.work).put("total", u.total).put("anchor", u.anchor);
                rows.put(o);
            }
        } catch (JSONException e) { throw new IllegalStateException(e); }
        prefs.edit().putString("upgrades", rows.toString()).putString("village", village)
            .putLong("importedAt", importedAt).putString("playerTag", playerTag).putInt("townHall", townHall).putInt("builders", builders).putLong("boostUntil", boostUntil).apply();
    }
    public void changeBoost(long now, long newEnd) {
        for (Upgrade u : upgrades) u.settle(now, boostUntil);
        boostUntil = newEnd; save();
    }
    public int goblins(long now) {
        int count = 0;
        for (Upgrade u : upgrades) if (u.isBuilder() && u.goblin && u.remaining(now, boostUntil) > 0) count++;
        return count;
    }
    public void importVillage(VillageImport data) {
        if (!data.tag.equals(playerTag)) village = data.tag;
        playerTag = data.tag; importedAt = data.timestamp;
        builders = data.builders; townHall = data.townHall; boostUntil = data.boostUntil;
        upgrades.clear(); upgrades.addAll(data.upgrades); save();
    }
    public int busy(long now) {
        int count = 0;
        for (Upgrade u : upgrades) if (u.isBuilder() && !u.goblin && u.remaining(now, boostUntil) > 0) count++;
        return count;
    }
}

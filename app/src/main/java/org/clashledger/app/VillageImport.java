package org.clashledger.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.ArrayList;
import java.util.List;

/** Parses a complete export before any saved village is changed. All game durations are seconds. */
public final class VillageImport {
    public final String tag;
    public final long timestamp, boostUntil;
    public final int builders, townHall;
    public final List<Upgrade> upgrades = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
    private static final long MAX_DURATION = 366L * 24 * 3600;
    private static final String[][] SECTIONS = {
        {"buildings","Builders"}, {"traps","Builders"}, {"heroes","Builders"}, {"guardians","Builders"},
        {"units","Laboratory"}, {"siege_machines","Laboratory"}, {"spells","Laboratory"}, {"pets","Pets"},
        {"buildings2","Builder Base"}, {"traps2","Builder Base"}, {"heroes2","Builder Base"}, {"units2","Star Laboratory"}
    };
    public VillageImport(String text, JSONObject names, long now) throws JSONException {
        if (text.length() > 1_000_000) throw new IllegalArgumentException("Export is too large (maximum 1 MB).");
        JSONTokener tokener = new JSONTokener(text.trim());
        Object parsed = tokener.nextValue();
        if (!(parsed instanceof JSONObject) || tokener.nextClean() != 0) throw new IllegalArgumentException("Paste one complete village JSON object.");
        JSONObject root = (JSONObject) parsed;
        tag = root.getString("tag").trim();
        if (!tag.matches("#[A-Za-z0-9]{3,20}")) throw new IllegalArgumentException("Export has no valid player tag.");
        timestamp = number(root, "timestamp", 946684800L, 4102444800L) * 1000L;
        if (timestamp > now + 300000L) throw new IllegalArgumentException("Export timestamp is in the future. Check your device clock.");
        if (!(root.opt("buildings") instanceof JSONArray)) throw new IllegalArgumentException("This is not a village export: buildings are missing.");
        JSONObject boosts = root.has("boosts") ? root.getJSONObject("boosts") : new JSONObject();
        long boostSeconds = boosts.has("builder_boost") ? number(boosts, "builder_boost", 0, MAX_DURATION) : 0;
        boostUntil = boostSeconds > 0 ? timestamp + boostSeconds * 1000L : 0;
        for (String key : new String[]{"lab_boost", "clocktower_boost", "builder_consumable", "lab_consumable"}) {
            if (boosts.has(key) && number(boosts,key,0,MAX_DURATION) > 0) warnings.add("Active " + key.replace('_',' ') + " is not simulated; its timers may need correction.");
        }
        int huts = 0, hall = 0;
        JSONArray buildings = root.getJSONArray("buildings");
        for (int i=0; i<buildings.length(); i++) {
            JSONObject item = buildings.getJSONObject(i);
            if (item.optLong("data") == 1000001) hall = (int) number(item,"lvl",1,100);
            if (item.optLong("data") == 1000015) huts += item.has("cnt") ? (int) number(item,"cnt",1,7) : 1;
        }
        JSONArray base = root.optJSONArray("buildings2");
        if (base != null) for (int i=0;i<base.length();i++) {
            JSONObject item=base.getJSONObject(i);
            if (item.optLong("data")==1000065 && item.optInt("lvl")>=5) { huts++; break; }
        }
        townHall = hall;
        builders = Math.max(1, Math.min(6, huts));
        boolean helper = false;
        for (String[] section : SECTIONS) {
            if (!root.has(section[0])) continue;
            JSONArray entries = root.getJSONArray(section[0]);
            for (int i=0; i<entries.length(); i++) {
                JSONObject item = entries.getJSONObject(i);
                if (!item.has("timer")) continue;
                long seconds = number(item,"timer",0,MAX_DURATION);
                long id = number(item,"data",1,Integer.MAX_VALUE);
                int level = (int) number(item,"lvl",0,999);
                String name = names.optString(String.valueOf(id), "Item " + id);
                if (!names.has(String.valueOf(id))) warnings.add("Unknown item " + id + " kept with its ID; you can rename it.");
                Upgrade u = new Upgrade("import:" + section[0] + ":" + i, name, section[1], level+1, seconds*1000L, timestamp);
                u.goblin = item.optBoolean("extra",false);
                upgrades.add(u);
                if (item.optLong("helper_timer",0)>0) helper = true;
                if (upgrades.size()>300) throw new IllegalArgumentException("Export has too many running upgrades.");
            }
        }
        if (helper) warnings.add("Active helper work is not simulated; affected timers may need correction.");
        if (now-timestamp > 86400000L) warnings.add("This export is over a day old. Timers use its original timestamp.");
    }
    private static long number(JSONObject object, String key, long min, long max) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof Number)) throw new IllegalArgumentException(key + " must be a whole number.");
        Number n=(Number)value; long number=n.longValue();
        if (n.doubleValue()!=number || number<min || number>max) throw new IllegalArgumentException(key + " is outside its supported range.");
        return number;
    }
}

package org.clashledger.app;

import org.json.JSONObject;
import org.json.JSONException;

/** Export identity is a player tag and export timestamp, independent of whitespace/key order. */
public final class ClipboardOffer {
    public final VillageImport village;
    public final String key;
    public ClipboardOffer(String text,long now)throws JSONException {
        if(text==null||text.length()>1_000_000||!text.trim().startsWith("{"))throw new IllegalArgumentException("Not an export");
        village=new VillageImport(text,new JSONObject(),now);
        key=village.tag.toUpperCase(java.util.Locale.ROOT)+"|"+village.timestamp;
    }
    public boolean duplicate(String savedTag,long savedTimestamp){return village.tag.equalsIgnoreCase(savedTag)&&village.timestamp==savedTimestamp;}
    public boolean older(String savedTag,long savedTimestamp){return village.tag.equalsIgnoreCase(savedTag)&&village.timestamp<savedTimestamp;}
}

package org.clashledger.app;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Versioned account snapshots. A single persisted JSON record makes selection and updates atomic. */
public final class AccountBook {
    private final JSONObject accounts;
    private String active;
    public AccountBook(String saved, JSONObject legacy) throws JSONException {
        if(saved==null) { accounts=new JSONObject(); active="legacy"; accounts.put(active,copy(legacy)); }
        else { JSONObject root=new JSONObject(saved); if(root.getInt("version")!=1)throw new JSONException("Unsupported account storage version");
            accounts=root.getJSONObject("accounts");active=root.getString("active");
            if(accounts.length()==0||!accounts.has(active))throw new JSONException("Missing active village");
        }
    }
    private static JSONObject copy(JSONObject value) throws JSONException { return new JSONObject(value.toString()); }
    public String activeId() { return active; }
    public List<String> ids() { List<String> ids=new ArrayList<>();accounts.keys().forEachRemaining(ids::add);java.util.Collections.sort(ids);return ids; }
    public JSONObject get(String id) throws JSONException { return copy(accounts.getJSONObject(id)); }
    public void put(String id,JSONObject value) throws JSONException { if(!accounts.has(id))throw new JSONException("Village was removed");accounts.put(id,copy(value)); }
    public void select(String id) throws JSONException { if(!accounts.has(id))throw new JSONException("Village not found");active=id; }
    public String next() {
        List<String> ordered=ids();active=ordered.get((ordered.indexOf(active)+1)%ordered.size());return active;
    }
    public String add(JSONObject value) throws JSONException { String id=UUID.randomUUID().toString();accounts.put(id,copy(value));return id; }
    public String findTag(String tag) throws JSONException { if(tag.isEmpty())return null;for(String id:ids())if(tag.equalsIgnoreCase(accounts.getJSONObject(id).optString("playerTag")))return id;return null; }
    public void remove(String id) throws JSONException {
        if(!accounts.has(id))return;accounts.remove(id);
        if(accounts.length()==0)active=add(new JSONObject());else if(id.equals(active))active=ids().get(0);
    }
    public String json() throws JSONException { return new JSONObject().put("version",1).put("active",active).put("accounts",accounts).toString(); }
}

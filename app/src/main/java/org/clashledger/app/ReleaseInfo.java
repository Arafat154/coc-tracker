package org.clashledger.app;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
public final class ReleaseInfo {
    public final String version;
    public final boolean hasApk;
    public ReleaseInfo(JSONObject data)throws JSONException {
        if(data.optBoolean("draft")||data.optBoolean("prerelease"))throw new IllegalArgumentException("Not a stable release");
        version=data.getString("tag_name");ReleaseVersion.compare(version,"0.0.0");
        JSONArray assets=data.optJSONArray("assets");boolean apk=false;
        if(assets!=null)for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);if(a.optString("name").toLowerCase(java.util.Locale.ROOT).endsWith(".apk")&&a.optLong("size")>0&&a.optString("state").equals("uploaded"))apk=true;}
        hasApk=apk;
    }
    public boolean newerThan(String installed){return hasApk&&ReleaseVersion.compare(version,installed)>0;}
    public static boolean checkDue(long last,long now){return last<=0||now<last||now-last>=86400000L;}
}

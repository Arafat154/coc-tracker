package org.clashledger.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReleaseInfoTest {
    private JSONObject release(String name,String state,long size)throws Exception {
        return new JSONObject().put("tag_name","v1.5.0").put("assets",new JSONArray().put(new JSONObject().put("name",name).put("state",state).put("size",size)));
    }
    @Test public void newerStableUploadedApkCanBeOffered()throws Exception {
        ReleaseInfo r=new ReleaseInfo(release("app-debug.apk","uploaded",100));
        assertTrue(r.newerThan("1.4.0"));assertFalse(r.newerThan("1.5.0"));assertFalse(r.newerThan("2.0.0"));
    }
    @Test public void sourceOnlyIncompleteOrEmptyAssetsDoNotAdvertiseAnUpdate()throws Exception {
        assertFalse(new ReleaseInfo(release("source.zip","uploaded",100)).newerThan("1.4.0"));
        assertFalse(new ReleaseInfo(release("app.apk","starter",100)).newerThan("1.4.0"));
        assertFalse(new ReleaseInfo(release("app.apk","uploaded",0)).newerThan("1.4.0"));
    }
    @Test public void prereleaseDraftAndInvalidTagsAreRejected()throws Exception {
        for(String key:new String[]{"prerelease","draft"})try{new ReleaseInfo(release("app.apk","uploaded",10).put(key,true));fail();}catch(IllegalArgumentException expected){}
        try{new ReleaseInfo(release("app.apk","uploaded",10).put("tag_name","latest"));fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void automaticChecksAreDailyAndRecoverFromClockChanges(){
        assertTrue(ReleaseInfo.checkDue(0,1000));assertFalse(ReleaseInfo.checkDue(1000,2000));
        assertTrue(ReleaseInfo.checkDue(1000,86401000));assertTrue(ReleaseInfo.checkDue(2000,1000));
    }
}

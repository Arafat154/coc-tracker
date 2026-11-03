package org.clashledger.app;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;

public class AccountBookTest {
    private JSONObject village(String tag,String name) throws Exception {
        return new JSONObject().put("playerTag",tag).put("village",name).put("boostUntil",1793678740000L)
            .put("upgrades",new JSONArray().put(new JSONObject().put("id","same-id").put("work",9000)))
            .put("boosts",new JSONObject().put("apprenticeLevel",8).put("repeatHelpers",true));
    }
    @Test public void legacyMigrationPreservesEntireSnapshot() throws Exception {
        JSONObject old=village("#ABC123","Main village");AccountBook b=new AccountBook(null,old);
        assertEquals("legacy",b.activeId());assertEquals(old.toString(),b.get("legacy").toString());
        AccountBook loaded=new AccountBook(b.json(),new JSONObject());assertEquals(8,loaded.get("legacy").getJSONObject("boosts").getInt("apprenticeLevel"));
        assertEquals(9000,loaded.get("legacy").getJSONArray("upgrades").getJSONObject(0).getInt("work"));
    }
    @Test public void switchDoesNotAlterOtherVillageWorkOrBoosts() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));String id=b.add(village("#DEF456","Alt"));
        b.select(id);JSONObject edited=b.get(id);edited.put("boostUntil",42);b.put(id,edited);
        AccountBook loaded=new AccountBook(b.json(),new JSONObject());assertEquals(id,loaded.activeId());
        assertEquals(1793678740000L,loaded.get("legacy").getLong("boostUntil"));assertEquals(42,loaded.get(id).getLong("boostUntil"));
    }
    @Test public void lookupUsesPlayerTagRatherThanDisplayName() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Same name"));String id=b.add(village("#DEF456","Same name"));
        assertEquals(id,b.findTag("#def456"));assertEquals("legacy",b.findTag("#ABC123"));assertNull(b.findTag(""));
    }
    @Test public void savingAnInactiveVillageCannotChangeSelection() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));String id=b.add(village("#DEF456","Alt"));b.select(id);
        b.put("legacy",village("#ABC123","Renamed"));assertEquals(id,b.activeId());
    }
    @Test public void deletingActiveVillageRetainsAnotherProfile() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));String id=b.add(village("#DEF456","Alt"));b.select(id);b.remove(id);
        assertEquals("legacy",b.activeId());assertEquals(1,b.ids().size());
    }
    @Test public void deletingLastProfileLeavesUsableEmptyVillage() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));b.remove("legacy");
        assertEquals(1,b.ids().size());assertEquals(0,b.get(b.activeId()).length());
    }
    @Test public void returnedSnapshotsCannotMutateSavedAccounts() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));JSONObject copy=b.get("legacy");copy.put("village","Oops");assertEquals("Main",b.get("legacy").getString("village"));
    }
    @Test public void staleSaveCannotResurrectRemovedAccount() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));b.remove("legacy");
        assertThrows(JSONException.class,()->b.put("legacy",new JSONObject()));
    }
    @Test public void corruptBookIsNotSilentlyOverwritten() {
        assertThrows(JSONException.class,()->new AccountBook("{\"version\":1,\"active\":\"missing\",\"accounts\":{}}",new JSONObject()));
    }
    @Test public void cyclingVisitsEveryVillageAndWrapsWithoutChangingSnapshots() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));b.add(village("#DEF456","Alt"));b.add(village("#GHI789","Third"));
        java.util.Map<String,String> before=new java.util.HashMap<>();for(String id:b.ids())before.put(id,b.get(id).toString());
        java.util.Set<String> visited=new java.util.HashSet<>();String start=b.activeId();
        for(int i=0;i<3;i++)visited.add(b.next());assertEquals(3,visited.size());assertEquals(start,b.activeId());
        for(String id:b.ids())assertEquals(before.get(id),b.get(id).toString());
    }
    @Test public void cyclingOneVillageIsNoOp() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));String before=b.json();b.next();assertEquals(before,b.json());
    }
    @Test public void cycleOrderSurvivesProcessRestart() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));b.add(village("#DEF456","Alt"));b.add(village("#GHI789","Third"));b.next();
        AccountBook loaded=new AccountBook(b.json(),new JSONObject());assertEquals(b.next(),loaded.next());
    }
    @Test public void cyclingSkipsRemovedVillage() throws Exception {
        AccountBook b=new AccountBook(null,village("#ABC123","Main"));String removed=b.add(village("#DEF456","Alt"));b.add(village("#GHI789","Third"));b.remove(removed);
        for(int i=0;i<4;i++)assertNotEquals(removed,b.next());
    }
}

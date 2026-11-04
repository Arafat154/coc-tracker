package org.clashledger.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class ClipboardOfferTest {
    private final String json="{\"tag\":\"#TEST123\",\"timestamp\":1793701231,\"buildings\":[]}";
    @Test public void whitespaceAndKeyOrderHaveSameIdentity()throws Exception {
        ClipboardOffer a=new ClipboardOffer(json,1793701231000L);
        ClipboardOffer b=new ClipboardOffer(" {\"buildings\":[],\"timestamp\":1793701231,\"tag\":\"#test123\"} ",1793701231000L);
        assertEquals(a.key,b.key);
        assertTrue(a.duplicate("#test123",1793701231000L));
        assertFalse(a.duplicate("#OTHER",1793701231000L));
    }
    @Test public void olderWarningOnlyAppliesToMatchingVillage()throws Exception {
        ClipboardOffer a=new ClipboardOffer(json,1793702231000L);
        assertTrue(a.older("#TEST123",1793701232000L));
        assertFalse(a.older("#OTHER",1793701232000L));
        assertFalse(a.older("#TEST123",1793701230000L));
        assertFalse(a.duplicate("#TEST123",1793701230000L));
    }
    @Test public void unrelatedInvalidAndFutureClipboardTextIsRejected(){
        for(String text:new String[]{"hello","{}","[]",json.replace("1793701231","1993701231"),json+" trailing"}){
            try{new ClipboardOffer(text,1793701231000L);fail(text);}catch(Exception expected){}
        }
    }
}

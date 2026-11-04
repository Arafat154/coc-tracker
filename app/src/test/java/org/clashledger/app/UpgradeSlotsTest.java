package org.clashledger.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
public class UpgradeSlotsTest {
    private Upgrade job(String group,boolean extra){Upgrade u=new Upgrade(group,group,group,1,3600000,1000);u.goblin=extra;return u;}
    @Test public void forgeSharesBuildersButAutoForgeDoesNot(){
        Upgrade forge=job("Forge",false),automatic=job("Auto Forge",false);
        assertFalse(UpgradeSlots.available(List.of(forge),null,"Builders",false,1,2,1000,0));
        assertTrue(UpgradeSlots.available(List.of(automatic),null,"Builders",false,1,2,1000,0));
        assertEquals(3600000,UpgradeSummary.next(List.of(forge),"Builders",1000,0));
        assertEquals(0,UpgradeSummary.next(List.of(automatic),"Builders",1000,0));
    }
    @Test public void goblinResearchDoesNotConsumeNormalLabSlot(){
        Upgrade normal=job("Laboratory",false),goblin=job("Laboratory",true);
        assertTrue(UpgradeSlots.available(List.of(normal),null,"Laboratory",true,6,2,1000,0));
        assertTrue(UpgradeSlots.available(List.of(goblin),null,"Laboratory",false,6,2,1000,0));
        assertFalse(UpgradeSlots.available(List.of(normal,goblin),null,"Laboratory",true,6,2,1000,0));
        assertFalse(UpgradeSlots.available(List.of(),null,"Pets",true,6,2,1000,0));
    }
    @Test public void editingOwnJobKeepsItsSlot(){Upgrade u=job("Builders",false);assertTrue(UpgradeSlots.available(List.of(u),u,"Builders",false,1,2,1000,0));}
    @Test public void forgePotionAndAutoForgeUseDifferentRates(){
        Upgrade normal=job("Forge",false),automatic=job("Auto Forge",false);
        assertEquals(360000,normal.wallRemaining(1000,3601000));
        assertEquals(3600000,automatic.wallRemaining(1000,3601000));
        normal.boosts.ends[3]=3601000;assertEquals(3600000,normal.wallRemaining(1000,0));
    }
    @Test public void completedForgeReleasesBuilder(){Upgrade u=job("Forge",false);assertTrue(UpgradeSlots.available(List.of(u),null,"Builders",false,1,2,3601000,0));}
}

package org.clashledger.app;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class UpgradeSummaryTest {
    @Test public void nextLabIncludesPotionAndHelperEffects() {
        Upgrade lab=new Upgrade("a","Research","Laboratory",2,100000,1000);lab.boosts.ends[0]=11000;lab.helperLevel=8;lab.helperEnd=11000;
        assertEquals(3125,UpgradeSummary.next(Arrays.asList(lab),"Laboratory",1000,0));
    }
    @Test public void completedAndGoblinResearchDoNotOccupyNormalSlot() {
        Upgrade done=new Upgrade("a","Done","Laboratory",2,0,1000),goblin=new Upgrade("b","Extra","Laboratory",2,9000,1000);goblin.goblin=true;
        assertEquals(0,UpgradeSummary.next(Arrays.asList(done,goblin),"Laboratory",1000,0));
    }
    @Test public void builderBaseUsesStarLabSeparately() {
        Upgrade home=new Upgrade("a","Home","Laboratory",2,10000,1000),base=new Upgrade("b","Base","Star Laboratory",2,5000,1000);
        assertEquals(5000,UpgradeSummary.next(Arrays.asList(home,base),"Star Laboratory",1000,0));
    }
}

package org.clashledger.app;
import java.util.List;

/** Normal builders are shared with Forge; Goblin jobs use their own slot. */
public final class UpgradeSlots {
    public static boolean available(List<Upgrade> upgrades,Upgrade excluded,String group,boolean goblin,int builders,int baseBuilders,long now,long boostUntil){
        if(goblin&&!group.equals("Builders")&&!group.equals("Laboratory"))return false;
        int same=0,home=0;
        for(Upgrade u:upgrades)if(u!=excluded&&u.remaining(now,boostUntil)>0){
            if(u.group.equals(group)&&u.goblin==goblin)same++;
            if(u.occupiesBuilder()&&!u.goblin)home++;
        }
        if(goblin)return same<1;
        if(group.equals("Builders"))return home<builders;
        if(group.equals("Forge"))return home<builders&&same<4;
        return same<(group.equals("Builder Base")?baseBuilders:1);
    }
    private UpgradeSlots(){}
}

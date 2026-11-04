package org.clashledger.app;

import java.util.List;
public final class UpgradeSummary {
    /** Earliest normal-slot completion; temporary Goblin jobs do not occupy the normal lab. */
    public static long next(List<Upgrade> upgrades,String group,long now,long boostUntil) {
        long next=Long.MAX_VALUE;
        for(Upgrade u:upgrades)if((u.group.equals(group)||(group.equals("Builders")&&u.occupiesBuilder()))&&!u.goblin) {
            long left=u.wallRemaining(now,boostUntil);if(left>0)next=Math.min(next,left);
        }
        return next==Long.MAX_VALUE?0:next;
    }
    private UpgradeSummary() { }
}

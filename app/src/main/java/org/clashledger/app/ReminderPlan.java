package org.clashledger.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure scheduling decisions: only timers armed while running can produce completion alerts. */
public final class ReminderPlan {
    public static final class Event {
        public final String key, title, detail;
        public final long remaining;
        public Event(String key, String title, String detail, long remaining) {
            this.key=key; this.title=title; this.detail=detail; this.remaining=remaining;
        }
    }
    public final Map<String,Long> armed = new HashMap<>();
    public final List<Event> due = new ArrayList<>();
    public long next = Long.MAX_VALUE;
    public ReminderPlan(List<Event> current, Map<String,Long> previous, long now, boolean deliverDue) {
        for (Event event : current) {
            if (event.remaining > 0) {
                long at=now+event.remaining;
                armed.put(event.key,at); next=Math.min(next,at);
            } else if (deliverDue && previous.containsKey(event.key) && previous.get(event.key)<=now) {
                due.add(event);
            }
        }
    }
    public static List<Event> events(List<Upgrade> upgrades, String village, long boostUntil, long now, Set<String> categories, boolean potion) {
        List<Event> result=new ArrayList<>();
        for(Upgrade u:upgrades) if(categories.contains(u.group)) {
            String key=village+"|"+u.id+"|"+u.group+"|"+u.name+"|"+u.level;
            result.add(new Event(key,u.name+" is ready",u.group+" · Level "+u.level+(u.goblin?" · Goblin":""),u.wallRemaining(now,boostUntil)));
        }
        if(potion && boostUntil>0) result.add(new Event(village+"|potion|"+boostUntil,"Builder Potion finished","Your builders are back to normal speed.",Math.max(0,boostUntil-now)));
        return result;
    }
}

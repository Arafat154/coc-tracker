package org.clashledger.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ReminderSettings {
    public boolean enabled, vibration, potion;
    public String mode;
    public int leadMinutes;
    public Set<String> categories;
    private final SharedPreferences prefs;
    public ReminderSettings(Context context) {
        prefs=context.getSharedPreferences("reminders",Context.MODE_PRIVATE);
        enabled=prefs.getBoolean("enabled",false);
        vibration=prefs.getBoolean("vibration",true);
        potion=prefs.getBoolean("potion",true);
        leadMinutes=Math.max(0,Math.min(1440,prefs.getInt("leadMinutes",0)));
        mode=prefs.getString("mode","notification");
        categories=new HashSet<>(prefs.getStringSet("categories",new HashSet<>(Arrays.asList(Upgrade.GROUPS))));
    }
    public void save() {
        prefs.edit().putBoolean("enabled",enabled).putBoolean("vibration",vibration).putBoolean("potion",potion)
            .putInt("leadMinutes",leadMinutes).putString("mode",mode).putStringSet("categories",new HashSet<>(categories)).apply();
    }
    public String soundUri() { return prefs.getString("sound_"+mode,""); }
    public void setSoundUri(String value) { prefs.edit().putString("sound_"+mode,value).apply(); }
    public String channelId() { return "upgrade_"+mode+(vibration?"_vibrate":"_still")+"_v1"+(soundUri().isEmpty()?"":"_"+java.util.UUID.nameUUIDFromBytes(soundUri().getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
}

package org.clashledger.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Reads only after opt-in, once on foreground entry. No monitor, service, or automatic import. */
public final class ClipboardSuggestions {
    public static SharedPreferences preferences(Context c){return c.getSharedPreferences("convenience",0);}
    public static void check(Activity activity,java.util.function.BooleanSupplier stillCurrent,java.util.function.Consumer<String> review){
        SharedPreferences prefs=preferences(activity);
        if(!prefs.getBoolean("suggestClipboard",false)||!activity.hasWindowFocus())return;
        String text;
        try {
            ClipData clip=activity.getSystemService(ClipboardManager.class).getPrimaryClip();
            if(clip==null||clip.getItemCount()==0)return;
            CharSequence value=clip.getItemAt(0).getText();
            if(value==null||value.length()>1_000_000)return;
            text=value.toString();
        }catch(SecurityException e){return;}
        new Thread(()->{
            ClipboardOffer offer;
            try{offer=new ClipboardOffer(text,System.currentTimeMillis());}
            catch(Exception|StackOverflowError invalid){return;}
            activity.runOnUiThread(()->{
                if(activity.isFinishing()||activity.isDestroyed()||!activity.hasWindowFocus()||!stillCurrent.getAsBoolean()||!prefs.getBoolean("suggestClipboard",false))return;
                Set<String> ignored=prefs.getStringSet("dismissedExports",java.util.Collections.emptySet());
                if(ignored.contains(offer.key))return;
                String name=offer.village.tag;boolean known=false,older=false;
                for(Ledger saved:Ledger.all(activity))if(offer.village.tag.equalsIgnoreCase(saved.playerTag)){
                    if(offer.duplicate(saved.playerTag,saved.importedAt))return;
                    known=true;name=saved.village+" ("+saved.playerTag+")";older=offer.older(saved.playerTag,saved.importedAt);break;
                }
                String message=(known?"Update saved village: ":"Add a new village: ")+name+"\n\n"+(older?"This export is OLDER than your saved village. Review carefully before replacing its timers.\n\n":"")+"Review the import before applying it. Other villages are kept.";
                new AlertDialog.Builder(activity).setTitle(older?"Older village export found":"Village export found")
                    .setMessage(message).setPositiveButton("Review",(d,w)->review.accept(text))
                    .setNegativeButton("Dismiss",(d,w)->dismiss(prefs,offer.key))
                    .setOnCancelListener(d->dismiss(prefs,offer.key)).show();
            });
        },"clipboard-export-review").start();
    }
    private static void dismiss(SharedPreferences prefs,String key){
        Set<String> ignored=new HashSet<>(prefs.getStringSet("dismissedExports",java.util.Collections.emptySet()));
        if(ignored.size()>=64)ignored.clear();ignored.add(key);prefs.edit().putStringSet("dismissedExports",ignored).apply();
    }
    private ClipboardSuggestions(){}
}

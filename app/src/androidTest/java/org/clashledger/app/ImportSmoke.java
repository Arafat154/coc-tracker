package org.clashledger.app;

import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

/** Exercises the real clipboard and dialogs without changing saved villages. */
public final class ImportSmoke extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            String fixture;
            try(java.io.InputStream in=getContext().getAssets().open("reported-export.json")){fixture=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
            startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            final String json=fixture;
            runOnMainSync(()->getTargetContext().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Test export",json)));
            click("Import village JSON");click("Paste from clipboard");click("Preview import");
            if(find("Review village import")==null)throw new AssertionError("Import preview did not open");
            click("Back");click("Cancel");
            runOnMainSync(()->{
                android.text.SpannableString rich=new android.text.SpannableString(json);
                rich.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,rich.length(),0);
                getTargetContext().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Test export",rich));
                ExportEditText input=new ExportEditText(getTargetContext());input.setText("");input.setSelection(0);
                input.onTextContextMenuItem(android.R.id.paste);
                // The emulator host clipboard bridge can add a trailing newline. JSON content must match.
                if(!json.trim().equals(input.exportText().trim()))throw new AssertionError("Native paste changed JSON: length="+input.exportText().length()+", expected="+json.length()+", error="+input.getError());
                if(input.getText().getSpans(0,input.length(),android.text.style.StyleSpan.class).length!=0)throw new AssertionError("Rich clipboard span retained");
                if(input.length()>4096)throw new AssertionError("Large export reached the visible text layout");
                input.loadExport("");
                android.view.inputmethod.InputConnection connection=input.onCreateInputConnection(new android.view.inputmethod.EditorInfo());
                if(connection==null)throw new AssertionError("Missing input connection");
                connection.commitText(json,1);
                if(!json.equals(input.exportText())||input.length()>4096)throw new AssertionError("Keyboard paste was not preserved safely");
                input.loadExport("");if(!input.exportText().isEmpty())throw new AssertionError("Clear retained previous export");
                android.content.SharedPreferences accounts=getTargetContext().getSharedPreferences("village_accounts",0);
                String before=accounts.getString("book",null);
                try {
                    org.json.JSONObject data=new org.json.JSONObject(json);data.put("tag","#FORGETEST");
                    VillageImport imported=new VillageImport(data.toString(),new org.json.JSONObject(),System.currentTimeMillis());
                    Ledger village=Ledger.create(getTargetContext()).importVillage(imported);
                    Upgrade forge=new Upgrade("manual-forge","Capital Gold","Forge",1,3600000,System.currentTimeMillis());
                    forge.boosts=village.boosts;village.upgrades.add(forge);village.save();
                    Ledger refreshed=village.importVillage(imported);
                    Upgrade retained=refreshed.upgrades.stream().filter(u->u.id.equals("manual-forge")).findFirst().orElseThrow(()->new AssertionError("Forge lost on re-import"));
                    if(retained.work>3600000||retained.work<=0||retained.boosts!=refreshed.boosts)throw new AssertionError("Forge work or boost link reset");
                    if(new Ledger(getTargetContext()).upgrades.stream().noneMatch(u->u.id.equals("manual-forge")))throw new AssertionError("Forge not persisted");
                }catch(org.json.JSONException error){throw new AssertionError(error);}
                finally{android.content.SharedPreferences.Editor edit=accounts.edit();if(before==null)edit.remove("book");else edit.putString("book",before);if(!edit.commit())throw new AssertionError("Failed to restore saved villages");}
                Upgrade home=new Upgrade("h","Cannon","Builders",1,60000,0),base=new Upgrade("b","Cannon","Builder Base",1,60000,0);
                if(Artwork.forUpgrade(getTargetContext(),home)!=R.drawable.art_1000008||Artwork.forUpgrade(getTargetContext(),base)!=R.drawable.art_1000044)throw new AssertionError("Cannon artwork is not village-specific");
                BoostState boosts=new BoostState();boosts.ends[2]=10000;
                if(Artwork.boost(boosts,0,1000)!=R.drawable.art_builder_bite)throw new AssertionError("Builder Bite artwork mismatch");
            });
            result.putString("stream","Clipboard paste/preview, native/keyboard paste, Forge persistence and village/boost artwork passed; saved villages untouched.\n");finish(-1,result);
        }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}
    }
    private AccessibilityNodeInfo find(String text){return findNode(getUiAutomation().getRootInActiveWindow(),text);}
    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo n,String text){
        if(n==null)return null;
        if(n.getText()!=null&&text.equalsIgnoreCase(n.getText().toString()))return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=findNode(n.getChild(i),text);if(found!=null)return found;}
        return null;
    }
    private void click(String text)throws Exception{
        AccessibilityNodeInfo node=null;
        for(int i=0;i<30&&node==null;i++){Thread.sleep(100);node=find(text);}
        if(node==null||!node.performAction(AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("Missing button: "+text);
        Thread.sleep(500);waitForIdleSync();
    }
}

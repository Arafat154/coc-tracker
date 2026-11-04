package org.clashledger.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Anonymous release requests contain the app version, never village data. */
public final class AppUpdates {
    private static final String RELEASES="https://github.com/chitti-a7r4/coc-tracker/releases";
    private static boolean automaticInFlight;
    private static SharedPreferences state(Context c){return c.getSharedPreferences("app_updates",0);}
    private static String installed(Context c){
        try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}
        catch(android.content.pm.PackageManager.NameNotFoundException e){return "0.0.0";}
    }
    public static String available(Context c){
        String latest=state(c).getString("available","");
        try{return ReleaseVersion.compare(latest,installed(c))>0?latest:"";}catch(IllegalArgumentException e){return "";}
    }
    private static void remember(Context c,ReleaseInfo release){
        state(c).edit().putString("available",release.hasApk?release.version:"").putLong("lastAttempt",System.currentTimeMillis()).apply();
    }
    public static synchronized void checkAutomatically(Activity activity,Runnable refresh){
        if(automaticInFlight||!ClipboardSuggestions.preferences(activity).getBoolean("autoUpdates",true)||!ReleaseInfo.checkDue(state(activity).getLong("lastAttempt",0),System.currentTimeMillis()))return;
        automaticInFlight=true;state(activity).edit().putLong("lastAttempt",System.currentTimeMillis()).apply();
        Context app=activity.getApplicationContext();String current=installed(app);
        new Thread(()->{
            try{remember(app,fetch(current));}catch(Exception ignored){/* Offline opens remain quiet. Manual checking is always available. */}
            finally{synchronized(AppUpdates.class){automaticInFlight=false;}}
            activity.runOnUiThread(()->{if(!activity.isFinishing()&&!activity.isDestroyed())refresh.run();});
        },"automatic-release-check").start();
    }
    public static void openAvailable(Activity activity){String version=available(activity);open(activity,RELEASES+(version.isEmpty()?"/latest":"/tag/"+Uri.encode(version)));}
    public static void show(Activity activity) {
        String current=installed(activity),latest=available(activity);
        new AlertDialog.Builder(activity).setTitle(latest.isEmpty()?"App updates · "+current:"Update available · "+latest)
            .setMessage("Installed: "+current+"\n\nThe app can check GitHub once a day when opened and show an update notice. Change this under Import & update preferences. Requests contain no village data.\n\nDownload the APK from the release page and install it over this app to keep your villages. Android will ask before installing.")
            .setPositiveButton("Check now",(d,w)->check(activity,current)).setNeutralButton("Open release",(d,w)->openAvailable(activity)).setNegativeButton("Close",null).show();
    }
    private static ReleaseInfo fetch(String current)throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL("https://api.github.com/repos/chitti-a7r4/coc-tracker/releases/latest").openConnection();
        try {
            connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept","application/vnd.github+json");connection.setRequestProperty("User-Agent","Clash-Ledger/"+current);
            int status=connection.getResponseCode();
            if(status!=200)throw new java.io.IOException(status==403||status==429?"GitHub is rate limiting requests. Try again later.":status==404?"No published release found.":"GitHub returned HTTP "+status);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>1_000_000)throw new java.io.IOException("Release response is too large");bytes.write(buffer,0,n);}}
            return new ReleaseInfo(new JSONObject(bytes.toString("UTF-8")));
        }finally{connection.disconnect();}
    }
    private static void check(Activity activity,String current) {
        AlertDialog progress=new AlertDialog.Builder(activity).setTitle("Checking for updates…").setMessage("Contacting GitHub").setNegativeButton("Cancel",null).show();
        new Thread(()->{
            String message,title,link=RELEASES+"/latest";
            try {
                ReleaseInfo release=fetch(current);remember(activity,release);
                title=release.newerThan(current)?"Update available · "+release.version:!release.hasApk?"No APK available in latest release":"You're up to date";
                message="Installed: "+current+"\nLatest published release: "+release.version+(release.newerThan(current)?"\n\nOpen the release and download its APK from Assets. Install over your existing app to keep saved villages.":!release.hasApk?"\n\nThe publisher has not attached an APK yet.":"");
                link=RELEASES+"/tag/"+Uri.encode(release.version);
            }catch(Exception e){title="Couldn't check for updates";message="Check your connection and try again, or open Releases in your browser.\n\n"+(e.getMessage()==null?"Request failed":e.getMessage());}
            final String heading=title,body=message,url=link;
            activity.runOnUiThread(()->{if(activity.isFinishing()||activity.isDestroyed())return;if(activity instanceof MainActivity)((MainActivity)activity).refreshUpdateNotice();if(!progress.isShowing())return;progress.dismiss();new AlertDialog.Builder(activity).setTitle(heading).setMessage(body).setPositiveButton("Open release",(d,w)->open(activity,url)).setNegativeButton("Close",null).show();});
        },"release-check").start();
    }
    private static void open(Activity activity,String url) {
        try {activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
        catch(android.content.ActivityNotFoundException e){new AlertDialog.Builder(activity).setMessage("Open this address in a browser:\n"+url).setPositiveButton("Close",null).show();}
    }
    private AppUpdates() { }
}

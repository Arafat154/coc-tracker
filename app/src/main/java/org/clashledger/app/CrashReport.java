package org.clashledger.app;

import android.content.Context;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import java.io.File;
import java.nio.charset.StandardCharsets;

/** Local, opt-in sharing of stack frames only; never record exports or exception messages. */
public final class CrashReport {
    private static boolean installed;
    private static File file(Context c){return new File(c.getFilesDir(),"last-crash.txt");}
    public static synchronized void install(Context context){
        if(installed)return;installed=true;
        Context c=context.getApplicationContext();Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try{
                StringBuilder report=new StringBuilder("Clash Ledger ").append(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName)
                    .append("\nDevice: ").append(android.os.Build.MANUFACTURER).append(' ').append(android.os.Build.MODEL)
                    .append("\nAndroid: ").append(android.os.Build.VERSION.RELEASE).append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\nTime: ").append(new java.util.Date()).append('\n');
                Throwable cause=error;
                for(int depth=0;cause!=null&&depth<8;depth++,cause=cause.getCause()){
                    report.append(cause.getClass().getName()).append('\n');int count=0;
                    for(StackTraceElement frame:cause.getStackTrace()){if(count++>=64)break;report.append("  at ").append(frame).append('\n');}
                }
                try(java.io.FileOutputStream out=new java.io.FileOutputStream(file(c))){out.write(report.toString().getBytes(StandardCharsets.UTF_8));}
            }catch(Throwable ignored){/* Preserve Android's normal crash handling even if storage is unavailable. */}
            if(previous!=null)previous.uncaughtException(thread,error);
            else {android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}
        });
    }
    public static void show(android.app.Activity activity){
        String report;
        try{report=new String(java.nio.file.Files.readAllBytes(file(activity).toPath()),StandardCharsets.UTF_8);}
        catch(java.io.IOException e){new AlertDialog.Builder(activity).setTitle("Crash diagnostics").setMessage("No crash report saved. If the app closes unexpectedly, reopen it and check here. Reports stay on your device and exclude village exports and player tags.").setPositiveButton("Close",null).show();return;}
        final String text=report;
        new AlertDialog.Builder(activity).setTitle("Last crash · stored on this device").setMessage(text)
            .setPositiveButton("Copy report",(d,w)->activity.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Clash Ledger crash",text)))
            .setNeutralButton("Delete report",(d,w)->file(activity).delete()).setNegativeButton("Close",null).show();
    }
    private CrashReport(){}
}

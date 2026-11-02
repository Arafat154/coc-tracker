package org.clashledger.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

public final class MainActivity extends Activity {
    private final int gold = Color.rgb(242,196,108), ink = Color.rgb(242,245,247), muted = Color.rgb(144,158,174);
    private final int canvas = Color.rgb(12,18,27), surface = Color.rgb(22,31,44), line = Color.rgb(41,54,72), green = Color.rgb(143,219,166);
    private Ledger ledger;
    private LinearLayout content, timerList, tabs;
    private TextView boostStatus, boostCaption, overview, nextBuilder, readyCount, reminderStatusView;
    private Typeface displayFace;
    private boolean builderBase;
    private final java.util.List<TimerBinding> bindings = new java.util.ArrayList<>();
    private static final class TimerBinding {
        final Upgrade upgrade; final TextView remaining, finish; final ProgressBar progress;
        TimerBinding(Upgrade u, TextView r, TextView f, ProgressBar p) { upgrade=u; remaining=r; finish=f; progress=p; }
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { public void run() { updateTimers(); handler.postDelayed(this, 1000); } };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        displayFace = Typeface.createFromAsset(getAssets(), "fonts/BarlowCondensed-Bold.ttf");
        if (state != null) builderBase = state.getBoolean("builderBase");
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(canvas); getWindow().setNavigationBarColor(canvas);
        ledger = new Ledger(this);
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("builderBase", builderBase); super.onSaveInstanceState(state); }
    @Override protected void onResume() { super.onResume(); ledger = new Ledger(this); build(); handler.removeCallbacks(tick); handler.post(tick); UpgradeWidget.refresh(this); Reminders.sync(this,true); updateReminderStatus(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable bg(int color, int radius) { GradientDrawable b = new GradientDrawable(); b.setColor(color); b.setCornerRadius(dp(radius)); return b; }
    private GradientDrawable panel(int first, int second, int border, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{first, second});
        d.setCornerRadius(dp(radius)); d.setStroke(dp(1),border); return d;
    }
    private void touch(View view, android.graphics.drawable.Drawable background, Runnable action) {
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x25FFFFFF), background, null));
        view.setOnClickListener(v -> action.run()); view.setFocusable(true);
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("tnum"); t.setIncludeFontPadding(false);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return t;
    }
    private TextView title(String value, int size) { TextView t=text(value,size,ink,true); t.setTypeface(displayFace); return t; }
    private TextView label(String value, int color) { TextView t=text(value,10,color,true); t.setLetterSpacing(0.13f); return t; }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l=new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int width, int height) { return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height)); }
    private void space(LinearLayout parent,int height) { parent.addView(new View(this),lp(1,height)); }
    private ImageView art(int resource, int padding) { ImageView i=new ImageView(this); i.setImageResource(resource); i.setScaleType(ImageView.ScaleType.FIT_CENTER); i.setPadding(dp(padding),dp(padding),dp(padding),dp(padding)); i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); return i; }
    private ImageView iconButton(int resource,String description,Runnable action) {
        ImageView image=art(resource,13); image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES); image.setContentDescription(description);
        touch(image,panel(surface,surface,line,14),action); return image;
    }
    private Button button(String value, Runnable action) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setTextColor(gold); b.setTextSize(13);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); b.setMinHeight(dp(46)); b.setMinimumHeight(dp(46)); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(dp(14),dp(9),dp(14),dp(9)); b.setStateListAnimator(null);
        touch(b,panel(0xFF243044,0xFF1B2637,line,12),action); return b;
    }
    private Button primary(String value, Runnable action) {
        Button b=button(value,action); b.setTextColor(0xFF302415);
        touch(b,panel(0xFFF7D88F,0xFFE7B65A,0xFFFFD990,12),action); return b;
    }
    private void build() {
        LinearLayout root=column(); root.setBackgroundColor(canvas);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setVerticalScrollBarEnabled(false);
        content=column(); content.setPadding(dp(20),dp(14),dp(20),dp(24)); scroll.addView(content); root.addView(scroll,lp(-1,-1));
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets) -> {
            root.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        root.requestApplyInsets();

        LinearLayout brand=horizontal();
        ImageView mark=art(R.drawable.ic_shield,7); mark.setBackground(panel(0xFF423925,0xFF242A2B,0xFF665438,12)); brand.addView(mark,lp(38,38));
        LinearLayout wordmark=column(); wordmark.setPadding(dp(10),0,0,0); wordmark.addView(title("CLASH LEDGER",22));
        wordmark.addView(label("YOUR VILLAGE COMPANION",muted)); brand.addView(wordmark,new LinearLayout.LayoutParams(0,-2,1));
        brand.addView(iconButton(R.drawable.ic_widget,"Add home screen widget",this::pinWidget),lp(44,44));
        LinearLayout.LayoutParams settingsParams=lp(44,44); settingsParams.leftMargin=dp(8);
        brand.addView(iconButton(R.drawable.ic_settings,"Settings",this::settingsMenu),settingsParams); content.addView(brand);
        space(content,17);
        LinearLayout heading=horizontal(); heading.addView(label("VILLAGE OVERVIEW",muted),new LinearLayout.LayoutParams(0,-2,1));
        heading.addView(label("●  OFFLINE",green)); content.addView(heading); space(content,12);

        LinearLayout hero=column(); hero.setPadding(dp(16),dp(12),dp(12),dp(12));
        hero.setBackground(panel(0xFF293649,0xFF182B2B,0xFF40534E,22));
        LinearLayout heroTop=horizontal(); LinearLayout heroCopy=column();
        heroCopy.addView(label("HOME VILLAGE",gold)); space(heroCopy,7);
        TextView village=title(ledger.townHall>0 ? "Town Hall "+ledger.townHall : "Your village",29); heroCopy.addView(village);
        space(heroCopy,5); TextView player=text(ledger.village,12,0xFFCDD6DC,true); player.setMaxLines(1); player.setEllipsize(android.text.TextUtils.TruncateAt.END); heroCopy.addView(player);
        space(heroCopy,10); TextView local=text(ledger.importedAt>0 ? "Export · " + DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(ledger.importedAt)) : "Import your village to get started",10,0xFF9DAFB8,false); heroCopy.addView(local);
        heroTop.addView(heroCopy,new LinearLayout.LayoutParams(0,-2,1));
        ImageView town=art(Artwork.townHall(this,ledger.townHall),0); heroTop.addView(town,lp(100,102)); hero.addView(heroTop);
        space(hero,9);
        Button importer=primary("Import village JSON",this::importJson); importer.setCompoundDrawablePadding(dp(8));
        android.graphics.drawable.Drawable download=getDrawable(R.drawable.ic_import); download.setTint(0xFF49351A); download.setBounds(0,0,dp(18),dp(18)); importer.setCompoundDrawables(download,null,null,null);
        hero.addView(importer,lp(-1,42)); content.addView(hero);
        space(content,12);

        LinearLayout metrics=horizontal();
        overview=metric(metrics,"BUILDERS",R.drawable.ic_hammer,gold);
        nextBuilder=metric(metrics,"NEXT FREE",R.drawable.ic_clock,green);
        readyCount=metric(metrics,"READY",R.drawable.ic_shield,0xFFADA7FF);
        content.addView(metrics); space(content,16);

        LinearLayout boost=horizontal(); boost.setPadding(dp(10),dp(9),dp(12),dp(9)); boost.setBackground(panel(0xFF302B47,0xFF242238,0xFF524563,18));
        boost.addView(art(R.drawable.art_builder_potion,0),lp(58,70));
        LinearLayout potionCopy=column(); potionCopy.setPadding(dp(10),0,dp(6),0); potionCopy.addView(label("BUILDER POTION",0xFFCEB3F4)); space(potionCopy,5);
        potionCopy.addView(title("10× builder speed",22)); space(potionCopy,5);
        boostStatus=text("",12,0xFFD6C4ED,true); potionCopy.addView(boostStatus);
        boostCaption=text("",10,muted,false); boostCaption.setVisibility(View.GONE); potionCopy.addView(boostCaption);
        boost.addView(potionCopy,new LinearLayout.LayoutParams(0,-2,1));
        boost.addView(iconButton(R.drawable.ic_plus,"Record a Builder Potion",this::potionDialog),lp(44,44));
        boost.setOnClickListener(v -> boostOptions()); content.addView(boost);
        space(content,16);

        tabs=horizontal(); tabs.setPadding(dp(4),dp(4),dp(4),dp(4)); tabs.setBackground(bg(0xFF161F2C,14));
        content.addView(tabs,lp(-1,48)); renderTabs(); space(content,12);
        LinearLayout upgradeHeading=horizontal(); upgradeHeading.addView(title("Upgrade queue",26),new LinearLayout.LayoutParams(0,-2,1));
        Button add=button("+ Add",()->editor(null)); upgradeHeading.addView(add,lp(76,44)); content.addView(upgradeHeading); space(content,10);
        timerList=column(); content.addView(timerList); renderTimers();
        space(content,16);
        LinearLayout widgetCard=horizontal(); widgetCard.setPadding(dp(16),dp(16),dp(14),dp(16));
        touch(widgetCard,panel(0xFF1F2B39,0xFF17232E,line,18),this::pinWidget);
        widgetCard.addView(art(R.drawable.ic_widget,3),lp(30,30));
        LinearLayout widgetCopy=column(); widgetCopy.setPadding(dp(12),0,dp(8),0); widgetCopy.addView(title("Take your village home",21)); space(widgetCopy,4);
        widgetCopy.addView(text("Add the live home screen widget",12,muted,false)); widgetCard.addView(widgetCopy,new LinearLayout.LayoutParams(0,-2,1)); widgetCard.addView(art(R.drawable.ic_arrow,0),lp(20,20)); content.addView(widgetCard);
        space(content,22);
        TextView credits=text("Built for Clashers. All your data stays on this device.\nUnofficial fan content · Artwork © Supercell",11,muted,false); credits.setGravity(Gravity.CENTER); credits.setLineSpacing(dp(5),1); content.addView(credits);
        Button about=button("About & artwork credits",this::about); LinearLayout.LayoutParams aboutParams=lp(-1,44); aboutParams.topMargin=dp(8); content.addView(about,aboutParams);
        updateTimers();
    }
    private TextView metric(LinearLayout parent,String label,int icon,int color) {
        LinearLayout cell=column(); cell.setPadding(dp(11),dp(12),dp(8),dp(12)); cell.setBackground(panel(surface,0xFF17212F,line,14));
        LinearLayout top=horizontal(); ImageView image=art(icon,0); image.setColorFilter(color); top.addView(image,lp(12,12));
        TextView name=label(label,muted); name.setTextSize(9); name.setLetterSpacing(0.04f); name.setPadding(dp(5),0,0,0); top.addView(name); cell.addView(top); space(cell,7);
        TextView value=title("—",25); value.setTextColor(color); value.setMaxLines(1); cell.addView(value);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); if(parent.getChildCount()>0)p.leftMargin=dp(8); parent.addView(cell,p); return value;
    }
    private void renderTabs() {
        tabs.removeAllViews();
        String[] labels={"Home Village","Builder Base"};
        for(int i=0;i<labels.length;i++) {
            final boolean base=i==1; boolean selected=builderBase==base;
            TextView tab=text(labels[i],13,selected?gold:muted,true); tab.setGravity(Gravity.CENTER);
            touch(tab,selected?panel(0xFF30384A,0xFF283143,0xFF485062,11):bg(Color.TRANSPARENT,11),()->{builderBase=base;renderTabs();renderTimers();updateTimers();});
            tab.setSelected(selected); tabs.addView(tab,new LinearLayout.LayoutParams(0,-1,1));
        }
    }
    private String clock(long ms) {
        long s=Math.max(0,ms/1000); return String.format(java.util.Locale.getDefault(),"%02d:%02d",s/60,s%60);
    }
    private void updateTimers() {
        if (timerList == null) return;
        long now=System.currentTimeMillis(), soonest=Long.MAX_VALUE; int ready=0;
        for(Upgrade u:ledger.upgrades) {
            long left=u.wallRemaining(now,ledger.boostUntil);
            if(left<=0) ready++; else if(u.isBuilder()) soonest=Math.min(soonest,left);
        }
        overview.setText(ledger.busy(now)+" / "+ledger.builders+(ledger.goblins(now)>0?" + G":""));
        nextBuilder.setText(soonest==Long.MAX_VALUE?"Available":Upgrade.duration(soonest));
        readyCount.setText(String.format(java.util.Locale.getDefault(),"%02d",ready));
        boolean boosted=ledger.boostUntil>now;
        boostStatus.setText(boosted?"●  ACTIVE  ·  "+clock(ledger.boostUntil-now)+" left":"No active boost");
        boostCaption.setText(boosted?"Tap to adjust your boost":"Import a boost or record a potion");
        for(TimerBinding b:bindings) {
            Upgrade u=b.upgrade; long left=u.wallRemaining(now,ledger.boostUntil);
            b.remaining.setText(left>0?Upgrade.duration(left):"Ready to collect");
            b.remaining.setTextColor(left>0?ink:green);
            b.finish.setText(left>0?"↗ "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(now+left))+(u.isBuilder()&&boosted?"  ·  10×":""):"Upgrade complete");
            b.progress.setProgress(u.progress(now,ledger.boostUntil));
        }
    }
    private int groupColor(String group) { return group.equals("Laboratory")||group.equals("Star Laboratory")?0xFFB8A1ED:group.equals("Pets")?0xFF81C9DE:gold; }
    private void renderTimers() {
        bindings.clear(); timerList.removeAllViews();
        long now=System.currentTimeMillis();
        boolean hasAny=false;
        for(String group:Upgrade.GROUPS) {
            boolean base=group.equals("Builder Base")||group.equals("Star Laboratory"); if(base!=builderBase)continue;
            java.util.List<Upgrade> list=new java.util.ArrayList<>();
            for(Upgrade u:ledger.upgrades)if(u.group.equals(group))list.add(u);
            list.sort(java.util.Comparator.comparingLong(u->u.wallRemaining(now,ledger.boostUntil)));
            if(list.isEmpty())continue; hasAny=true;
            LinearLayout section=horizontal(); TextView sectionName=label(group.toUpperCase(java.util.Locale.ROOT),groupColor(group)); section.addView(sectionName,new LinearLayout.LayoutParams(0,-2,1));
            TextView count=text(String.format(java.util.Locale.getDefault(),"%02d",list.size()),11,muted,true); section.addView(count); section.setPadding(0,dp(9),dp(2),dp(10)); timerList.addView(section);
            for(Upgrade u:list) {
                LinearLayout row=column(); row.setPadding(dp(12),dp(12),dp(12),dp(12));
                touch(row,panel(0xFF1C2737,0xFF17212F,line,17),()->editor(u));
                LinearLayout upper=horizontal();
                FrameLayout portrait=new FrameLayout(this); portrait.setBackground(panel(0xFF344053,0xFF242E3F,0xFF40506A,13));
                portrait.addView(art(Artwork.forName(this,u.name),4),new FrameLayout.LayoutParams(-1,-1)); upper.addView(portrait,lp(60,66));
                LinearLayout copy=column(); copy.setPadding(dp(12),0,0,0);
                LinearLayout nameRow=horizontal(); TextView name=text(u.name,15,ink,true); name.setMaxLines(2); nameRow.addView(name,new LinearLayout.LayoutParams(0,-2,1));
                nameRow.addView(art(R.drawable.ic_arrow,2),lp(16,16)); copy.addView(nameRow); space(copy,5);
                TextView level=label("LEVEL "+Math.max(0,u.level-1)+"  →  "+u.level+(u.goblin?"   • GOBLIN":""),u.goblin?green:muted); level.setTextSize(9); level.setLetterSpacing(0.04f); copy.addView(level); space(copy,7);
                TextView remaining=text("",19,ink,true); copy.addView(remaining); upper.addView(copy,new LinearLayout.LayoutParams(0,-2,1)); row.addView(upper); space(row,10);
                ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); bar.setProgressDrawable(getDrawable(R.drawable.progress)); bar.setProgressTintList(ColorStateList.valueOf(groupColor(group))); bar.setMax(1000); row.addView(bar,lp(-1,4));
                space(row,7); TextView finish=text("",10,muted,false); row.addView(finish); bindings.add(new TimerBinding(u,remaining,finish,bar));
                LinearLayout.LayoutParams p=lp(-1,-2); p.bottomMargin=dp(9); timerList.addView(row,p);
                row.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Remove "+u.name+"?").setMessage("This removes only the local timer.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{ledger.upgrades.remove(u);changed();}).show();return true;});
            }
        }
        if(!hasAny) {
            LinearLayout empty=column(); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(24),dp(22),dp(24),dp(24)); empty.setBackground(panel(surface,0xFF131E2C,line,18));
            empty.addView(art(Artwork.forName(this,"Barbarian King"),0),lp(82,82)); space(empty,13); empty.addView(title("A new chapter awaits",24)); space(empty,7);
            TextView hint=text("Import your village or add an upgrade.\nWe'll keep an eye on the countdown.",13,muted,false); hint.setGravity(Gravity.CENTER); hint.setLineSpacing(dp(4),1); empty.addView(hint); timerList.addView(empty);
        }
    }
    private void boostOptions() { new AlertDialog.Builder(this).setTitle("Builder Potion").setItems(new String[]{"Record a potion · +1 hour","Adjust or end boost"},(d,w)->{if(w==0)potionDialog();else adjustBoost();}).setNegativeButton("Close",null).show(); }
    private void about() {
        new AlertDialog.Builder(this).setTitle("Made for your village")
            .setMessage("Clash Ledger is an open-source, offline upgrade tracker. Import a new export when you make changes in-game.\n\nArtwork © Supercell, sourced through the ClashKing asset collection. Barlow Condensed by Jeremy Tribby (SIL Open Font License).\n\nThis material is unofficial and is not endorsed by Supercell. For more information see Supercell's Fan Content Policy.")
            .setPositiveButton("Done",null).setNeutralButton("Fan Content Policy",(d,w)->startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://supercell.com/en/fan-content-policy/")))).show();
    }
    private void changed() { ledger.save(); Reminders.sync(this,false); UpgradeWidget.refresh(this); build(); }
    private void potionDialog() {
        new AlertDialog.Builder(this).setTitle("Record a Builder Potion?").setMessage("Use a potion in Clash of Clans first. This adds one hour at 10× to your tracked builder boost; laboratory and pet timers are unaffected.")
            .setNegativeButton("Cancel",null).setPositiveButton("Record potion",(d,w) -> { long now = System.currentTimeMillis(); ledger.changeBoost(now, Math.max(now,ledger.boostUntil)+3600000L); changed(); }).show();
    }
    private LinearLayout form() { LinearLayout l = column(); l.setPadding(dp(22),dp(8),dp(22),dp(10)); return l; }
    private EditText field(LinearLayout form,String label,String value,boolean number) {
        space(form,12); form.addView(text(label,12,muted,false)); space(form,6); EditText e = new EditText(this); e.setText(value); e.setTextColor(ink); e.setSingleLine(true); e.setTextSize(15); e.setPadding(dp(12),dp(12),dp(12),dp(12)); e.setBackground(panel(surface,surface,line,10));
        e.setInputType(number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS); form.addView(e); return e;
    }
    private void adjustBoost() {
        LinearLayout f = form(); EditText minutes = field(f,"Boost minutes remaining (0 ends it)",String.valueOf(Math.max(0,(ledger.boostUntil-System.currentTimeMillis()+59999)/60000)),true);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Correct boost timer").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> { try { int m=Integer.parseInt(minutes.getText().toString()); if(m<0||m>10080) throw new IllegalArgumentException(); long now=System.currentTimeMillis(); ledger.changeBoost(now,now+m*60000L); changed(); dialog.dismiss(); } catch(IllegalArgumentException e) { minutes.setError("Enter 0–10080 minutes"); } })); dialog.show();
    }
    private void settingsMenu() {
        new AlertDialog.Builder(this).setTitle("Your village, your way")
            .setItems(new String[]{"Village settings", "Alarms & notifications", "About & artwork"},(d,w)->{if(w==0)settings();else if(w==1)reminderOptions();else about();}).setNegativeButton("Close",null).show();
    }
    private void updateReminderStatus() {
        if(reminderStatusView==null)return;
        ReminderSettings options=new ReminderSettings(this);
        String message;
        if(!options.enabled)message="Reminders are off. Turn them on to track future completions.";
        else if(!Reminders.allowed(this))message="Notifications are blocked by Android. Allow them in notification settings below.";
        else {
            android.app.NotificationChannel channel=getSystemService(android.app.NotificationManager.class).getNotificationChannel(options.channelId());
            if(channel!=null&&channel.getImportance()==android.app.NotificationManager.IMPORTANCE_NONE)message="This alert channel is blocked. Open sound & notification settings below.";
            else message=Reminders.exact(this)?"Precise timing is enabled. Alerts follow your saved upgrade timers.":"Approximate timing. Android may delay alerts; enable precise alarms below.";
        }
        reminderStatusView.setText(message);
    }
    private void saveReminderOptions(ReminderSettings options) {
        options.save(); Reminders.ensureChannel(this,options);
        if(!options.enabled)getSystemService(android.app.NotificationManager.class).cancelAll();
        Reminders.sync(this,true); updateReminderStatus();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==101) { Reminders.sync(this,false); updateReminderStatus(); }
    }
    private void reminderOptions() {
        ReminderSettings options=new ReminderSettings(this);
        LinearLayout f=form();
        f.addView(text("Know when your next builder is free.",16,ink,true)); space(f,10);
        Switch enabled=new Switch(this); enabled.setText("Enable reminders"); enabled.setTextColor(ink); enabled.setTextSize(15); enabled.setPadding(0,dp(10),0,dp(10)); enabled.setChecked(options.enabled); f.addView(enabled,lp(-1,52));
        reminderStatusView=text("",12,muted,false); reminderStatusView.setLineSpacing(dp(3),1); f.addView(reminderStatusView); space(f,18);
        f.addView(label("ALERT STYLE",gold)); space(f,8);
        Spinner mode=new Spinner(this); String[] modes={"Quiet notification", "Notification sound", "Alarm tone · up to 1 minute"}; String[] values={"silent","notification","alarm"};
        mode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,modes));
        for(int i=0;i<values.length;i++)if(values[i].equals(options.mode))mode.setSelection(i);
        f.addView(mode,lp(-1,48));
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id) {
                if(!options.mode.equals(values[position])) { options.mode=values[position]; saveReminderOptions(options); }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        CheckBox vibration=new CheckBox(this); vibration.setText("Vibrate"); vibration.setTextColor(ink); vibration.setChecked(options.vibration); f.addView(vibration,lp(-1,48));
        vibration.setOnCheckedChangeListener((b,checked)->{options.vibration=checked;saveReminderOptions(options);});
        space(f,12); f.addView(label("NOTIFY ME WHEN THESE FINISH",gold)); space(f,8);
        for(String group:Upgrade.GROUPS) {
            CheckBox category=new CheckBox(this); category.setText(group); category.setTextColor(ink); category.setChecked(options.categories.contains(group)); f.addView(category,lp(-1,44));
            category.setOnCheckedChangeListener((b,checked)->{if(checked)options.categories.add(group);else options.categories.remove(group);saveReminderOptions(options);});
        }
        CheckBox potion=new CheckBox(this); potion.setText("Builder Potion expires"); potion.setTextColor(ink); potion.setChecked(options.potion); f.addView(potion,lp(-1,44));
        potion.setOnCheckedChangeListener((b,checked)->{options.potion=checked;saveReminderOptions(options);});
        enabled.setOnCheckedChangeListener((b,checked)->{
            options.enabled=checked;saveReminderOptions(options);
            if(checked && android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},101);
        });
        space(f,12);
        f.addView(button("Precise alarm timing",()->{
            if(android.os.Build.VERSION.SDK_INT>=31) {
                try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName()))); }
                catch(android.content.ActivityNotFoundException e) { Toast.makeText(this,"Open Android app settings → Alarms & reminders",Toast.LENGTH_LONG).show(); }
            } else Toast.makeText(this,"Precise timing is supported on this Android version.",Toast.LENGTH_SHORT).show();
        }),lp(-1,48)); space(f,8);
        f.addView(button("Sound & notification settings",()->{
            Reminders.ensureChannel(this,options);
            android.content.Intent intent=new android.content.Intent(Reminders.allowed(this)?android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS:android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(android.provider.Settings.EXTRA_CHANNEL_ID,options.channelId());
            try { startActivity(intent); } catch(android.content.ActivityNotFoundException e) { Toast.makeText(this,"Open Android app settings → Notifications",Toast.LENGTH_LONG).show(); }
        }),lp(-1,48)); space(f,8);
        f.addView(button("Test reminder in 10 seconds",()->{
            if(!options.enabled) { Toast.makeText(this,"Enable reminders first.",Toast.LENGTH_SHORT).show(); return; }
            if(!Reminders.allowed(this)) { saveReminderOptions(options); Toast.makeText(this,"Allow notifications, then try the test again.",Toast.LENGTH_LONG).show(); return; }
            Reminders.test(this); Toast.makeText(this,Reminders.exact(this)?"Test scheduled for 10 seconds. You can leave the app.":"Test scheduled. Android may delay it without precise alarm access.",Toast.LENGTH_LONG).show();
        }),lp(-1,48)); space(f,14);
        f.addView(text("Alerts use your last imported or edited timers. Import again after changes in-game. Sound and vibration follow your Android volume, channel, and Do Not Disturb settings. Already-completed imports do not trigger alerts.",12,muted,false));
        ScrollView scroll=new ScrollView(this); scroll.addView(f);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Alarms & notifications").setView(scroll).setPositiveButton("Done",null).create();
        dialog.setOnDismissListener(d->reminderStatusView=null); dialog.show(); updateReminderStatus();
    }
    private void settings() {
        LinearLayout f=form(); EditText name=field(f,"Village name",ledger.village,false), count=field(f,"Available builders (1–7)",String.valueOf(ledger.builders),true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Your village").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> { try { int n=Integer.parseInt(count.getText().toString()); if(n<1||n>7) throw new IllegalArgumentException(); if(n<ledger.busy(System.currentTimeMillis())) { count.setError("Finish or remove busy builder timers first"); return; } String s=name.getText().toString().trim(); if(s.isEmpty()||s.length()>40) { name.setError("Use 1–40 characters"); return; } ledger.village=s; ledger.builders=n; changed(); dialog.dismiss(); } catch(IllegalArgumentException e) { count.setError("Enter 1–7"); } })); dialog.show();
    }
    private void editor(Upgrade existing) {
        LinearLayout f=form();
        EditText name=field(f,"Upgrade name",existing==null?"":existing.name,false);
        Spinner group=new Spinner(this); String[] groups=Upgrade.GROUPS; group.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,groups));
        if(existing!=null) for(int i=0;i<groups.length;i++) if(groups[i].equals(existing.group)) group.setSelection(i);
        f.addView(text("Category",12,muted,false)); f.addView(group);
        EditText level=field(f,"Target level",existing==null?"":String.valueOf(existing.level),true);
        long remaining=existing==null?0:existing.remaining(System.currentTimeMillis(),ledger.boostUntil);
        long mins=(remaining+59999)/60000;
        f.addView(text("Work remaining at NORMAL speed. If boosted, enter the time before the boost, or convert the remaining work. Use 0 to mark ready.",12,muted,false));
        EditText days=field(f,"Days",String.valueOf(mins/1440),true), hours=field(f,"Hours (0–23)",String.valueOf(mins/60%24),true), minutes=field(f,"Minutes (0–59)",String.valueOf(mins%60),true);
        ScrollView sc=new ScrollView(this); sc.addView(f);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Add upgrade":"Edit upgrade").setView(sc).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> {
            try {
                String n=name.getText().toString().trim(); if(n.isEmpty()||n.length()>60) { name.setError("Use 1–60 characters"); return; }
                int l=Integer.parseInt(level.getText().toString()), dd=Integer.parseInt(days.getText().toString()), hh=Integer.parseInt(hours.getText().toString()), mm=Integer.parseInt(minutes.getText().toString());
                if(l<1||l>999||dd<0||dd>365||hh<0||hh>23||mm<0||mm>59) throw new IllegalArgumentException();
                long work=(dd*1440L+hh*60L+mm)*60000L;
                String g=groups[group.getSelectedItemPosition()]; long now=System.currentTimeMillis();
                int busy=0; for(Upgrade u:ledger.upgrades) if(u!=existing&&!u.goblin&&u.group.equals(g)&&u.remaining(now,ledger.boostUntil)>0) busy++;
                int capacity=g.equals("Builders")?ledger.builders:g.equals("Builder Base")?2:1;
                if(work>0&&busy>=capacity&&(existing==null||!existing.goblin)) { Toast.makeText(this,"All "+g.toLowerCase(java.util.Locale.ROOT)+" slots are busy. Edit or remove an existing timer.",Toast.LENGTH_LONG).show(); return; }
                Upgrade u=new Upgrade(existing==null?UUID.randomUUID().toString():existing.id,n,g,l,work,now);
                if(existing!=null) { u.goblin=existing.goblin; if(work==mins*60000L && g.equals(existing.group)) { u.work=existing.remaining(now,ledger.boostUntil); u.total=existing.total; } ledger.upgrades.set(ledger.upgrades.indexOf(existing),u); } else ledger.upgrades.add(u);
                changed(); dialog.dismiss();
            } catch(IllegalArgumentException e) { Toast.makeText(this,"Enter level 1–999, days 0–365, hours 0–23 and minutes 0–59.",Toast.LENGTH_LONG).show(); }
        })); dialog.show();
    }
    private void importJson() {
        LinearLayout f = form();
        f.addView(text("In Clash of Clans: Settings → More Settings → Data Export → Copy. Paste the full export below.", 14, ink, false));
        EditText input = new EditText(this);
        input.setTextColor(ink); input.setHint("{\"tag\":\"#…\",\"timestamp\":…}"); input.setHintTextColor(muted);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setBackground(panel(surface,surface,line,12)); input.setPadding(dp(12),dp(12),dp(12),dp(12)); input.setTypeface(Typeface.MONOSPACE); input.setTextSize(12); input.setMinLines(5); input.setMaxLines(9);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1_000_001)});
        f.addView(button("Paste from clipboard", () -> {
            android.content.ClipboardManager clipboard = getSystemService(android.content.ClipboardManager.class);
            android.content.ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) { input.setError("Copy the village export first."); return; }
            CharSequence value = clip.getItemAt(0).getText();
            if (value == null) { input.setError("Clipboard has no JSON text."); return; }
            input.setText(value); input.setError(null);
        }));
        space(f,12); f.addView(input); space(f,12);
        f.addView(text("Import replaces the saved village timers and boost with this snapshot. New in-game actions require a fresh export.", 12, muted, false));
        ScrollView sc = new ScrollView(this); sc.addView(f);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Import village JSON").setView(sc).setNegativeButton("Cancel",null).setPositiveButton("Preview import",null).create();
        dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> {
            try {
                org.json.JSONObject names;
                try (java.io.InputStream stream = getAssets().open("item_names.json")) {
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = stream.read(buffer)) != -1) bytes.write(buffer,0,count);
                    names = new org.json.JSONObject(bytes.toString("UTF-8"));
                }
                long now = System.currentTimeMillis();
                VillageImport data = new VillageImport(input.getText().toString(), names, now);
                StringBuilder summary = new StringBuilder(data.tag + "\nExported " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(data.timestamp)) + "\n");
                for (String group : Upgrade.GROUPS) {
                    int count=0; for (Upgrade u : data.upgrades) if (u.group.equals(group)) count++;
                    summary.append("\n").append(group).append(": ").append(count);
                }
                summary.append("\n\nBuilder Potion: ").append(data.boostUntil>now ? "10× · " + Upgrade.duration(data.boostUntil-now) + " left" : "inactive / expired");
                if (ledger.importedAt > data.timestamp && data.tag.equals(ledger.playerTag)) summary.append("\n\nThis export is older than your last import.");
                for (String warning : data.warnings) summary.append("\n\n").append(warning);
                summary.append("\n\nReplace current timers with this export?");
                new AlertDialog.Builder(this).setTitle("Review village import").setMessage(summary.toString()).setNegativeButton("Back",null)
                    .setPositiveButton("Import village",(d,w) -> { ledger.importVillage(data); changed(); dialog.dismiss(); Toast.makeText(this,"Imported " + data.upgrades.size() + " upgrade timers",Toast.LENGTH_LONG).show(); }).show();
            } catch (org.json.JSONException | IllegalArgumentException e) {
                input.setError("Invalid village export: " + e.getMessage());
            } catch (java.io.IOException e) {
                input.setError("Could not load item names. Please reinstall the app.");
            }
        }));
        dialog.show();
    }
    private void pinWidget() {
        AppWidgetManager m=getSystemService(AppWidgetManager.class);
        if(m.isRequestPinAppWidgetSupported()) m.requestPinAppWidget(new ComponentName(this,UpgradeWidget.class),null,null);
        else new AlertDialog.Builder(this).setTitle("Add your widget").setMessage("Long-press an empty area on your home screen, choose Widgets, then select Clash Ledger.").setPositiveButton("Got it",null).show();
    }
}

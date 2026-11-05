package com.gathin.tvhome;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The PS4-style Home MainActivity shows instead of its grid while a theme sets "ps4_layout": true.
 *
 * Measured on 1920x1080 reference screenshots: the status bar centred at y=102, a row of 200 px square tiles at
 * y=209 starting at x=73 with 3 px gaps, the focused tile 343 px wide with a 4 px light border and a "Start" strip
 * below it, its name in 60 px thin type to its right. Down leaves the row: every tile goes back to its small size
 * and the focused app's actions (Start, Information) open below it, then the now-playing card at the bottom.
 * After ps4_idle_seconds untouched (10 by default; 0 never) the apps fade away to show the wallpaper and the
 * remaining pieces gather in the corners; any key brings everything back.
 */
final class Ps4Home {

    static final class App {
        final String label, pkg;
        final ComponentName component;
        final Drawable banner, icon;
        final int fill;
        final boolean leanback;

        App(String label, ComponentName component, Drawable banner, Drawable icon, int fill, boolean leanback) {
            this.label = label;
            this.component = component;
            this.pkg = component.getPackageName();
            this.banner = banner;
            this.icon = icon;
            this.fill = fill;
            this.leanback = leanback;
        }

        String version(Context ctx) {
            try {
                PackageInfo info = ctx.getPackageManager().getPackageInfo(pkg, 0);
                return info.versionName == null ? "" : info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                return "";
            }
        }
    }

    // Reference geometry (px at 1920 wide).
    private static final float ROW_X = 73, ROW_Y = 209, TILE = 200, GAP = 3, FOCUS_GAP = 8;
    private static final float BIG = 343, STRIP = 77, BORDER = 4, FOCUS_Y = 203;
    private static final float SUB_Y = 433, BAR_Y = 102, MEDIA_X = 73, MEDIA_Y = 830, MEDIA_W = 840, MEDIA_H = 190,
            MEDIA_TEXT_W = 330;
    private static final int ZONE_BAR = 0, ZONE_ROW = 1, ZONE_SUB = 2, ZONE_MEDIA = 3, ZONE_OPTIONS = 4, ZONE_MOVE = 5;
    /** Hold OK this long on a tile for its options. Timed, not by key repeats: the key helper drops those here. */
    private static final long HOLD_MS = 500;

    private final MainActivity a;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<App> apps = new ArrayList<>();
    private final List<FrameLayout> tiles = new ArrayList<>();
    private final List<View> discs = new ArrayList<>();
    private final List<View> stars = new ArrayList<>();
    private final List<TextView> startLabels = new ArrayList<>();
    private final List<String> favorites = new ArrayList<>();
    private final List<View> optionRows = new ArrayList<>();
    private final List<Runnable> optionActions = new ArrayList<>();
    private FrameLayout optionsPanel;
    private int optionFocus, moveFrom;
    private boolean okPending, okLongFired;
    /** The app opened last, shown first; lastOpenedAt is when, pausedAt when Home last left the screen. */
    private String recent;
    private long lastOpenedAt, pausedAt;
    private final Runnable okLong = () -> {
        okPending = false;
        okLongFired = true;
        openOptions();
    };
    private final List<View> barItems = new ArrayList<>();
    private final List<View> subItems = new ArrayList<>();
    private final List<View> mediaItems = new ArrayList<>();
    private final Set<String> running = new HashSet<>();
    private FrameLayout root, row, sub, mediaCard;
    private View notifItem, userItem, clock;
    private TextView title, network, libraryCount, notifCount, btCount;
    private ImageView networkIcon;
    private FrameLayout barView, idleLayer;
    private ImageView mediaArt, mediaPlay, mediaAppIcon;
    private Ps4.Wave mediaProgress;
    /** The controller our callback is on, and what the card shows (to notice a change the callback missed). */
    private MediaController registered;
    private String shown = "";
    private boolean checking;
    private int builtPalette;
    private TextView mediaApp, mediaTitle, mediaArtist, idleGreeting;
    private int focus, zone = ZONE_ROW, barFocus, subFocus, mediaFocus, swallowUp = -1;
    private boolean dirty, receiverOn, idle, resumed;
    private Ps4Sounds sounds;

    private MediaSessionManager sessions;
    private MediaController media;
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged = this::pickMedia;
    private final MediaController.Callback mediaCallback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { showMedia(); }
        @Override public void onMetadataChanged(MediaMetadata metadata) { showMedia(); }
        @Override public void onSessionDestroyed() { pickMedia(null); }
    };
    private final Runnable notificationsChanged = this::showNotificationCount;
    private final Runnable goIdle = this::sleep;

    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dirty = true;
        }
    };

    Ps4Home(MainActivity activity) {
        this.a = activity;
    }

    private int px(float ref) {
        return Ps4.px(a, ref);
    }

    // ---- apps ----------------------------------------------------------------------------------

    /** TV apps (and, with all, every launchable app), dock_apps first in their order, then by name. */
    static List<App> loadApps(Context ctx, boolean all) {
        PackageManager pm = ctx.getPackageManager();
        Config cfg = Config.load(ctx);
        Set<String> hidden = new HashSet<>(cfg.hiddenApps);
        List<App> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<ResolveInfo> found = new ArrayList<>(pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0));
        int tv = found.size();
        if (all) found.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0));
        for (int i = 0; i < found.size(); i++) {
            ResolveInfo ri = found.get(i);
            if (ri.activityInfo == null || !ri.activityInfo.exported) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName()) || hidden.contains(pkg) || !seen.add(pkg)) continue;
            String label = cfg.appLabels.containsKey(pkg) ? cfg.appLabels.get(pkg) : String.valueOf(ri.loadLabel(pm));
            Drawable icon = Ps4.sharpIcon(ctx, ri);
            int fill = Ps4.iconColor(icon, Ps4.pal(ctx).tile);
            Drawable banner = null;
            out.add(new App(Cards.cleanLabel(label, pkg), new ComponentName(pkg, ri.activityInfo.name), banner, icon, fill, i < tv));
        }
        List<String> dock = cfg.dockApps;
        Collections.sort(out, (x, y) -> {
            int dx = dock.indexOf(x.pkg), dy = dock.indexOf(y.pkg);
            if (dx >= 0 || dy >= 0) return dx < 0 ? 1 : dy < 0 ? -1 : Integer.compare(dx, dy);
            return x.label.compareToIgnoreCase(y.label);
        });
        return out;
    }

    static void launch(android.app.Activity act, App app) {
        Intent i = new Intent(Intent.ACTION_MAIN).setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (app.leanback) i.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Ps4.open(act, i);
    }

    /** A tile's artwork: the banner centred on its edge colour, or the icon and name on PS blue. */
    static FrameLayout art(Context ctx, App app, float size) {
        FrameLayout f = new FrameLayout(ctx);
        f.setBackgroundColor(app.fill);
        ImageView img = new ImageView(ctx);
        if (app.banner != null) {
            img.setImageDrawable(app.banner);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            f.addView(img, new FrameLayout.LayoutParams(-1, -1));
        } else {
            // A soft light from the top over the icon's own colour, the icon large in the middle, the name below.
            f.setBackground(new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Ps4.lighten(app.fill, 0.10f), app.fill}));
            img.setImageDrawable(app.icon);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            boolean small = size < 200;
            int s = Ps4.px(ctx, size * (small ? 0.62f : 0.48f));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s, s, small ? Gravity.CENTER : Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            if (!small) lp.topMargin = Ps4.px(ctx, size * 0.17f);
            img.setElevation(Ps4.px(ctx, 6));
            f.addView(img, lp);
            if (!small) {
                TextView name = Ps4.text(ctx, size * 0.075f, UiFont.get(ctx, R.string.font_regular), Ps4.TEXT);
                name.setText(app.label);
                name.setGravity(Gravity.CENTER);
                FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
                np.bottomMargin = Ps4.px(ctx, size * 0.09f);
                np.leftMargin = np.rightMargin = Ps4.px(ctx, 12);
                f.addView(name, np);
            }
        }
        return f;
    }

    /** The app opened last (usage access), among the tiles: the PS4 shows one running game at a time. */
    private String lastOpened() {
        Set<String> tilePkgs = new HashSet<>();
        for (App app : apps) tilePkgs.add(app.pkg);
        return lastOpened(tilePkgs);
    }

    /** Sets lastOpenedAt too: when that app came to the front. */
    private String lastOpened(Set<String> tilePkgs) {
        lastOpenedAt = 0;
        try {
            UsageStatsManager usage = (UsageStatsManager) a.getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            long boot = now - SystemClock.elapsedRealtime();
            UsageEvents events = usage.queryEvents(Math.max(boot, now - 12 * 3600_000L), now);
            UsageEvents.Event e = new UsageEvents.Event();
            String last = null;
            while (events.hasNextEvent()) {
                events.getNextEvent(e);
                if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND && tilePkgs.contains(e.getPackageName())) {
                    last = e.getPackageName();
                    lastOpenedAt = e.getTimeStamp();
                }
            }
            return last;
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshRunning() {
        running.clear();
        String last = lastOpened();
        if (last != null) running.add(last);
        if (media != null && media.getPlaybackState() != null
                && media.getPlaybackState().getState() != PlaybackState.STATE_STOPPED) running.add(media.getPackageName());
        for (int i = 0; i < discs.size(); i++) {
            boolean on = i < apps.size() && running.contains(apps.get(i).pkg);
            discs.get(i).setVisibility(on ? View.VISIBLE : View.GONE);
        }
        updateSpin();
    }

    /**
     * A running app's disc turns like a CD in a drive, one turn every 1.6 s at 30 frames a second, and only while it
     * can be seen: on the focused tile's strip, with Home on screen and awake. Every frame redraws the screen, and
     * spinning discs nobody could see (the strips of unfocused tiles) kept this box near half a CPU core busy.
     */
    private void updateSpin() {
        View want = null;
        if (resumed && !idle && bigFocus() && focus < discs.size() && focus < apps.size()
                && discs.get(focus).getVisibility() == View.VISIBLE) {
            want = discs.get(focus);
        }
        spinning = want;
        startTicking();
    }

    private View spinning;
    private boolean ticking;

    private void startTicking() {
        if (ticking) return;
        ticking = true;
        handler.post(animTick);
    }

    /**
     * One timer for everything that moves on Home - the running app's disc and the player's wave - at 30 frames a
     * second, so they redraw in the same frame instead of two timers out of step filling every display frame.
     */
    private final Runnable animTick = new Runnable() {
        @Override
        public void run() {
            boolean busy = false;
            if (spinning != null && resumed) {
                spinning.setRotation((spinning.getRotation() + 360f * 33 / 1600) % 360f);
                busy = true;
            }
            if (mediaProgress != null && mediaProgress.frame()) busy = true;
            if (busy) handler.postDelayed(this, 33);
            else ticking = false;
        }
    };

    // ---- build ---------------------------------------------------------------------------------

    void create() {
        builtPalette = Ps4.paletteVersion;
        sounds = Ps4Sounds.get(a);
        root = new FrameLayout(a);
        Bitmap wall = Ps4.wallpaper(a);
        ImageView back = new ImageView(a);
        back.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (wall != null) back.setImageBitmap(wall);
        else back.setImageDrawable(new Ps4.Backdrop(Ps4.pal(a), false, false));
        root.addView(back, new FrameLayout.LayoutParams(-1, -1));
        // A light shade behind the status bar, so white text reads over any wallpaper.
        View shade = new View(a);
        shade.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x8C000000, 0x00000000}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, px(190)));

        buildBar();
        row = new FrameLayout(a);
        row.setClipChildren(false);
        root.addView(row, new FrameLayout.LayoutParams(-1, px(FOCUS_Y + BIG + STRIP + 10)));
        title = Ps4.text(a, 60, Ps4.thin(a), Ps4.TEXT);
        title.setShadowLayer(px(10), 0, px(2), 0x66000000);
        root.addView(title, new FrameLayout.LayoutParams(-2, px(80)));
        sub = new FrameLayout(a);
        root.addView(sub, new FrameLayout.LayoutParams(-1, px(TILE)));
        sub.setTranslationY(px(SUB_Y));
        buildMedia();
        a.setContentView(root);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        a.registerReceiver(packages, f);
        receiverOn = true;
        Ps4Listener.watch(notificationsChanged);
        startMedia();
        reload();
    }

    private void buildBar() {
        FrameLayout bar = new FrameLayout(a);
        barView = bar;
        bar.setClipChildren(false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, px(70));
        lp.topMargin = px(BAR_Y - 35);
        root.addView(bar, lp);

        // Left: notifications with their count, then the network, like the PS4's information area.
        LinearLayout notif = barItem(new Ps4.Icon(Ps4.Icon.INFO), "");
        notifCount = (TextView) notif.getChildAt(1);
        notif.setOnClickListener(v -> Ps4.open(a, new Intent(a, Ps4NotificationsActivity.class)));
        bar.addView(notif, at(86, Gravity.START));
        notifItem = notif;
        LinearLayout net = barItem(new Ps4.Icon(Ps4.Icon.NETWORK), null);
        network = (TextView) net.getChildAt(1);
        networkIcon = (ImageView) net.getChildAt(0);
        net.setOnClickListener(v -> Ps4.open(a, SystemSettings.intent(a, Settings.ACTION_WIFI_SETTINGS)));
        bar.addView(net, at(250, Gravity.START));
        // Bluetooth: how many paired devices are connected now (the remote, headphones...); opens Sound/devices.
        LinearLayout bt = barItem(new Ps4.Icon(Ps4.Icon.BLUETOOTH), "");
        btCount = (TextView) bt.getChildAt(1);
        bt.setOnClickListener(v -> Ps4.open(a, new Intent(a, Ps4QuickMenuActivity.class)
                .putExtra(Ps4QuickMenuActivity.EXTRA_SECTION, Ps4QuickMenuActivity.SECTION_SOUND)));
        bar.addView(bt, at(640, Gravity.START));

        // Centre: online dot, the user's photo and name; opens the profile options.
        LinearLayout user = new LinearLayout(a);
        user.setGravity(Gravity.CENTER_VERTICAL);
        user.setPadding(px(14), 0, px(14), 0);
        View dot = new View(a);
        dot.setBackground(Ps4.circle(Ps4.pal(a).online));
        user.addView(dot, new LinearLayout.LayoutParams(px(16), px(16)));
        View av = Ps4.avatar(a, 54);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(px(54), px(54));
        ap.leftMargin = px(22);
        user.addView(av, ap);
        TextView name = Ps4.text(a, 30, Ps4.light(a), Ps4.TEXT);
        name.setText(Ps4.userName(a));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-2, -2);
        np.leftMargin = px(18);
        user.addView(name, np);
        user.setOnClickListener(v -> Ps4.open(a, new Intent(a, Ps4QuickMenuActivity.class)
                .putExtra(Ps4QuickMenuActivity.EXTRA_SECTION, Ps4QuickMenuActivity.SECTION_PROFILE)));
        bar.addView(user, at(840, Gravity.START));
        barItems.add(user);
        userItem = user;

        // Right of centre: the library with its app count (where the PS4 shows trophies), then Settings and Power.
        LinearLayout lib = barItem(new Ps4.Icon(Ps4.Icon.LIBRARY), "");
        libraryCount = (TextView) lib.getChildAt(1);
        lib.setOnClickListener(v -> Ps4.open(a, new Intent(a, Ps4AppsActivity.class)));
        bar.addView(lib, at(1290, Gravity.START));
        LinearLayout gear = barItem(new Ps4.Icon(Ps4.Icon.GEAR), null);
        gear.removeViewAt(1);
        gear.setOnClickListener(v -> Ps4.open(a, new Intent(a, Ps4QuickMenuActivity.class)));
        bar.addView(gear, at(1420, Gravity.START));
        LinearLayout power = barItem(new Ps4.Icon(Ps4.Icon.POWER), null);
        power.removeViewAt(1);
        power.setOnClickListener(v -> Ps4.open(a, new Intent(a, Ps4QuickMenuActivity.class)
                .putExtra(Ps4QuickMenuActivity.EXTRA_SECTION, Ps4QuickMenuActivity.SECTION_POWER)));
        bar.addView(power, at(1500, Gravity.START));

        // Right: the clock, in thin type.
        TextClock c = new TextClock(a);
        c.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(36));
        c.setTypeface(Ps4.light(a));
        c.setTextColor(Ps4.TEXT);
        c.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -1, Gravity.END);
        cp.rightMargin = px(96);
        bar.addView(c, cp);
        clock = c;
    }

    private FrameLayout.LayoutParams at(float x, int gravity) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -1, gravity);
        lp.leftMargin = px(x);
        return lp;
    }

    private LinearLayout barItem(Drawable icon, String label) {
        LinearLayout item = new LinearLayout(a);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(px(14), 0, px(14), 0);
        ImageView iv = new ImageView(a);
        iv.setImageDrawable(icon);
        item.addView(iv, new LinearLayout.LayoutParams(px(40), px(40)));
        TextView t = Ps4.text(a, 30, Ps4.light(a), Ps4.TEXT);
        if (label != null) t.setText(label);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.leftMargin = px(14);
        item.addView(t, tp);
        barItems.add(item);
        return item;
    }

    /**
     * The now-playing card, in Material 3 / One UI style rather than the PS4's square boxes: a 40 px rounded tonal
     * surface tinted with the accent, rounded album art, the app as a small chip, a slim progress track, and a filled
     * rounded play button in the accent colour between round previous / next buttons. Focus is a ring and a lift.
     */
    private void buildMedia() {
        Ps4.Palette pl = Ps4.pal(a);
        mediaCard = new FrameLayout(a);
        android.graphics.drawable.GradientDrawable surface = new android.graphics.drawable.GradientDrawable();
        surface.setColor(pl.surface);
        surface.setCornerRadius(px(40));
        surface.setStroke(Math.max(1, px(1.5f)), 0x22FFFFFF);
        mediaCard.setBackground(surface);
        mediaCard.setClipChildren(false);

        mediaArt = new ImageView(a);
        mediaArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FrameLayout artFrame = new FrameLayout(a);
        artFrame.setBackground(rounded(0xFF000000, 26));
        artFrame.setClipToOutline(true);
        artFrame.addView(mediaArt, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(px(154), px(154), Gravity.CENTER_VERTICAL);
        ap.leftMargin = px(18);
        mediaCard.addView(artFrame, ap);

        LinearLayout chip = new LinearLayout(a);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        mediaAppIcon = new ImageView(a);
        chip.addView(mediaAppIcon, new LinearLayout.LayoutParams(px(26), px(26)));
        mediaApp = Ps4.text(a, 22, UiFont.get(a, R.string.font_regular), pl.primary);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-2, -2);
        cl.leftMargin = px(10);
        chip.addView(mediaApp, cl);
        mediaCard.addView(chip, mediaText(24, 30));
        mediaTitle = Ps4.text(a, 34, UiFont.get(a, R.string.font_regular), Ps4.TEXT);
        mediaCard.addView(mediaTitle, mediaText(62, 46));
        mediaArtist = Ps4.text(a, 26, Ps4.light(a), Ps4.TEXT_DIM);
        mediaCard.addView(mediaArtist, mediaText(108, 36));

        // The progress line: a travelling wave up to the position (Android 13's player), a flat track after it.
        mediaProgress = new Ps4.Wave(a, pl.primary, pl.track);
        mediaProgress.onChange = this::startTicking;
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(px(MEDIA_TEXT_W), px(26));
        tp.leftMargin = px(196);
        tp.topMargin = px(148);
        mediaCard.addView(mediaProgress, tp);

        LinearLayout controls = new LinearLayout(a);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setClipChildren(false);
        View prev = control(Ps4.Icon.PREV, false);
        View play = control(Ps4.Icon.PAUSE, true);
        View next = control(Ps4.Icon.NEXT, false);
        mediaPlay = (ImageView) ((FrameLayout) play).getChildAt(0);
        controls.addView(prev);
        controls.addView(play);
        controls.addView(next);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL);
        cp.rightMargin = px(26);
        mediaCard.addView(controls, cp);

        artFrame.setOnClickListener(v -> openMediaApp());
        prev.setOnClickListener(v -> { if (media != null) media.getTransportControls().skipToPrevious(); });
        play.setOnClickListener(v -> togglePlay());
        next.setOnClickListener(v -> { if (media != null) media.getTransportControls().skipToNext(); });
        mediaItems.add(artFrame);
        mediaItems.add(prev);
        mediaItems.add(play);
        mediaItems.add(next);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(MEDIA_W), px(MEDIA_H));
        lp.leftMargin = px(MEDIA_X);
        lp.topMargin = px(MEDIA_Y);
        root.addView(mediaCard, lp);
        mediaCard.setVisibility(View.GONE);
    }

    private android.graphics.drawable.GradientDrawable rounded(int color, float radiusRef) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(radiusRef));
        return g;
    }

    private FrameLayout.LayoutParams mediaText(float y, float h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(MEDIA_TEXT_W), px(h));
        lp.leftMargin = px(196);
        lp.topMargin = px(y);
        return lp;
    }

    /** A round icon button; the main one is the accent-filled rounded square of Material 3's expressive player. */
    private View control(int icon, boolean main) {
        Ps4.Palette pl = Ps4.pal(a);
        FrameLayout f = new FrameLayout(a);
        ImageView iv = new ImageView(a);
        iv.setImageDrawable(new Ps4.Icon(icon, main ? pl.onPrimary : Ps4.TEXT));
        float size = main ? 84 : 64;
        f.setBackground(main ? rounded(pl.primary, 28) : rounded(0x1AFFFFFF, size / 2));
        f.addView(iv, new FrameLayout.LayoutParams(px(main ? 40 : 30), px(main ? 40 : 30), Gravity.CENTER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(size), px(size));
        lp.leftMargin = px(14);
        f.setLayoutParams(lp);
        return f;
    }

    /**
     * Once a second while Home is on screen: moves the progress to where the song is, and redraws the card when the
     * song, the cover or play/pause changed without a callback (some apps update their session late or not at all).
     */
    private final Runnable progressTick = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(this);
            if (!resumed) return;
            handler.postDelayed(this, 1000);
            if (!mediaSignature().equals(shown)) {
                checking = true;   // showMedia runs this again; that run must not redraw a second time
                showMedia();
                checking = false;
                return;
            }
            if (media == null || media.getMetadata() == null || media.getPlaybackState() == null) return;
            PlaybackState st = media.getPlaybackState();
            boolean playing = st.getState() == PlaybackState.STATE_PLAYING;
            mediaProgress.setMoving(playing && !idleHidden());
            long duration = media.getMetadata().getLong(MediaMetadata.METADATA_KEY_DURATION);
            if (duration <= 0) {
                mediaProgress.setProgress(0f);
                return;
            }
            long pos = st.getPosition();
            if (playing) pos += (long) ((SystemClock.elapsedRealtime() - st.getLastPositionUpdateTime()) * st.getPlaybackSpeed());
            mediaProgress.setProgress(pos / (float) duration);
        }
    };

    private boolean idleHidden() {
        return mediaCard.getVisibility() != View.VISIBLE;
    }

    /** What the card should show now: app, title, artist, cover identity and play state. */
    private String mediaSignature() {
        if (media == null) return "";
        MediaMetadata m = media.getMetadata();
        PlaybackState st = media.getPlaybackState();
        if (m == null) return media.getPackageName();
        // Text and state only: every getMetadata() is a fresh copy from the app, so its cover is a new Bitmap each
        // time and comparing it said "changed" forever (Home spun on it and froze). A new cover comes with a new
        // title or album anyway, and the metadata callback redraws it.
        return media.getPackageName() + "|" + m.getText(MediaMetadata.METADATA_KEY_TITLE) + "|"
                + m.getText(MediaMetadata.METADATA_KEY_ARTIST) + "|" + m.getText(MediaMetadata.METADATA_KEY_ALBUM)
                + "|" + (st == null ? -1 : st.getState());
    }

    // ---- media ---------------------------------------------------------------------------------

    private void startMedia() {
        sessions = (MediaSessionManager) a.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (sessions == null) return;
        try {
            sessions.addOnActiveSessionsChangedListener(sessionsChanged, Ps4Listener.component(a), handler);
            pickMedia(null);
        } catch (SecurityException noAccess) {
            sessions = null;   // notification access not granted: no card
        }
    }

    private void pickMedia(List<MediaController> list) {
        if (sessions == null) return;
        if (list == null) {
            try {
                list = sessions.getActiveSessions(Ps4Listener.component(a));
            } catch (SecurityException e) {
                return;
            }
        }
        MediaController best = null;
        boolean bestTitled = false;
        for (MediaController c : list) {
            PlaybackState s = c.getPlaybackState();
            if (c.getMetadata() == null || s == null) continue;
            if (s.getState() == PlaybackState.STATE_PLAYING) { best = c; break; }
            // Nothing playing: a paused session that says what it was playing (a song title) beats one that doesn't
            // (YouTube's empty session after a video).
            CharSequence title = c.getMetadata().getText(MediaMetadata.METADATA_KEY_TITLE);
            boolean titled = title != null && title.length() > 0;
            boolean paused = s.getState() != PlaybackState.STATE_STOPPED && s.getState() != PlaybackState.STATE_NONE;
            if (paused && (best == null || (titled && !bestTitled))) {
                best = c;
                bestTitled = titled;
            }
        }
        // Keep the callback on exactly one controller: the one for the session being shown.
        boolean same = registered != null && best != null && registered.getSessionToken().equals(best.getSessionToken());
        if (!same) {
            if (registered != null) registered.unregisterCallback(mediaCallback);
            registered = best;
            if (best != null) best.registerCallback(mediaCallback, handler);
        }
        media = registered;
        showMedia();
    }

    private void showMedia() {
        MediaMetadata m = media == null ? null : media.getMetadata();
        if (m == null) {
            shown = mediaSignature();
            mediaCard.setVisibility(View.GONE);
            if (resumed) handler.postDelayed(progressTick, 1000);
            if (zone == ZONE_MEDIA) setZone(ZONE_ROW);
            refreshRunning();
            return;
        }
        Bitmap art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (art == null) art = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (art == null) art = m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (art != null) {
            mediaArt.setImageBitmap(art);
            mediaArt.setBackground(null);
        } else {
            try {
                mediaArt.setImageDrawable(a.getPackageManager().getApplicationIcon(media.getPackageName()));
            } catch (PackageManager.NameNotFoundException e) {
                mediaArt.setImageDrawable(new Ps4.Icon(Ps4.Icon.SOUND));
            }
            mediaArt.setBackgroundColor(Ps4.pal(a).tile);
        }
        CharSequence t = m.getText(MediaMetadata.METADATA_KEY_TITLE);
        if (t == null) t = m.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        CharSequence ar = m.getText(MediaMetadata.METADATA_KEY_ARTIST);
        if (ar == null) ar = m.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        if (ar == null) ar = m.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
        mediaTitle.setText(t == null ? "" : t);
        mediaArtist.setText(ar == null ? "" : ar);
        boolean playing = media.getPlaybackState() != null && media.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
        String app = media.getPackageName();
        try {
            app = String.valueOf(a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(app, 0)));
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        mediaApp.setText(playing ? app : app + " · En pausa");
        mediaPlay.setImageDrawable(new Ps4.Icon(playing ? Ps4.Icon.PAUSE : Ps4.Icon.PLAY, Ps4.pal(a).onPrimary));
        try {
            mediaAppIcon.setImageDrawable(a.getPackageManager().getApplicationIcon(media.getPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
            mediaAppIcon.setImageDrawable(null);
        }
        mediaCard.setVisibility(View.VISIBLE);
        shown = mediaSignature();
        if (!checking) progressTick.run();
        refreshRunning();
    }

    private void togglePlay() {
        if (media == null || media.getPlaybackState() == null) return;
        if (media.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
            media.getTransportControls().pause();
            return;
        }
        MediaController target = media;
        target.getTransportControls().play();
        // Once an app's process has been killed in the background (common on a 1 GB TV), its session can
        // linger showing the last song with play() silently doing nothing: there is nothing left running to
        // resume. If playback still has not started shortly after, wake the app properly by opening it -
        // the same as tapping its tile - instead of leaving the button looking broken.
        handler.postDelayed(() -> {
            if (media != target) return;   // a different session took over meanwhile
            PlaybackState st = target.getPlaybackState();
            if (st == null || st.getState() != PlaybackState.STATE_PLAYING) openMediaApp();
        }, 1200);
    }

    private void openMediaApp() {
        if (media == null) return;
        PackageManager pm = a.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(media.getPackageName());
        if (i == null) i = pm.getLaunchIntentForPackage(media.getPackageName());
        if (i != null) Ps4.open(a, i);
    }

    // ---- tiles ---------------------------------------------------------------------------------

    private void reload() {
        dirty = false;
        String keep = focus < apps.size() && focus != 0 ? apps.get(focus).pkg : null;
        apps.clear();
        apps.addAll(arrange(loadApps(a, false)));
        row.removeAllViews();
        tiles.clear();
        discs.clear();
        stars.clear();
        startLabels.clear();
        for (App app : apps) {
            FrameLayout t = tileWith(art(a, app, BIG));
            tiles.add(t);
            stars.get(stars.size() - 1).setVisibility(favorites.contains(app.pkg) ? View.VISIBLE : View.GONE);
        }
        tiles.add(libraryTile());
        focus = 0;
        if (keep != null) for (int i = 0; i < apps.size(); i++) if (apps.get(i).pkg.equals(keep)) focus = i;
        libraryCount.setText(String.valueOf(apps.size()));
        refreshRunning();
        layout(false);
    }

    private List<App> arrange(List<App> loaded) {
        favorites.clear();
        favorites.addAll(Ps4.savedList(a, "favorites"));
        List<String> order = Ps4.savedList(a, "order");
        List<App> out = new ArrayList<>(loaded);
        Collections.sort(out, (x, y) -> {
            int fx = favorites.indexOf(x.pkg), fy = favorites.indexOf(y.pkg);
            if (fx >= 0 || fy >= 0) return fx < 0 ? 1 : fy < 0 ? -1 : Integer.compare(fx, fy);
            int ox = order.indexOf(x.pkg), oy = order.indexOf(y.pkg);
            if (ox >= 0 || oy >= 0) return ox < 0 ? 1 : oy < 0 ? -1 : Integer.compare(ox, oy);
            return Integer.compare(loaded.indexOf(x), loaded.indexOf(y));
        });
        Set<String> pkgs = new HashSet<>();
        for (App app : loaded) pkgs.add(app.pkg);
        recent = lastOpened(pkgs);
        if (recent != null) {
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i).pkg.equals(recent)) {
                    out.add(0, out.remove(i));
                    break;
                }
            }
        }
        return out;
    }

    /** True when the first tile is there only because it was opened last (not its own place in the row). */
    private boolean recentPinned() {
        return recent != null && !apps.isEmpty() && apps.get(0).pkg.equals(recent);
    }

    /**
     * Saves the row: favourites in their own list, everything else in "order". A favourite keeps its old place in
     * "order", so taking it out of favourites puts it back where it was rather than at the front.
     */
    private void saveOrder() {
        List<String> old = Ps4.savedList(a, "order");
        List<String> oldFavs = Ps4.savedList(a, "favorites");
        List<String> order = new ArrayList<>();
        List<String> favs = new ArrayList<>();
        String pinned = recentPinned() ? recent : null;
        for (App app : apps) {
            if (app.pkg.equals(pinned)) continue;
            if (favorites.contains(app.pkg)) favs.add(app.pkg);
            else order.add(app.pkg);
        }
        if (pinned != null) {
            if (favorites.contains(pinned)) {
                int at = oldFavs.indexOf(pinned);
                favs.add(at < 0 ? 0 : Math.min(at, favs.size()), pinned);
            } else {
                int at = old.indexOf(pinned);
                order.add(at < 0 ? order.size() : Math.min(at, order.size()), pinned);
            }
        }
        for (String f : favs) {
            int at = old.indexOf(f);
            order.add(at < 0 ? order.size() : Math.min(at, order.size()), f);
        }
        favorites.clear();
        favorites.addAll(favs);
        Ps4.saveList(a, "order", order);
        Ps4.saveList(a, "favorites", favs);
    }

    private FrameLayout libraryTile() {
        FrameLayout art = new FrameLayout(a);
        art.setBackgroundColor(Ps4.pal(a).tile);
        ImageView iv = new ImageView(a);
        iv.setImageDrawable(new Ps4.Icon(Ps4.Icon.LIBRARY));
        int s = px(BIG * 0.36f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s, s, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        lp.topMargin = px(BIG * 0.2f);
        art.addView(iv, lp);
        TextView name = Ps4.text(a, BIG * 0.085f, Ps4.light(a), Ps4.TEXT);
        name.setText("Biblioteca");
        name.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        np.bottomMargin = px(BIG * 0.12f);
        art.addView(name, np);
        return tileWith(art);
    }

    /** One tile at focused size (343 x 343 plus the Start strip); unfocused tiles are the same view scaled down. */
    private FrameLayout tileWith(View art) {
        FrameLayout t = new FrameLayout(a);
        t.setClipChildren(false);
        t.addView(art, new FrameLayout.LayoutParams(px(BIG), px(BIG)));
        // The strip: "Iniciar", with a disc on its left while the app is running, as on the PS4.
        LinearLayout strip = new LinearLayout(a);
        strip.setGravity(Gravity.CENTER);
        strip.setBackgroundColor(Ps4.pal(a).strip);
        ImageView disc = new ImageView(a);
        disc.setImageDrawable(new Ps4.Icon(Ps4.Icon.DISC));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(px(34), px(34));
        dp.rightMargin = px(14);
        strip.addView(disc, dp);
        disc.setVisibility(View.GONE);
        discs.add(disc);
        TextView start = Ps4.text(a, 30, Ps4.light(a), Ps4.TEXT);
        start.setText("Iniciar");
        strip.addView(start, new LinearLayout.LayoutParams(-2, -2));
        startLabels.add(start);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(px(BIG), px(STRIP));
        sp.topMargin = px(BIG);
        t.addView(strip, sp);
        View border = new View(a);
        border.setBackground(Ps4.box(a, BORDER, Ps4.FOCUS, 0));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(px(BIG + 2 * BORDER), px(BIG + STRIP + 2 * BORDER));
        bp.leftMargin = bp.topMargin = -px(BORDER);
        t.addView(border, bp);
        // A favourite's star, in the art's top-right corner (child 3; layout uses 1 and 2).
        ImageView star = new ImageView(a);
        star.setImageDrawable(new Ps4.Icon(Ps4.Icon.STAR, 0xFFFFD54F));
        star.setBackground(Ps4.circle(0x99000000));
        star.setPadding(px(9), px(9), px(9), px(9));
        FrameLayout.LayoutParams stp = new FrameLayout.LayoutParams(px(56), px(56), Gravity.TOP | Gravity.END);
        stp.topMargin = stp.rightMargin = px(12);
        t.addView(star, stp);
        star.setVisibility(View.GONE);
        stars.add(star);
        t.setPivotX(0);
        t.setPivotY(0);
        row.addView(t, new FrameLayout.LayoutParams(px(BIG), px(BIG + STRIP)));
        return t;
    }

    // ---- layout --------------------------------------------------------------------------------

    /** The focused tile is shown large only while the row itself has the focus (or the bar, above it). */
    private boolean bigFocus() {
        return zone == ZONE_ROW || zone == ZONE_BAR || zone == ZONE_OPTIONS || zone == ZONE_MOVE;
    }

    private void layout(boolean animate) {
        boolean big = bigFocus();
        float fx = focus == 0 ? ROW_X : ROW_X + TILE + (big ? FOCUS_GAP : GAP);
        float focusW = big ? BIG : TILE;
        float small = TILE / BIG;
        for (int i = 0; i < tiles.size(); i++) {
            FrameLayout t = tiles.get(i);
            boolean focused = i == focus;
            float x;
            if (i < focus) x = fx - (big ? FOCUS_GAP - GAP : 0) - (focus - i) * (TILE + GAP);
            else if (i > focus) x = fx + focusW + (big ? FOCUS_GAP : GAP) + (i - focus - 1) * (TILE + GAP);
            else x = fx;
            boolean large = focused && big;
            float scale = large ? 1f : small;
            float y = large ? (zone == ZONE_MOVE ? FOCUS_Y - 16 : FOCUS_Y) : ROW_Y;
            View strip = t.getChildAt(1), border = t.getChildAt(2);
            float borderAlpha = large && zone != ZONE_BAR ? 1f : 0f;
            if (animate && t.getVisibility() == View.VISIBLE) {
                t.animate().translationX(px(x)).translationY(px(y)).scaleX(scale).scaleY(scale)
                        .setDuration(170).setInterpolator(new DecelerateInterpolator()).start();
                strip.animate().alpha(large ? 1f : 0f).setDuration(170).start();
                border.animate().alpha(borderAlpha).setDuration(170).start();
            } else {
                t.setTranslationX(px(x));
                t.setTranslationY(px(y));
                t.setScaleX(scale);
                t.setScaleY(scale);
                strip.setAlpha(large ? 1f : 0f);
                border.setAlpha(borderAlpha);
            }
            t.setVisibility(x + TILE < -400 || x > 2300 ? View.INVISIBLE : View.VISIBLE);
            if (focused) t.bringToFront();
        }
        title.setText(focus < apps.size() ? apps.get(focus).label : "Biblioteca");
        if (big) {
            title.setTranslationX(px(fx + BIG + 18));
            title.setTranslationY(px(FOCUS_Y + BIG + STRIP - 82));
        } else {
            title.setTranslationX(px(fx + subCount() * (TILE + 4) + 22));
            title.setTranslationY(px(SUB_Y + TILE - 90));
        }
        boolean showTitle = zone != ZONE_MEDIA;
        if (animate) {
            title.setAlpha(0f);
            if (showTitle) title.animate().alpha(1f).setDuration(220).start();
        } else {
            title.setAlpha(showTitle ? 1f : 0f);
        }
        buildSub(fx);
        paintFocus();
        updateSpin();
    }

    private int subCount() {
        return focus < apps.size() ? 2 : 1;
    }

    /** The focused item's actions, shown only once Down leaves the row: Start and Information. */
    private void buildSub(float fx) {
        sub.removeAllViews();
        subItems.clear();
        if (zone != ZONE_SUB) {
            sub.setVisibility(View.GONE);
            return;
        }
        if (focus < apps.size()) {
            App app = apps.get(focus);
            subItems.add(subCard(new Ps4.Icon(Ps4.Icon.PLAY), "Iniciar", v -> launch(a, app)));
            subItems.add(subCard(new Ps4.Icon(Ps4.Icon.INFO), "Información", v -> Ps4.open(a, Ps4.appInfo(app.pkg))));
        } else {
            subItems.add(subCard(new Ps4.Icon(Ps4.Icon.LIBRARY), "Abrir", v -> Ps4.open(a, new Intent(a, Ps4AppsActivity.class))));
        }
        for (int i = 0; i < subItems.size(); i++) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(TILE), px(TILE));
            lp.leftMargin = px(fx + i * (TILE + 4));
            sub.addView(subItems.get(i), lp);
        }
        sub.setVisibility(View.VISIBLE);
        sub.setAlpha(0f);
        sub.animate().alpha(1f).setDuration(160).start();
    }

    private View subCard(Drawable icon, String label, View.OnClickListener click) {
        FrameLayout card = new FrameLayout(a);
        card.setBackground(Ps4.box(a, 0, 0, Ps4.pal(a).card));
        ImageView iv = new ImageView(a);
        iv.setImageDrawable(icon);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(px(64), px(64), Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        ip.topMargin = px(44);
        card.addView(iv, ip);
        TextView t = Ps4.text(a, 24, Ps4.light(a), Ps4.TEXT);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        tp.bottomMargin = px(30);
        card.addView(t, tp);
        card.setOnClickListener(click);
        return card;
    }

    private void paintFocus() {
        for (int i = 0; i < subItems.size(); i++) {
            subItems.get(i).setForeground(zone == ZONE_SUB && i == subFocus ? Ps4.box(a, BORDER, Ps4.FOCUS, 0) : null);
        }
        for (int i = 0; i < barItems.size(); i++) {
            barItems.get(i).setBackground(zone == ZONE_BAR && i == barFocus ? Ps4.box(a, 3, Ps4.FOCUS, 0x26FFFFFF) : null);
        }
        for (int i = 0; i < mediaItems.size(); i++) {
            boolean on = zone == ZONE_MEDIA && i == mediaFocus;
            View m = mediaItems.get(i);
            float radius = i == 0 ? 26 : i == 2 ? 28 : 32;
            android.graphics.drawable.GradientDrawable ring = null;
            if (on) {
                ring = new android.graphics.drawable.GradientDrawable();
                ring.setCornerRadius(px(radius));
                ring.setStroke(px(3), 0xFFFFFFFF);
            }
            m.setForeground(ring);
            m.animate().scaleX(on ? 1.08f : 1f).scaleY(on ? 1.08f : 1f).setDuration(140).start();
        }
    }

    // ---- idle: the apps step aside for the wallpaper -------------------------------------------

    private void scheduleIdle() {
        handler.removeCallbacks(goIdle);
        int seconds = Ps4.option(a, "ps4_idle_seconds", 10);
        if (seconds > 0 && resumed) handler.postDelayed(goIdle, seconds * 1000L);
    }

    private void showConnections() {
        Ps4.Net n = Ps4.net(a);
        Ps4.Icon icon = new Ps4.Icon(n.ethernet ? Ps4.Icon.ETHERNET : Ps4.Icon.NETWORK);
        icon.level = n.wifi ? Math.max(1, n.bars) : n.online ? 3 : 0;
        networkIcon.setImageDrawable(icon);
        network.setText(n.online ? n.name : "Sin conexión");
        int connected = 0;
        for (String[] d : Ps4.bluetooth(a)) if (!d[1].isEmpty()) connected++;
        btCount.setText(connected > 0 ? String.valueOf(connected) : "");
    }

    /** A greeting for the time of day, sometimes a playful one, with the owner's name. */
    static String greeting(Context ctx) {
        String name = Ps4.userName(ctx);
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String[] lines;
        if (h < 5) {
            lines = new String[]{"¿Trasnochando, %s?", "La ciudad duerme, %s", "Noche de héroes, %s", "¿Una más y a dormir, %s?",
                    "Buenas noches, %s"};
        } else if (h < 12) {
            lines = new String[]{"Buenos días, %s", "¿Listo para el día, %s?", "Arriba, %s: el café primero",
                    "Buen día para una maratón, %s"};
        } else if (h < 19) {
            lines = new String[]{"Buenas tardes, %s", "¿Qué vemos hoy, %s?", "Tarde de series, %s", "Hola de nuevo, %s"};
        } else {
            lines = new String[]{"Buenas noches, %s", "¿Película o serie, %s?", "Noche de estreno, %s", "Hora de relajarse, %s"};
        }
        return String.format(lines[new java.util.Random().nextInt(lines.length)], name);
    }

    /** Roboto at a given weight (the TV's own variable Roboto), as One UI sets its lock-screen clock. */
    private static android.graphics.Typeface roboto(int weight) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            return android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, weight, false);
        }
        return android.graphics.Typeface.create("sans-serif", weight >= 600 ? android.graphics.Typeface.BOLD
                : android.graphics.Typeface.NORMAL);
    }

    /**
     * The idle screen's text, on the left in Bebas Neue (tall and narrow, like the SPIDER-MAN title): the date in
     * capitals in the accent, a big clock, a short accent rule and the greeting, centred above the player.
     */
    private void buildIdleLayer() {
        android.graphics.Typeface bebas = a.getResources().getFont(R.font.bebas_neue);
        int accent = Ps4.pal(a).primary;
        idleLayer = new FrameLayout(a);
        View scrim = new View(a);
        scrim.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x99000000, 0x00000000}));
        idleLayer.addView(scrim, new FrameLayout.LayoutParams(px(900), -1, Gravity.START));
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        TextClock date = new TextClock(a);
        date.setFormat12Hour("EEEE  ·  d 'de' MMMM");
        date.setFormat24Hour("EEEE  ·  d 'de' MMMM");
        date.setAllCaps(true);
        date.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(40));
        date.setTypeface(bebas);
        date.setLetterSpacing(0.08f);
        date.setTextColor(accent);
        date.setIncludeFontPadding(false);
        // wrap_content + setMaxWidth, not match_parent: see the comment on idleGreeting below for why a
        // match_parent child here can silently draw past box's own bounds for a long enough date (a month
        // name longer than "mayo") instead of wrapping.
        date.setMaxWidth(px(700));
        box.addView(date, new LinearLayout.LayoutParams(-2, -2));
        TextClock big = new TextClock(a);
        big.setFormat12Hour("h:mm");
        big.setFormat24Hour("H:mm");
        big.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(300));
        big.setTypeface(bebas);
        big.setTextColor(Ps4.TEXT);
        big.setIncludeFontPadding(false);
        big.setSingleLine(true);
        // Wrap, not match_parent: at match_parent width inside the box below, a two-digit hour ("11:42") was
        // wider than the box's old fixed 470px and wrapped onto a second line ("11:4" / "2") that landed on
        // top of the greeting underneath it, instead of just taking the room it needs.
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.topMargin = px(4);
        box.addView(big, cp);
        View rule = new View(a);
        rule.setBackgroundColor(accent);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(px(90), px(5));
        rp.topMargin = -px(34);   // Bebas leaves a tall gap under its figures
        rp.bottomMargin = px(26);
        box.addView(rule, rp);
        // Not Ps4.text() (it leaves a one-line-tall measurement a later setSingleLine(false) does not undo), and
        // not match_parent inside box: box is wrap_content, so a match_parent child there is measured with no
        // real width limit (AT_MOST the whole screen), draws its single line at that width without ever needing
        // to wrap onto the second line maxLines allows, and then box - sized to the clock above it, not to this
        // line - clips whatever did not fit. setMaxWidth gives it a real limit to wrap against instead.
        idleGreeting = new TextView(a);
        idleGreeting.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(56));
        idleGreeting.setTypeface(bebas);
        idleGreeting.setTextColor(Ps4.TEXT);
        idleGreeting.setIncludeFontPadding(false);
        idleGreeting.setMaxLines(2);
        idleGreeting.setMaxWidth(px(700));
        idleGreeting.setLetterSpacing(0.02f);
        idleGreeting.setLineSpacing(0, 0.95f);
        box.addView(idleGreeting, new LinearLayout.LayoutParams(-2, -2));
        for (TextView v : new TextView[]{date, big, idleGreeting}) v.setShadowLayer(px(14), 0, px(3), 0x99000000);
        // Left edge at x=110, as wide as the clock needs (wrap_content: see big's own comment above) so it is
        // never cut short, which in practice still ends well before the figure in the middle of the wallpaper.
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.CENTER_VERTICAL);
        bp.leftMargin = px(110);
        bp.bottomMargin = px(120);
        idleLayer.addView(box, bp);
        root.addView(idleLayer, root.indexOfChild(mediaCard), new FrameLayout.LayoutParams(-1, -1));
        idleLayer.setAlpha(0f);
        idleLayer.setVisibility(View.GONE);
    }

    private void sleep() {
        if (idle || !resumed) return;
        idle = true;
        updateSpin();
        long d = 600;
        if (idleLayer == null) buildIdleLayer();
        idleGreeting.setText(greeting(a));
        for (View v : new View[]{row, title, sub}) v.animate().alpha(0f).translationYBy(-px(30)).setDuration(d).start();
        barView.animate().alpha(0f).setDuration(d).start();
        idleLayer.setVisibility(View.VISIBLE);
        idleLayer.animate().alpha(1f).setDuration(d + 200).start();
        if (mediaCard.getVisibility() == View.VISIBLE) {
            float centre = (root.getWidth() - mediaCard.getWidth()) / 2f - mediaCard.getLeft();
            mediaCard.animate().translationX(centre).setDuration(d).start();
        }
    }

    private void wake() {
        idle = false;
        updateSpin();
        long d = 260;
        row.animate().alpha(1f).translationY(0).setDuration(d).start();
        title.animate().alpha(zone == ZONE_MEDIA ? 0f : 1f).translationYBy(px(30)).setDuration(d).start();
        sub.animate().alpha(1f).translationY(px(SUB_Y)).setDuration(d).start();
        barView.animate().alpha(1f).setDuration(d).start();
        if (idleLayer != null) {
            idleLayer.animate().alpha(0f).setDuration(d).withEndAction(() -> { if (!idle) idleLayer.setVisibility(View.GONE); }).start();
        }
        mediaCard.animate().translationX(0).setDuration(d).start();
    }

    // ---- keys and lifecycle --------------------------------------------------------------------

    boolean dispatchKey(KeyEvent e) {
        int code = e.getKeyCode();
        boolean ours = isOurs(code);
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            scheduleIdle();
            if (idle && ours) {   // the first key only brings Home back
                wake();
                swallowUp = code;
                return true;
            }
        } else {
            if (code == swallowUp) {
                swallowUp = -1;
                return true;
            }
            if (isOk(code)) {
                if (okPending) {   // a short press: open the app
                    okPending = false;
                    handler.removeCallbacks(okLong);
                    activate();
                }
                okLongFired = false;
            }
            return ours;
        }
        if (zone == ZONE_OPTIONS) return optionsKey(code, e);
        if (zone == ZONE_MOVE) return moveKey(code);
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: move(-1); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: move(1); return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (zone == ZONE_MEDIA || zone == ZONE_SUB) setZone(ZONE_ROW);
                else if (zone == ZONE_ROW) setZone(ZONE_BAR);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (zone == ZONE_BAR) setZone(ZONE_ROW);
                else if (zone == ZONE_ROW && mediaCard.getVisibility() == View.VISIBLE) setZone(ZONE_MEDIA);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (e.getRepeatCount() > 0 || e.isLongPress()) {
                    // A remote that repeats the key (no key helper) says it is held before the timer does.
                    if (okPending) {
                        handler.removeCallbacks(okLong);
                        okLong.run();
                    }
                    return true;
                }
                if (zone == ZONE_ROW && focus < apps.size()) {
                    okPending = true;
                    handler.postDelayed(okLong, HOLD_MS);
                } else {
                    activate();
                }
                return true;
            case KeyEvent.KEYCODE_NOTIFICATION:
            case KeyEvent.KEYCODE_SETTINGS:
                if (e.getRepeatCount() == 0) Ps4.open(a, new Intent(a, Ps4QuickMenuActivity.class));
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (zone == ZONE_ROW && focus < apps.size() && e.getRepeatCount() == 0) openOptions();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (zone != ZONE_ROW) { sounds.play(Ps4Sounds.BACK); setZone(ZONE_ROW); }
                else if (focus != 0) { sounds.play(Ps4Sounds.BACK); focus = 0; layout(true); }
                return true;
            default:
                return false;
        }
    }

    private static boolean isOk(int code) {
        return code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    // ---- options (hold OK or Menu on a tile) ---------------------------------------------------

    /**
     * The PS4 options menu: a panel on the right over a dimmed Home, listing what can be done with the focused app:
     * Start, Move, add to or remove from favourites, Information and, for apps the owner installed, Uninstall.
     */
    private void openOptions() {
        if (focus >= apps.size()) return;
        App app = apps.get(focus);
        sounds.play(Ps4Sounds.OK);
        Ps4.Palette pl = Ps4.pal(a);
        if (optionsPanel != null) root.removeView(optionsPanel);
        optionsPanel = new FrameLayout(a);
        View scrim = new View(a);
        scrim.setBackgroundColor(0x66000000);
        optionsPanel.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout panel = new FrameLayout(a);
        panel.setBackgroundColor(pl.panel);
        optionsPanel.addView(panel, new FrameLayout.LayoutParams(px(620), -1, Gravity.END));
        TextView head = Ps4.text(a, 44, Ps4.thin(a), Ps4.TEXT);
        head.setText(app.label);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(px(520), px(70));
        hp.leftMargin = px(56);
        hp.topMargin = px(70);
        panel.addView(head, hp);
        View rule = new View(a);
        rule.setBackgroundColor(0x664A5380);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-1, px(2));
        rp.topMargin = px(160);
        panel.addView(rule, rp);

        optionRows.clear();
        optionActions.clear();
        boolean fav = favorites.contains(app.pkg);
        addOption(panel, Ps4.Icon.PLAY, "Iniciar", () -> { closeOptions(); launch(a, app); });
        addOption(panel, Ps4.Icon.MOVE, "Mover", this::startMove);
        addOption(panel, Ps4.Icon.STAR, fav ? "Quitar de favoritos" : "Añadir a favoritos", () -> toggleFavorite(app));
        addOption(panel, Ps4.Icon.INFO, "Información", () -> { closeOptions(); Ps4.open(a, Ps4.appInfo(app.pkg)); });
        if (removable(app.pkg)) {
            addOption(panel, Ps4.Icon.TRASH, "Desinstalar", () -> {
                closeOptions();
                Ps4.open(a, new Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:" + app.pkg)));
            });
        }
        root.addView(optionsPanel, new FrameLayout.LayoutParams(-1, -1));
        optionsPanel.setAlpha(0f);
        optionsPanel.animate().alpha(1f).setDuration(160).start();
        panel.setTranslationX(px(80));
        panel.animate().translationX(0).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
        optionFocus = 0;
        zone = ZONE_OPTIONS;
        paintOptions();
        layout(true);
    }

    private void addOption(FrameLayout panel, int icon, String label, Runnable action) {
        LinearLayout r = new LinearLayout(a);
        r.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv = new ImageView(a);
        iv.setImageDrawable(new Ps4.Icon(icon));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(px(44), px(44));
        ip.leftMargin = px(22);
        r.addView(iv, ip);
        TextView t = Ps4.text(a, 32, Ps4.light(a), Ps4.TEXT);
        t.setText(label);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.leftMargin = px(26);
        r.addView(t, tp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(520), px(84));
        lp.leftMargin = px(40);
        lp.topMargin = px(196 + optionRows.size() * 92);
        panel.addView(r, lp);
        optionRows.add(r);
        optionActions.add(action);
    }

    private void paintOptions() {
        for (int i = 0; i < optionRows.size(); i++) {
            optionRows.get(i).setBackground(i == optionFocus ? Ps4.box(a, 4, 0xFFC9CDD8, 0x14FFFFFF) : null);
        }
    }

    private void closeOptions() {
        if (optionsPanel != null) {
            View gone = optionsPanel;
            gone.animate().alpha(0f).setDuration(140).withEndAction(() -> root.removeView(gone)).start();
            optionsPanel = null;
        }
        if (zone == ZONE_OPTIONS) {
            zone = ZONE_ROW;
            layout(true);
        }
    }

    private boolean optionsKey(int code, KeyEvent e) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN: {
                int next = Math.max(0, Math.min(optionRows.size() - 1, optionFocus + (code == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1)));
                if (next != optionFocus) { optionFocus = next; sounds.play(Ps4Sounds.MOVE); paintOptions(); }
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                // The release of the hold that opened this menu must not pick the first entry.
                if (e.getRepeatCount() == 0 && !okLongFired) {
                    sounds.play(Ps4Sounds.OK);
                    optionActions.get(optionFocus).run();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
                sounds.play(Ps4Sounds.BACK);
                closeOptions();
                return true;
            default:
                return isOurs(code);
        }
    }

    private boolean removable(String pkg) {
        try {
            android.content.pm.ApplicationInfo info = a.getPackageManager().getApplicationInfo(pkg, 0);
            return (info.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void toggleFavorite(App app) {
        // Record the row as it is first, so the app's own place is known when it leaves the favourites again.
        if (Ps4.savedList(a, "order").isEmpty()) {
            List<String> order = new ArrayList<>();
            for (App x : apps) order.add(x.pkg);
            Ps4.saveList(a, "order", order);
        }
        if (favorites.contains(app.pkg)) favorites.remove(app.pkg);
        else favorites.add(app.pkg);
        // Favourites lead the row: re-sort, keep the focus on the same app, and save.
        String pkg = app.pkg;
        Ps4.saveList(a, "favorites", favorites);
        closeOptions();
        reload();
        for (int i = 0; i < apps.size(); i++) if (apps.get(i).pkg.equals(pkg)) focus = i;
        layout(true);
    }

    // ---- move: the tile lifts and Left / Right carry it along its group ----------------------

    private void startMove() {
        closeOptions();
        moveFrom = focus;
        zone = ZONE_MOVE;
        startLabels.get(focus).setText("\u25C0   Mover   \u25B6");
        tiles.get(focus).getChildAt(2).setBackground(Ps4.box(a, BORDER + 2, Ps4.pal(a).primary, 0));
        tiles.get(focus).animate().translationYBy(-px(14)).setDuration(160).start();
        layout(true);
    }

    private boolean moveKey(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                int start = recentPinned() ? 1 : 0;
                if (focus < start) return true;
                boolean fav = favorites.contains(apps.get(focus).pkg);
                int lo = focus, hi = focus;
                while (lo - 1 >= start && favorites.contains(apps.get(lo - 1).pkg) == fav) lo--;
                while (hi + 1 < apps.size() && favorites.contains(apps.get(hi + 1).pkg) == fav) hi++;
                int to = focus + (code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                if (to < lo || to > hi) return true;
                swap(apps, focus, to);
                swap(tiles, focus, to);
                swap(discs, focus, to);
                swap(stars, focus, to);
                swap(startLabels, focus, to);
                focus = to;
                sounds.play(Ps4Sounds.MOVE);
                layout(true);
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BACK:
                endMove();
                return true;
            default:
                return isOurs(code);
        }
    }

    private static <T> void swap(List<T> list, int i, int j) {
        T t = list.get(i);
        list.set(i, list.get(j));
        list.set(j, t);
    }

    private void endMove() {
        sounds.play(Ps4Sounds.OK);
        startLabels.get(focus).setText("Iniciar");
        tiles.get(focus).getChildAt(2).setBackground(Ps4.box(a, BORDER, Ps4.FOCUS, 0));
        zone = ZONE_ROW;
        saveOrder();
        layout(true);
    }

    private static boolean isOurs(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER: case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_NOTIFICATION: case KeyEvent.KEYCODE_SETTINGS:
                return true;
            default:
                return false;
        }
    }

    private void move(int d) {
        int before;
        if (zone == ZONE_BAR) {
            before = barFocus;
            barFocus = Math.max(0, Math.min(barItems.size() - 1, barFocus + d));
            if (before != barFocus) { sounds.play(Ps4Sounds.MOVE); paintFocus(); }
        } else if (zone == ZONE_SUB) {
            before = subFocus;
            subFocus = Math.max(0, Math.min(subItems.size() - 1, subFocus + d));
            if (before != subFocus) { sounds.play(Ps4Sounds.MOVE); paintFocus(); }
        } else if (zone == ZONE_MEDIA) {
            before = mediaFocus;
            mediaFocus = Math.max(0, Math.min(mediaItems.size() - 1, mediaFocus + d));
            if (before != mediaFocus) { sounds.play(Ps4Sounds.MOVE); paintFocus(); }
        } else {
            int next = Math.max(0, Math.min(tiles.size() - 1, focus + d));
            if (next == focus) return;
            focus = next;
            sounds.play(Ps4Sounds.MOVE);
            layout(true);
        }
    }

    private void setZone(int z) {
        if (z == zone) return;
        sounds.play(Ps4Sounds.MOVE);
        zone = z;
        subFocus = 0;
        if (z == ZONE_MEDIA) mediaFocus = 2;   // play / pause first
        if (z == ZONE_BAR) barFocus = Math.min(barFocus, barItems.size() - 1);
        layout(true);
    }

    private void activate() {
        sounds.play(Ps4Sounds.OK);
        if (zone == ZONE_BAR) barItems.get(barFocus).performClick();
        else if (zone == ZONE_SUB) subItems.get(subFocus).performClick();
        else if (zone == ZONE_MEDIA) mediaItems.get(mediaFocus).performClick();
        else if (focus < apps.size()) launch(a, apps.get(focus));
        else Ps4.open(a, new Intent(a, Ps4AppsActivity.class));
    }

    private void showNotificationCount() {
        int n = Ps4Listener.list().size();
        notifCount.setText(n > 0 ? String.valueOf(n) : "");
    }

    /** Home pressed again: back to the first tile, as on the PS4's PS button. */
    void onNewIntent(Intent intent) {
        if (intent != null && MainActivity.ACTION_LAUNCHER_SETTINGS.equals(intent.getAction())) {
            a.startActivity(new Intent(a, LauncherSettingsActivity.class));
            return;
        }
        if (idle) wake();
        zone = ZONE_ROW;
        focus = 0;
        layout(true);
    }

    void onResume() {
        resumed = true;
        if (builtPalette != Ps4.paletteVersion) {
            a.recreate();
            return;
        }
        if (!dirty && pausedAt > 0) {
            Set<String> pkgs = new HashSet<>();
            for (App app : apps) pkgs.add(app.pkg);
            String last = lastOpened(pkgs);
            if (last != null && lastOpenedAt >= pausedAt) {
                dirty = true;
                zone = ZONE_ROW;
                focus = 0;
            }
        }
        if (dirty) reload();
        if (idle) wake();
        showConnections();
        showNotificationCount();
        if (sessions == null) startMedia();
        else pickMedia(null);
        refreshRunning();
        scheduleIdle();
    }

    void onPause() {
        resumed = false;
        pausedAt = System.currentTimeMillis();
        updateSpin();
        okPending = false;
        handler.removeCallbacks(okLong);
        if (zone == ZONE_MOVE) endMove();
        if (zone == ZONE_OPTIONS) closeOptions();
        handler.removeCallbacks(progressTick);
        mediaProgress.setMoving(false);
        handler.removeCallbacks(goIdle);
    }

    void onDestroy() {
        handler.removeCallbacks(goIdle);
        if (receiverOn) a.unregisterReceiver(packages);
        receiverOn = false;
        Ps4Listener.unwatch(notificationsChanged);
        if (sessions != null) sessions.removeOnActiveSessionsChangedListener(sessionsChanged);
        if (registered != null) registered.unregisterCallback(mediaCallback);
    }
}

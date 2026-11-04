package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * The PS4 Quick Menu: a dark panel (in the accent) over the left two thirds of the screen with Home still showing on
 * the right. Measured on the reference: title at x=94 (58 px thin), a rule at y=133, the entries down the left with
 * the icon at x=113 and the label at x=192 (33 px), a 4 px light focus rectangle, and the user in a footer under a
 * rule at y=949.
 *
 * The middle column shows what the focused entry is about, not just a heading: the volume and the Bluetooth devices
 * under Sound, the connection under Network, the picture mode under Picture, the latest notifications, and so on.
 * Right (or OK) moves into its buttons; an entry that is only a button opens straight away with OK.
 */
public final class Ps4QuickMenuActivity extends Activity {
    static final String EXTRA_SECTION = "com.gathin.tvhome.extra.PS4_SECTION";
    static final String SECTION_POWER = "power", SECTION_PROFILE = "profile", SECTION_THEME = "theme",
            SECTION_SOUND = "sound";
    private static final String LAUNCHERX = "com.google.android.apps.tv.launcherx";

    /** One line of an entry's content: a heading, a fact, a level bar, or a button. */
    private static final class Item {
        static final int SECTION = 0, INFO = 1, BAR = 2, ACTION = 3;
        final int kind;
        int icon, swatch;
        String label, value = "";
        float level;
        Runnable run;

        Item(int kind, String label) {
            this.kind = kind;
            this.label = label;
        }
    }

    private interface Content {
        List<Item> build();
    }

    private static final class Entry {
        final int icon;
        final String label;
        final Content content;
        /** True when the entry is one button: OK on it runs that button. */
        final boolean direct;

        Entry(int icon, String label, Content content, boolean direct) {
            this.icon = icon;
            this.label = label;
            this.content = content;
            this.direct = direct;
        }
    }

    private static final int AUTO_SWATCH = 1;

    private final List<Entry> entries = new ArrayList<>();
    private final List<View> rows = new ArrayList<>();
    private final List<View> actionViews = new ArrayList<>();
    private final List<Item> actions = new ArrayList<>();
    private TextView heading;
    private LinearLayout content;
    private int focus, actionFocus = -1;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    private int px(float ref) {
        return Ps4.px(this, ref);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        makeEntries();
        build();
        String section = getIntent().getStringExtra(EXTRA_SECTION);
        int start = SECTION_PROFILE.equals(section) ? 0 : SECTION_THEME.equals(section) ? 1
                : SECTION_SOUND.equals(section) ? 2 : SECTION_POWER.equals(section) ? 3 : 2;
        select(start);
        if (section != null) enterContent(0);
    }

    // ---- entries and their content -------------------------------------------------------------

    private void makeEntries() {
        entries.clear();
        entries.add(new Entry(Ps4.Icon.PROFILE, Ps4.userName(this), () -> {
            List<Item> out = new ArrayList<>();
            out.add(action(Ps4.Icon.PROFILE, "Cambiar de perfil", this::profiles));
            out.add(action(Ps4.Icon.CUSTOMIZE, "Personalizar el inicio", this::launcherSettings));
            return out;
        }, false));
        entries.add(new Entry(Ps4.Icon.PALETTE, "Tema", this::themeItems, false));
        entries.add(new Entry(Ps4.Icon.SOUND, "Sonido/dispositivos", this::soundItems, false));
        entries.add(new Entry(Ps4.Icon.POWER, "Energía", () -> {
            List<Item> out = new ArrayList<>();
            out.add(action(Ps4.Icon.POWER, "Entrar en modo de reposo", this::sleep));
            out.add(action(Ps4.Icon.POWER, "Apagar o reiniciar", this::powerMenu));
            return out;
        }, false));
        entries.add(new Entry(Ps4.Icon.NETWORK, "Red", this::networkItems, false));
        entries.add(new Entry(Ps4.Icon.PICTURE, "Imagen", this::pictureItems, false));
        entries.add(new Entry(Ps4.Icon.INFO, "Notificaciones", this::notificationItems, false));
        entries.add(new Entry(Ps4.Icon.LIBRARY, "Biblioteca", () -> {
            List<Item> out = new ArrayList<>();
            out.add(info("Aplicaciones", String.valueOf(Ps4Home.loadApps(this, false).size())));
            out.add(action(Ps4.Icon.LIBRARY, "Abrir la biblioteca", () -> go(new Intent(this, Ps4AppsActivity.class))));
            return out;
        }, true));
        entries.add(new Entry(Ps4.Icon.CUSTOMIZE, "Personalizar", () -> {
            List<Item> out = new ArrayList<>();
            out.add(action(Ps4.Icon.CUSTOMIZE, "Abrir los ajustes del inicio", this::launcherSettings));
            return out;
        }, true));
        entries.add(new Entry(Ps4.Icon.GEAR, "Configuración", () -> {
            List<Item> out = new ArrayList<>();
            out.add(action(Ps4.Icon.GEAR, "Abrir la configuración del televisor", () -> settings(Settings.ACTION_SETTINGS)));
            return out;
        }, true));
    }

    private static Item action(int icon, String label, Runnable run) {
        Item i = new Item(Item.ACTION, label);
        i.icon = icon;
        i.run = run;
        return i;
    }

    private static Item info(String label, String value) {
        Item i = new Item(Item.INFO, label);
        i.value = value;
        return i;
    }

    private List<Item> themeItems() {
        List<Item> out = new ArrayList<>();
        String chosen = Ps4.accent(this);
        for (String[] a : Ps4.ACCENTS) {
            Item o = action(0, a[0], () -> applyAccent(a[1]));
            o.swatch = "auto".equals(a[1]) ? AUTO_SWATCH : android.graphics.Color.parseColor(a[1]);
            if (a[1].equalsIgnoreCase(chosen)) o.value = "✓ Actual";
            out.add(o);
        }
        return out;
    }

    private List<Item> soundItems() {
        List<Item> out = new ArrayList<>();
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            if (am.isVolumeFixed()) {
                out.add(info("Volumen", "Lo controla el televisor"));
            } else {
                int v = am.getStreamVolume(AudioManager.STREAM_MUSIC), max = Math.max(1, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                boolean muted = am.isStreamMute(AudioManager.STREAM_MUSIC);
                Item bar = new Item(Item.BAR, "Volumen");
                bar.level = muted ? 0 : v / (float) max;
                bar.value = muted ? "Silenciado" : v + " / " + max;
                out.add(bar);
                out.add(action(Ps4.Icon.VOLUME_UP, "Subir volumen", () -> volume(AudioManager.ADJUST_RAISE)));
                out.add(action(Ps4.Icon.VOLUME_DOWN, "Bajar volumen", () -> volume(AudioManager.ADJUST_LOWER)));
                out.add(action(Ps4.Icon.MUTE, muted ? "Activar el sonido" : "Silenciar", () -> volume(AudioManager.ADJUST_TOGGLE_MUTE)));
            }
        }
        out.add(new Item(Item.SECTION, "Dispositivos Bluetooth"));
        List<String[]> devices = Ps4.bluetooth(this);
        if (devices.isEmpty()) out.add(info("Sin dispositivos emparejados", ""));
        for (String[] d : devices) {
            Item b = action(Ps4.Icon.BLUETOOTH, d[0], () -> settings(Settings.ACTION_BLUETOOTH_SETTINGS));
            b.value = d[1].isEmpty() ? "" : "Conectado";
            out.add(b);
        }
        out.add(action(Ps4.Icon.GEAR, "Ajustes de sonido", () -> settings(Settings.ACTION_SOUND_SETTINGS)));
        return out;
    }

    private List<Item> networkItems() {
        List<Item> out = new ArrayList<>();
        Ps4.Net n = Ps4.net(this);
        out.add(info("Conexión", !n.online ? "Sin conexión" : n.ethernet ? "Ethernet (cable)" : n.wifi ? "Wi-Fi" : "Otra"));
        if (n.online) {
            out.add(info("Red", n.name));
            if (n.wifi) {
                Item bar = new Item(Item.BAR, "Señal");
                bar.level = n.percent / 100f;
                bar.value = n.percent + " %";
                out.add(bar);
            }
            if (!n.address.isEmpty()) out.add(info("Dirección IP", n.address));
            out.add(info("Internet", n.validated ? "Conectado" : "Sin acceso"));
        }
        out.add(action(Ps4.Icon.NETWORK, "Ajustes de red", () -> settings(Settings.ACTION_WIFI_SETTINGS)));
        return out;
    }

    private List<Item> pictureItems() {
        List<Item> out = new ArrayList<>();
        Display d = getWindowManager().getDefaultDisplay();
        Display.Mode m = d.getMode();
        out.add(info("Resolución", m.getPhysicalWidth() + " × " + m.getPhysicalHeight()));
        out.add(info("Frecuencia", Math.round(m.getRefreshRate()) + " Hz"));
        StringBuilder hdr = new StringBuilder();
        Display.HdrCapabilities caps = d.getHdrCapabilities();
        if (caps != null) {
            for (int t : caps.getSupportedHdrTypes()) {
                String name = t == Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION ? "Dolby Vision"
                        : t == Display.HdrCapabilities.HDR_TYPE_HDR10 ? "HDR10"
                        : t == Display.HdrCapabilities.HDR_TYPE_HLG ? "HLG" : t == 4 ? "HDR10+" : null;
                if (name != null) hdr.append(hdr.length() > 0 ? " · " : "").append(name);
            }
        }
        out.add(info("HDR", hdr.length() > 0 ? hdr.toString() : "No disponible"));
        out.add(action(Ps4.Icon.PICTURE, "Ajustes de imagen", () -> settings(Settings.ACTION_DISPLAY_SETTINGS)));
        return out;
    }

    private List<Item> notificationItems() {
        List<Item> out = new ArrayList<>();
        List<StatusBarNotification> list = Ps4Listener.list();
        if (list.isEmpty()) out.add(info("No tienes notificaciones", ""));
        for (int i = 0; i < Math.min(4, list.size()); i++) {
            StatusBarNotification sbn = list.get(i);
            out.add(info(Ps4Listener.title(sbn), appName(sbn.getPackageName())));
        }
        out.add(action(Ps4.Icon.INFO, list.isEmpty() ? "Abrir notificaciones" : "Ver todas (" + list.size() + ")",
                () -> go(new Intent(this, Ps4NotificationsActivity.class))));
        return out;
    }

    private String appName(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    // ---- layout --------------------------------------------------------------------------------

    private void build() {
        rows.clear();
        FrameLayout root = new FrameLayout(this);
        FrameLayout panel = new FrameLayout(this);
        panel.setBackgroundColor(Ps4.pal(this).panel);
        root.addView(panel, new FrameLayout.LayoutParams(px(1261), -1));

        TextView title = Ps4.text(this, 58, Ps4.thin(this), Ps4.TEXT);
        title.setText("Menú rápido");
        panel.addView(title, place(94, 52, -2, 84));
        panel.addView(rule(), place(0, 133, 1236, 2));

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            float y = i == 0 ? 186 : 290 + (i - 1) * 73f;
            View r = row(e.icon, 0, e.label, "", 33, 52);
            panel.addView(r, place(97, y - 38, 427, 76));
            rows.add(r);
        }
        panel.addView(rule(), place(110, 240, 401, 2));

        // Middle column: the entry's name as a plain heading over a rule, then its content.
        heading = Ps4.text(this, 40, Ps4.thin(this), Ps4.TEXT);
        panel.addView(heading, place(600, 150, 620, 60));
        panel.addView(rule(), place(600, 222, 626, 2));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        panel.addView(content, place(590, 240, 646, 700));

        panel.addView(rule(), place(0, 949, 1236, 2));
        View av = Ps4.avatar(this, 50);
        panel.addView(av, place(94, 953, 50, 50));
        TextView name = Ps4.text(this, 30, Ps4.light(this), Ps4.TEXT);
        name.setText(Ps4.userName(this));
        panel.addView(name, place(158, 953, -2, 50));
        setContentView(root);
    }

    private FrameLayout.LayoutParams place(float x, float y, float w, float h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w < 0 ? -2 : px(w), h < 0 ? -2 : px(h));
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        return lp;
    }

    private View rule() {
        View v = new View(this);
        v.setBackgroundColor(0x664A5380);
        return v;
    }

    private View row(int icon, int swatch, String label, String trailing, float textSize, float iconSize) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv = new ImageView(this);
        Drawable d;
        if (swatch == AUTO_SWATCH) {
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setGradientType(android.graphics.drawable.GradientDrawable.SWEEP_GRADIENT);
            g.setColors(new int[]{0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA, 0xFFE53935});
            d = g;
        } else if (swatch != 0) {
            android.graphics.drawable.GradientDrawable g = Ps4.circle(swatch);
            g.setStroke(Math.max(1, px(2)), 0x66FFFFFF);
            d = g;
        } else {
            d = new Ps4.Icon(icon);
        }
        iv.setImageDrawable(d);
        float size = swatch != 0 ? iconSize * 0.85f : iconSize;
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(px(size), px(size));
        ip.leftMargin = px(16);
        r.addView(iv, ip);
        TextView t = Ps4.text(this, textSize, Ps4.light(this), Ps4.TEXT);
        t.setText(label);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.leftMargin = px(24);
        r.addView(t, tp);
        if (!trailing.isEmpty()) {
            TextView tr = Ps4.text(this, 25, Ps4.light(this), Ps4.pal(this).primary);
            tr.setText(trailing);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
            cp.rightMargin = px(20);
            r.addView(tr, cp);
        }
        return r;
    }

    private void select(int i) {
        focus = i;
        actionFocus = -1;
        for (int k = 0; k < rows.size(); k++) {
            rows.get(k).setBackground(k == focus ? Ps4.box(this, 4, 0xFFC9CDD8, 0) : null);
        }
        fillContent();
    }

    /** Builds the focused entry's content; keepAction keeps the focus on the same button after a refresh. */
    private void fillContent() {
        Entry e = entries.get(focus);
        heading.setText(e.label);
        content.removeAllViews();
        actions.clear();
        actionViews.clear();
        Ps4.Palette pl = Ps4.pal(this);
        for (Item it : e.content.build()) {
            switch (it.kind) {
                case Item.SECTION: {
                    TextView t = Ps4.text(this, 22, UiFont.get(this, R.string.font_regular), Ps4.TEXT_DIM);
                    t.setText(it.label.toUpperCase());
                    t.setLetterSpacing(0.12f);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(56));
                    lp.leftMargin = px(16);
                    t.setGravity(Gravity.BOTTOM);
                    content.addView(t, lp);
                    break;
                }
                case Item.INFO: {
                    LinearLayout r = new LinearLayout(this);
                    r.setGravity(Gravity.CENTER_VERTICAL);
                    TextView l = Ps4.text(this, 27, Ps4.light(this), Ps4.TEXT_DIM);
                    l.setText(it.label);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                    lp.leftMargin = px(16);
                    r.addView(l, lp);
                    TextView v = Ps4.text(this, 28, Ps4.light(this), Ps4.TEXT);
                    v.setText(it.value);
                    LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-2, -2);
                    vp.rightMargin = px(20);
                    r.addView(v, vp);
                    content.addView(r, new LinearLayout.LayoutParams(-1, px(58)));
                    break;
                }
                case Item.BAR: {
                    LinearLayout box = new LinearLayout(this);
                    box.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout r = new LinearLayout(this);
                    TextView l = Ps4.text(this, 27, Ps4.light(this), Ps4.TEXT_DIM);
                    l.setText(it.label);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                    lp.leftMargin = px(16);
                    r.addView(l, lp);
                    TextView v = Ps4.text(this, 28, Ps4.light(this), Ps4.TEXT);
                    v.setText(it.value);
                    LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-2, -2);
                    vp.rightMargin = px(20);
                    r.addView(v, vp);
                    box.addView(r);
                    FrameLayout track = new FrameLayout(this);
                    track.setBackground(rounded(0x33FFFFFF, 4));
                    View fill = new View(this);
                    fill.setBackground(rounded(pl.primary, 4));
                    fill.setPivotX(0);
                    fill.setScaleX(Math.max(0f, Math.min(1f, it.level)));
                    track.addView(fill, new FrameLayout.LayoutParams(-1, -1));
                    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, px(8));
                    tp.leftMargin = px(16);
                    tp.rightMargin = px(20);
                    tp.topMargin = px(12);
                    box.addView(track, tp);
                    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
                    bp.bottomMargin = px(14);
                    content.addView(box, bp);
                    break;
                }
                default: {
                    View r = row(it.icon, it.swatch, it.label, it.value, 30, 38);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(68));
                    lp.bottomMargin = px(4);
                    content.addView(r, lp);
                    actions.add(it);
                    actionViews.add(r);
                    break;
                }
            }
        }
        paintActions();
    }

    private android.graphics.drawable.GradientDrawable rounded(int color, float radius) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(radius));
        return g;
    }

    private void enterContent(int i) {
        if (actions.isEmpty()) return;
        actionFocus = Math.max(0, Math.min(actions.size() - 1, i));
        rows.get(focus).setBackground(Ps4.box(this, 2, 0x80C9CDD8, 0));
        paintActions();
    }

    private void paintActions() {
        for (int k = 0; k < actionViews.size(); k++) {
            actionViews.get(k).setBackground(k == actionFocus ? Ps4.box(this, 4, 0xFFC9CDD8, 0x14FFFFFF) : null);
        }
    }

    // ---- keys ----------------------------------------------------------------------------------

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            switch (code) {
                case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER: case KeyEvent.KEYCODE_BACK:
                    return true;
                default:
                    return super.dispatchKeyEvent(event);
            }
        }
        Ps4Sounds snd = Ps4Sounds.get(this);
        boolean inside = actionFocus >= 0;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:
                snd.play(Ps4Sounds.MOVE);
                if (inside) { actionFocus = Math.max(0, actionFocus - 1); paintActions(); }
                else select(Math.max(0, focus - 1));
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                snd.play(Ps4Sounds.MOVE);
                if (inside) { actionFocus = Math.min(actions.size() - 1, actionFocus + 1); paintActions(); }
                else select(Math.min(entries.size() - 1, focus + 1));
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!inside && !actions.isEmpty()) { snd.play(Ps4Sounds.MOVE); enterContent(0); }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (inside) { snd.play(Ps4Sounds.MOVE); select(focus); }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (event.getRepeatCount() > 0) return true;
                snd.play(Ps4Sounds.OK);
                if (inside) actions.get(actionFocus).run.run();
                else if (entries.get(focus).direct) actions.get(actions.size() - 1).run.run();
                else enterContent(0);
                return true;
            case KeyEvent.KEYCODE_BACK:
                snd.play(Ps4Sounds.BACK);
                if (inside) select(focus);
                else finish();
                return true;
            default:
                // The remote's Settings button again closes the menu, as pressing the PS button twice does.
                if (code == KeyEvent.KEYCODE_NOTIFICATION || code == KeyEvent.KEYCODE_SETTINGS) {
                    finish();
                    return true;
                }
                return super.dispatchKeyEvent(event);
        }
    }

    // ---- actions -------------------------------------------------------------------------------

    private void refresh() {
        int keep = actionFocus;
        fillContent();
        if (keep >= 0) enterContent(keep);
    }

    private void volume(int direction) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_PLAY_SOUND);
        refresh();
    }

    private void applyAccent(String value) {
        Ps4.setAccent(this, value);
        int keep = actionFocus;
        int entry = focus;
        makeEntries();
        build();
        select(entry);
        enterContent(keep);
    }

    private void launcherSettings() {
        go(new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS));
    }

    /**
     * Google TV's own profile chooser (with the accounts' photos) lives in its stock launcher. With that package
     * enabled, open it; otherwise the Accounts and profiles page of Settings.
     */
    private void profiles() {
        Intent chooser = new Intent().setClassName(LAUNCHERX, LAUNCHERX + ".profile.chooser.ProfileChooserActivity");
        try {
            if (getPackageManager().getApplicationInfo(LAUNCHERX, 0).enabled) {
                startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
                return;
            }
        } catch (Exception ignored) {
        }
        settings(Settings.ACTION_SYNC_SETTINGS);
    }

    private void settings(String action) {
        go(pick(action));
    }

    /**
     * One settings screen, never Android's chooser: with two answers (the onn's Sound settings has the TV settings
     * app and a vendor one) the TV settings app wins; with none (Display on the onn), the TV's display-and-sound
     * screen, then the launcher's usual fallback.
     */
    private Intent pick(String action) {
        PackageManager pm = getPackageManager();
        String[] tries = Settings.ACTION_DISPLAY_SETTINGS.equals(action)
                ? new String[]{action, Settings.ACTION_SOUND_SETTINGS} : new String[]{action};
        for (String a : tries) {
            List<android.content.pm.ResolveInfo> found = pm.queryIntentActivities(new Intent(a), PackageManager.MATCH_DEFAULT_ONLY);
            android.content.pm.ResolveInfo best = null;
            for (android.content.pm.ResolveInfo ri : found) {
                if (best == null || "com.android.tv.settings".equals(ri.activityInfo.packageName)) best = ri;
            }
            if (best != null) return new Intent(a).setClassName(best.activityInfo.packageName, best.activityInfo.name);
        }
        return SystemSettings.intent(this, action);
    }

    private void go(Intent intent) {
        Ps4.open(this, intent);
        finish();
    }

    private void sleep() {
        if (!HomeKeyService.sleep()) {
            Toast.makeText(this, "Activa el servicio de botones del launcher para usar el reposo.", Toast.LENGTH_LONG).show();
            return;
        }
        finish();
    }

    private void powerMenu() {
        if (!HomeKeyService.canOpenPowerMenu()) {
            settings(Settings.ACTION_SETTINGS);
            return;
        }
        HomeKeyService.openPowerMenu();
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }
}

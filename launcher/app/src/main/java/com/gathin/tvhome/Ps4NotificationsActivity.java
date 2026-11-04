package com.gathin.tvhome;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The PS4 notifications screen, built like the list screen: a title with the count, "Clear all" on the right of the
 * title, cards 1704 x 130 with the app's icon, title, text and age, and on each card that can be cleared a round
 * "Clear" button. Up/Down move between cards, Right reaches the card's Clear button, Up from the first card reaches
 * Clear all. Needs notification access (Ps4Listener).
 */
public final class Ps4NotificationsActivity extends Activity {
    private static final float LIST_X = 108, LIST_TOP = 160, LIST_BOTTOM = 930, CARD_W = 1704, CARD_H = 130, PITCH = 142;

    private final List<StatusBarNotification> items = new ArrayList<>();
    private final List<View> cards = new ArrayList<>();
    private final List<View> clears = new ArrayList<>();
    private FrameLayout list;
    private TextView count, empty;
    private View clearAll;
    /** -1 = Clear all; otherwise a card, with onClear when its Clear button has the focus. */
    private int focus, scrollRef;
    private boolean onClear;
    private final Runnable changed = this::fill;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    private int px(float ref) {
        return Ps4.px(this, ref);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(Ps4.screenBackground(this));
        TextView title = Ps4.text(this, 60, Ps4.thin(this), Ps4.TEXT);
        title.setText("Notificaciones");
        root.addView(title, place(108, 53, -2, 80));
        title.measure(0, 0);
        count = Ps4.text(this, 32, Ps4.light(this), Ps4.TEXT);
        FrameLayout.LayoutParams cp = place(0, 70, -2, 46);
        cp.leftMargin = px(108 + 24) + title.getMeasuredWidth();
        root.addView(count, cp);
        clearAll = pill(Ps4.Icon.CLEAR, "Borrar todas");
        clearAll.setOnClickListener(v -> {
            for (StatusBarNotification sbn : new ArrayList<>(items)) Ps4Listener.dismiss(sbn);
        });
        FrameLayout.LayoutParams ap = place(0, 62, -2, 64);
        ap.gravity = Gravity.END | Gravity.TOP;
        ap.rightMargin = px(108);
        root.addView(clearAll, ap);
        root.addView(rule(), place(0, 135, 1920, 2));

        FrameLayout clip = new FrameLayout(this);
        list = new FrameLayout(this);
        clip.addView(list, new FrameLayout.LayoutParams(-1, -2));
        root.addView(clip, place(0, LIST_TOP, 1920, LIST_BOTTOM - LIST_TOP));
        empty = Ps4.text(this, 32, Ps4.light(this), Ps4.TEXT);
        empty.setGravity(Gravity.CENTER);
        root.addView(empty, place(0, 500, 1920, 60));

        root.addView(rule(), place(0, 945, 1920, 2));
        LinearLayout hints = new LinearLayout(this);
        hints.setGravity(Gravity.CENTER_VERTICAL);
        hint(hints, Ps4.Icon.OK, "Abrir o borrar");
        hint(hints, Ps4.Icon.MOVE, "Ir a Borrar");
        hint(hints, Ps4.Icon.BACK, "Atrás");
        root.addView(hints, place(108, 948, -2, 50));
        View av = Ps4.avatar(this, 46);
        root.addView(av, place(1420, 950, 46, 46));
        TextView name = Ps4.text(this, 30, Ps4.light(this), Ps4.TEXT);
        name.setText(Ps4.userName(this));
        root.addView(name, place(1480, 948, -2, 50));
        setContentView(root);
        Ps4Listener.watch(changed);
        fill();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Ps4Listener.unwatch(changed);
    }

    private FrameLayout.LayoutParams place(float x, float y, float w, float h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w < 0 ? -2 : px(w), h < 0 ? -2 : px(h));
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        return lp;
    }

    private View rule() {
        View v = new View(this);
        v.setBackgroundColor(0x667D92D0);
        return v;
    }

    /** A rounded button with an icon and a label (Clear all, and each card's Clear). */
    private View pill(int icon, String label) {
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(px(22), 0, px(26), 0);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(0x26FFFFFF);
        g.setCornerRadius(px(32));
        b.setBackground(g);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(new Ps4.Icon(icon));
        b.addView(iv, new LinearLayout.LayoutParams(px(26), px(26)));
        TextView t = Ps4.text(this, 26, Ps4.light(this), Ps4.TEXT);
        t.setText(label);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.leftMargin = px(12);
        b.addView(t, tp);
        return b;
    }

    private void hint(LinearLayout hints, int icon, String label) {
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(new Ps4.Icon(icon));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(px(34), px(34));
        ip.leftMargin = hints.getChildCount() == 0 ? 0 : px(40);
        hints.addView(iv, ip);
        TextView t = Ps4.text(this, 29, Ps4.light(this), Ps4.TEXT);
        t.setText(label);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.leftMargin = px(10);
        hints.addView(t, tp);
    }

    private void fill() {
        items.clear();
        items.addAll(Ps4Listener.list());
        list.removeAllViews();
        cards.clear();
        clears.clear();
        count.setText(items.isEmpty() ? "" : String.valueOf(items.size()));
        if (!Ps4Listener.connected()) {
            empty.setText("Falta el permiso de notificaciones del launcher.");
        } else {
            empty.setText(items.isEmpty() ? "No tienes notificaciones." : "");
        }
        boolean anyClearable = false;
        for (int i = 0; i < items.size(); i++) {
            StatusBarNotification sbn = items.get(i);
            FrameLayout c = card(sbn);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(CARD_W), px(CARD_H));
            lp.leftMargin = px(LIST_X);
            lp.topMargin = px(6 + i * PITCH);
            list.addView(c, lp);
            cards.add(c);
            View clear = null;
            if (sbn.isClearable()) {
                anyClearable = true;
                clear = pill(Ps4.Icon.CLEAR, "Borrar");
                FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(-2, px(64), Gravity.END | Gravity.BOTTOM);
                xp.rightMargin = px(18);
                xp.bottomMargin = px(14);
                c.addView(clear, xp);
            }
            clears.add(clear);
        }
        // The list is as tall as its cards, so scrolling it up shows the ones below (a list sized to the screen
        // clipped them, and the focus moved down past what could be seen).
        list.getLayoutParams().height = px(12 + items.size() * PITCH);
        list.requestLayout();
        clearAll.setVisibility(anyClearable ? View.VISIBLE : View.GONE);
        if (focus >= items.size()) focus = items.size() - 1;
        if (focus < 0 && !anyClearable) focus = 0;
        if (focus >= 0 && (focus >= clears.size() || clears.get(focus) == null)) onClear = false;
        paint();
    }

    private FrameLayout card(StatusBarNotification sbn) {
        FrameLayout c = new FrameLayout(this);
        FrameLayout art = new FrameLayout(this);
        art.setBackgroundColor(Ps4.pal(this).tile);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon(sbn));
        art.addView(iv, new FrameLayout.LayoutParams(px(76), px(76), Gravity.CENTER));
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(px(CARD_H - 2), px(CARD_H - 2));
        ap.leftMargin = ap.topMargin = px(1);
        c.addView(art, ap);
        TextView title = Ps4.text(this, 32, Ps4.light(this), Ps4.TEXT);
        title.setText(Ps4Listener.title(sbn));
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(px(1150), -2);
        tp.leftMargin = px(CARD_H + 20);
        tp.topMargin = px(16);
        c.addView(title, tp);
        TextView text = Ps4.text(this, 25, Ps4.light(this), Ps4.TEXT_DIM);
        text.setText(Ps4Listener.text(sbn));
        FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(px(1150), -2, Gravity.BOTTOM);
        xp.leftMargin = px(CARD_H + 20);
        xp.bottomMargin = px(18);
        c.addView(text, xp);
        TextView when = Ps4.text(this, 24, Ps4.light(this), Ps4.TEXT_DIM);
        when.setText(appName(sbn.getPackageName()) + " · " + DateUtils.getRelativeTimeSpanString(sbn.getPostTime(),
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                + (sbn.isClearable() ? "" : " · Permanente"));
        FrameLayout.LayoutParams wp = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.TOP);
        wp.rightMargin = px(22);
        wp.topMargin = px(16);
        c.addView(when, wp);
        return c;
    }

    private Drawable icon(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            Icon large = n.getLargeIcon();
            if (large != null) {
                Drawable d = large.loadDrawable(this);
                if (d != null) return d;
            }
            return getPackageManager().getApplicationIcon(sbn.getPackageName());
        } catch (Exception e) {
            return new Ps4.Icon(Ps4.Icon.INFO);
        }
    }

    private String appName(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private void paint() {
        Ps4.Palette pl = Ps4.pal(this);
        for (int i = 0; i < cards.size(); i++) {
            boolean on = i == focus && !onClear;
            cards.get(i).setBackground(on ? Ps4.box(this, 3, Ps4.FOCUS, pl.cardFocus)
                    : Ps4.box(this, 1, pl.cardLine, (pl.card & 0xFFFFFF) | 0x66000000));
            View clear = clears.get(i);
            if (clear != null) ring(clear, i == focus && onClear);
        }
        ring(clearAll, focus == -1);
        if (focus < 0) return;
        float top = focus * PITCH, visible = LIST_BOTTOM - LIST_TOP - CARD_H - 12;
        if (top - scrollRef > visible) scrollRef = (int) (top - visible);
        if (top < scrollRef) scrollRef = (int) top;
        list.animate().translationY(-px(scrollRef)).setDuration(140).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void ring(View v, boolean on) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(on ? 0x40FFFFFF : 0x26FFFFFF);
        g.setCornerRadius(px(32));
        if (on) g.setStroke(px(3), Ps4.FOCUS);
        v.setBackground(g);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        boolean ours = code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_BACK;
        if (!ours) return super.dispatchKeyEvent(e);
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        Ps4Sounds snd = Ps4Sounds.get(this);
        boolean clearAllShown = clearAll.getVisibility() == View.VISIBLE;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (focus > 0) { focus--; snd.play(Ps4Sounds.MOVE); }
                else if (focus == 0 && clearAllShown) { focus = -1; onClear = false; snd.play(Ps4Sounds.MOVE); }
                if (focus >= 0 && clears.get(focus) == null) onClear = false;
                paint();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (focus < cards.size() - 1) {
                    focus++;
                    if (clears.get(focus) == null) onClear = false;
                    snd.play(Ps4Sounds.MOVE);
                    paint();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (focus >= 0 && !onClear && clears.get(focus) != null) { onClear = true; snd.play(Ps4Sounds.MOVE); paint(); }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (onClear) { onClear = false; snd.play(Ps4Sounds.MOVE); paint(); }
                break;
            case KeyEvent.KEYCODE_MENU:
                if (focus >= 0 && focus < items.size()) { snd.play(Ps4Sounds.BACK); Ps4Listener.dismiss(items.get(focus)); }
                break;
            case KeyEvent.KEYCODE_BACK:
                snd.play(Ps4Sounds.BACK);
                finish();
                break;
            default:
                if (e.getRepeatCount() > 0) break;
                if (focus == -1) { snd.play(Ps4Sounds.BACK); clearAll.performClick(); break; }
                if (focus >= items.size()) break;
                if (onClear) {
                    snd.play(Ps4Sounds.BACK);
                    Ps4Listener.dismiss(items.get(focus));
                } else {
                    snd.play(Ps4Sounds.OK);
                    open(items.get(focus));
                }
                break;
        }
        return true;
    }

    private void open(StatusBarNotification sbn) {
        PendingIntent pi = sbn.getNotification().contentIntent;
        if (pi == null) return;
        try {
            pi.send();
            if ((sbn.getNotification().flags & Notification.FLAG_AUTO_CANCEL) != 0) Ps4Listener.dismiss(sbn);
            finish();
        } catch (PendingIntent.CanceledException ignored) {
        }
    }
}

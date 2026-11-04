package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
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
 * The PS4 list screen (Friends on the reference), as the app library: a title with a count and a rule at y=135,
 * a side menu at x=158 whose chosen entry is larger and brighter, cards 1103 x 130 from x=676 with a 1 px border
 * (3 px light when focused) and a square thumbnail, a blue dot left of each TV app, and a hint bar under a rule at
 * y=945 with the user on the right.
 */
public final class Ps4AppsActivity extends Activity {
    private static final float LIST_X = 676, LIST_TOP = 160, LIST_BOTTOM = 930, CARD_H = 130, PITCH = 142;

    private final List<List<Ps4Home.App>> lists = new ArrayList<>();
    private final List<View> navRows = new ArrayList<>();
    private final List<View> cards = new ArrayList<>();
    private FrameLayout list;
    private int nav, card, scrollRef;
    private boolean inNav;

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
        lists.add(Ps4Home.loadApps(this, false));
        lists.add(Ps4Home.loadApps(this, true));
        FrameLayout root = new FrameLayout(this);
        root.setBackground(Ps4.screenBackground(this));

        TextView title = Ps4.text(this, 60, Ps4.thin(this), Ps4.TEXT);
        title.setText("Biblioteca");
        root.addView(title, place(108, 53, -2, 80));
        title.measure(0, 0);
        LinearLayout count = new LinearLayout(this);
        count.setGravity(Gravity.CENTER_VERTICAL);
        ImageView ci = new ImageView(this);
        ci.setImageDrawable(new Ps4.Icon(Ps4.Icon.LIBRARY));
        count.addView(ci, new LinearLayout.LayoutParams(px(30), px(30)));
        TextView cn = Ps4.text(this, 32, Ps4.light(this), Ps4.TEXT);
        cn.setText(String.valueOf(lists.get(1).size()));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.leftMargin = px(10);
        count.addView(cn, cp);
        FrameLayout.LayoutParams countAt = place(0, 70, -2, 46);
        countAt.leftMargin = px(108 + 20) + title.getMeasuredWidth();
        root.addView(count, countAt);
        root.addView(rule(), place(0, 135, 1920, 2));

        navRows.add(navRow(Ps4.Icon.LIBRARY, "Aplicaciones de TV", lists.get(0).size()));
        navRows.add(navRow(Ps4.Icon.GEAR, "Todas las aplicaciones", lists.get(1).size()));
        for (int i = 0; i < navRows.size(); i++) root.addView(navRows.get(i), place(140, 168 + i * 85, 440, 80));
        root.addView(rule(), place(158, 168 + navRows.size() * 85 + 8, 405, 2));

        // The card list, clipped between the rules.
        FrameLayout clip = new FrameLayout(this);
        clip.setClipChildren(true);
        list = new FrameLayout(this);
        clip.addView(list, new FrameLayout.LayoutParams(-1, -2));
        root.addView(clip, place(600, LIST_TOP, 1220, LIST_BOTTOM - LIST_TOP));

        root.addView(rule(), place(0, 945, 1920, 2));
        LinearLayout hints = new LinearLayout(this);
        hints.setGravity(Gravity.CENTER_VERTICAL);
        hint(hints, Ps4.Icon.OK, "Abrir");
        hint(hints, Ps4.Icon.BACK, "Atrás");
        hint(hints, Ps4.Icon.MENU, "Información de la aplicación");
        root.addView(hints, place(108, 948, -2, 50));
        View av = Ps4.avatar(this, 46);
        root.addView(av, place(1420, 950, 46, 46));
        TextView name = Ps4.text(this, 30, Ps4.light(this), Ps4.TEXT);
        name.setText(Ps4.userName(this));
        root.addView(name, place(1480, 948, -2, 50));
        setContentView(root);
        showList(0);
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

    private View navRow(int icon, String label, int n) {
        FrameLayout r = new FrameLayout(this);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(new Ps4.Icon(icon));
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(px(42), px(42), Gravity.CENTER_VERTICAL);
        ip.leftMargin = px(18);
        r.addView(iv, ip);
        TextView t = Ps4.text(this, 27, Ps4.light(this), Ps4.TEXT_DIM);
        t.setText(label);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(px(300), -1);
        tp.leftMargin = px(83);
        r.addView(t, tp);
        LinearLayout count = new LinearLayout(this);
        count.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(this);
        dot.setBackground(Ps4.circle(Ps4.pal(this).online));
        count.addView(dot, new LinearLayout.LayoutParams(px(14), px(14)));
        TextView c = Ps4.text(this, 24, Ps4.light(this), Ps4.TEXT);
        c.setText(String.valueOf(n));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.leftMargin = px(8);
        count.addView(c, cp);
        FrameLayout.LayoutParams ccp = new FrameLayout.LayoutParams(-2, -1, Gravity.END);
        ccp.rightMargin = px(20);
        r.addView(count, ccp);
        return r;
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

    private void showList(int which) {
        nav = which;
        card = 0;
        scrollRef = 0;
        list.removeAllViews();
        cards.clear();
        List<Ps4Home.App> apps = lists.get(which);
        for (int i = 0; i < apps.size(); i++) {
            Ps4Home.App app = apps.get(i);
            View c = card(app);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(1103), px(CARD_H));
            lp.leftMargin = px(LIST_X - 600);
            lp.topMargin = px(6 + i * PITCH);
            list.addView(c, lp);
            if (app.leanback) {
                View dot = new View(this);
                dot.setBackground(Ps4.circle(Ps4.pal(this).online));
                FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(px(18), px(18));
                dp.leftMargin = px(652 - 600 - 9);
                dp.topMargin = px(6 + i * PITCH + CARD_H / 2 - 9);
                list.addView(dot, dp);
            }
            cards.add(c);
        }
        // As tall as its cards: sized to the screen, the list clipped every card below it, so the focus moved
        // down past what could be seen while the scroll seemed to do nothing.
        list.getLayoutParams().height = px(12 + apps.size() * PITCH);
        list.requestLayout();
        list.setTranslationY(0);
        paint();
    }

    private View card(Ps4Home.App app) {
        FrameLayout c = new FrameLayout(this);
        c.setBackground(null);
        FrameLayout art = Ps4Home.art(this, app, CARD_H);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(px(CARD_H - 2), px(CARD_H - 2));
        ap.leftMargin = ap.topMargin = px(1);
        c.addView(art, ap);
        TextView name = Ps4.text(this, 32, Ps4.light(this), Ps4.TEXT);
        name.setText(app.label);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(px(760), -2);
        np.leftMargin = px(CARD_H + 20);
        np.topMargin = px(16);
        c.addView(name, np);
        TextView sub = Ps4.text(this, 25, Ps4.light(this), Ps4.TEXT_DIM);
        sub.setText(app.leanback ? "Aplicación de TV" : "Aplicación");
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM);
        sp.leftMargin = px(CARD_H + 20);
        sp.bottomMargin = px(16);
        c.addView(sub, sp);
        TextView version = Ps4.text(this, 34, Ps4.light(this), Ps4.TEXT);
        String v = app.version(this);
        version.setText(v.length() > 14 ? v.substring(0, 14) : v);
        FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        vp.rightMargin = px(18);
        vp.bottomMargin = px(12);
        c.addView(version, vp);
        return c;
    }

    private void paint() {
        for (int i = 0; i < navRows.size(); i++) {
            FrameLayout r = (FrameLayout) navRows.get(i);
            TextView t = (TextView) r.getChildAt(1);
            boolean chosen = i == nav;
            t.setTextColor(chosen ? Ps4.TEXT : Ps4.TEXT_DIM);
            t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(chosen ? 31 : 27));
            r.setBackground(inNav && chosen ? Ps4.box(this, 3, Ps4.FOCUS, 0x1AFFFFFF) : null);
        }
        for (int i = 0; i < cards.size(); i++) {
            boolean on = !inNav && i == card;
            cards.get(i).setBackground(on ? Ps4.box(this, 3, Ps4.FOCUS, Ps4.pal(this).cardFocus) : Ps4.box(this, 1, Ps4.pal(this).cardLine, (Ps4.pal(this).card & 0xFFFFFF) | 0x66000000));
        }
        // The focused card sits in the middle of the list (or as high as the list's start allows) and the list
        // moves under it, so the cards above and below are always in view.
        float view = LIST_BOTTOM - LIST_TOP, content = 12 + cards.size() * PITCH;
        float target = card * PITCH - (view - CARD_H) / 2f;
        scrollRef = (int) Math.max(0, Math.min(target, Math.max(0, content - view)));
        list.animate().translationY(-px(scrollRef)).setDuration(140).setInterpolator(new DecelerateInterpolator()).start();
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
        if (code == KeyEvent.KEYCODE_BACK) snd.play(Ps4Sounds.BACK);
        else if (code == KeyEvent.KEYCODE_MENU) snd.play(Ps4Sounds.OK);
        else if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (e.getRepeatCount() == 0) snd.play(Ps4Sounds.OK);
        } else snd.play(Ps4Sounds.MOVE);
        List<Ps4Home.App> apps = lists.get(nav);
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (inNav) { if (nav > 0) showList(nav - 1); inNav = true; paint(); }
                else if (card > 0) { card--; paint(); }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (inNav) { if (nav < lists.size() - 1) showList(nav + 1); inNav = true; paint(); }
                else if (card < cards.size() - 1) { card++; paint(); }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!inNav) { inNav = true; paint(); }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (inNav && !cards.isEmpty()) { inNav = false; paint(); }
                break;
            case KeyEvent.KEYCODE_MENU:
                if (!inNav && card < apps.size()) Ps4.open(this, Ps4.appInfo(apps.get(card).pkg));
                break;
            case KeyEvent.KEYCODE_BACK:
                finish();
                break;
            default:
                if (e.getRepeatCount() > 0) break;
                if (inNav) { inNav = false; paint(); }
                else if (card < apps.size()) Ps4Home.launch(this, apps.get(card));
                break;
        }
        return true;
    }
}

package com.gathin.tvhome;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * Notification access for the PS4 layout: the notifications screen and its count in the status bar, and, through the
 * same grant, the media sessions behind the now-playing card (MediaSessionManager needs an enabled listener).
 * Granted with: cmd notification allow_listener com.gathin.tvhome/.Ps4Listener
 */
public final class Ps4Listener extends NotificationListenerService {
    private static Ps4Listener running;
    private static final List<Runnable> watchers = new ArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());

    static ComponentName component(Context ctx) {
        return new ComponentName(ctx, Ps4Listener.class);
    }

    static void watch(Runnable r) {
        if (!watchers.contains(r)) watchers.add(r);
    }

    static void unwatch(Runnable r) {
        watchers.remove(r);
    }

    private static void changed() {
        main.post(() -> {
            for (Runnable r : new ArrayList<>(watchers)) r.run();
        });
    }

    /** The notifications worth listing: not ongoing (media, downloads), not group summaries, with some text. */
    static List<StatusBarNotification> list() {
        List<StatusBarNotification> out = new ArrayList<>();
        Ps4Listener l = running;
        if (l == null) return out;
        StatusBarNotification[] all;
        try {
            all = l.getActiveNotifications();
        } catch (RuntimeException e) {
            return out;
        }
        if (all == null) return out;
        for (StatusBarNotification sbn : all) {
            Notification n = sbn.getNotification();
            if (sbn.isOngoing() || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) continue;
            if (title(sbn).isEmpty() && text(sbn).isEmpty()) continue;
            out.add(sbn);
        }
        out.sort((a, b) -> Long.compare(b.getPostTime(), a.getPostTime()));
        return out;
    }

    static boolean connected() {
        return running != null;
    }

    static void dismiss(StatusBarNotification sbn) {
        Ps4Listener l = running;
        if (l != null && sbn.isClearable()) l.cancelNotification(sbn.getKey());
    }

    static String title(StatusBarNotification sbn) {
        CharSequence t = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
        return t == null ? "" : t.toString();
    }

    static String text(StatusBarNotification sbn) {
        CharSequence t = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (t == null) t = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        return t == null ? "" : t.toString().replace('\n', ' ');
    }

    @Override
    public void onListenerConnected() {
        running = this;
        changed();
    }

    @Override
    public void onListenerDisconnected() {
        if (running == this) running = null;
        changed();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        changed();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        changed();
    }
}

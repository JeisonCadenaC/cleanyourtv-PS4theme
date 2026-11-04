package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shared pieces of the PS4-style layout ("ps4_layout": true in a theme's config.json): sizes measured on 1920x1080
 * reference screenshots and scaled to the screen, the palette, fonts, borders, line icons and the default backdrop.
 */
final class Ps4 {
    private Ps4() {}

    // Palette, sampled from the reference screenshots.
    static final int BLUE_TOP = 0xFF2C6DB9, BLUE_BOTTOM = 0xFF0A1C69, TILE_BLUE = 0xFF0C2E8F;
    static final int PANEL = 0xF20A0D33, LINE = 0x80A9B3D6, FOCUS = 0xFFDDE2EA, TEXT = 0xFFFFFFFF;
    static final int TEXT_DIM = 0xB3FFFFFF, CARD_FILL = 0x33102A7A, CARD_LINE = 0xFF7F8FC4, ONLINE = 0xFF3B9BFF;

    // ---- accent palette -----------------------------------------------------------------------

    /** The accent choices offered in the Quick Menu's Theme entry: a label and a value ("auto" or #RRGGBB). */
    static final String[][] ACCENTS = {
            {"Automático (del fondo)", "auto"},
            {"Azul PS4", "#1F5FC4"},
            {"Rojo Spider-Man", "#D0141E"},
            {"Morado", "#7B3FD1"},
            {"Verde", "#1E9E5A"},
            {"Naranja", "#E8701A"},
            {"Grafito", "#5A6270"},
    };

    /** Every colour the PS4 screens use, derived from one hue and saturation (HSL), like a Material tonal palette. */
    static final class Palette {
        final float hue, sat;
        final int strip, panel, card, cardLine, cardFocus, tile, top, bottom, glow, surface, primary, onPrimary,
                online, avatarTop, avatarBottom, track;

        Palette(float hue, float sat) {
            this.hue = hue;
            this.sat = sat;
            float s = Math.min(sat, 0.85f);
            strip = 0xF2000000 | hsl(hue, s * 0.85f, 0.40f);
            panel = 0xF2000000 | hsl(hue, Math.min(s, 0.55f), 0.09f);
            card = 0xF0000000 | hsl(hue, Math.min(s, 0.6f), 0.15f);
            cardLine = 0xFF000000 | hsl(hue, s * 0.45f, 0.62f);
            cardFocus = 0x55000000 | hsl(hue, s * 0.8f, 0.35f);
            tile = 0xFF000000 | hsl(hue, s * 0.85f, 0.30f);
            top = 0xFF000000 | hsl(hue, s * 0.75f, 0.38f);
            bottom = 0xFF000000 | hsl(hue, s * 0.85f, 0.11f);
            glow = 0x55000000 | hsl(hue, s * 0.7f, 0.58f);
            surface = 0xE0000000 | hsl(hue, Math.min(s, 0.35f), 0.12f);
            primary = 0xFF000000 | hsl(hue, Math.max(0.5f, s * 0.9f), 0.78f);
            onPrimary = 0xFF000000 | hsl(hue, Math.min(s, 0.7f), 0.16f);
            online = 0xFF000000 | hsl(hue, Math.max(0.6f, s), 0.62f);
            avatarTop = 0xFF000000 | hsl(hue, s * 0.8f, 0.58f);
            avatarBottom = 0xFF000000 | hsl(hue, s * 0.85f, 0.32f);
            track = 0x40FFFFFF;
        }
    }

    private static Palette palette;
    private static String paletteKey;
    /** Bumped whenever the accent changes, so screens built with the old colours rebuild. */
    static int paletteVersion;

    private static android.content.SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences("ps4", Context.MODE_PRIVATE);
    }

    /** The owner's accent choice from the Quick Menu, else the theme's "ps4_accent", else automatic. */
    static String accent(Context ctx) {
        String chosen = prefs(ctx).getString("accent", "");
        if (!chosen.isEmpty()) return chosen;
        String t = theme(ctx).optString("ps4_accent", "auto").trim();
        return t.isEmpty() ? "auto" : t;
    }

    static List<String> savedList(Context ctx, String key) {
        String v = prefs(ctx).getString(key, "");
        List<String> out = new ArrayList<>();
        for (String part : v.split(",")) if (!part.trim().isEmpty()) out.add(part.trim());
        return out;
    }

    static void saveList(Context ctx, String key, List<String> values) {
        prefs(ctx).edit().putString(key, android.text.TextUtils.join(",", values)).apply();
    }

    static void setAccent(Context ctx, String value) {
        prefs(ctx).edit().putString("accent", value).apply();
        palette = null;
        paletteVersion++;
    }

    static synchronized Palette pal(Context ctx) {
        String choice = accent(ctx);
        File wall = null;
        if ("auto".equals(choice)) {
            Config cfg = Config.load(ctx);
            wall = cfg.wallpaper != null && cfg.wallpaper.isFile() ? cfg.wallpaper
                    : cfg.wallpapers != null && !cfg.wallpapers.isEmpty() ? cfg.wallpapers.get(0) : null;
        }
        String key = choice + (wall == null ? "" : "|" + wall.getPath() + wall.lastModified());
        if (palette != null && key.equals(paletteKey)) return palette;
        float[] hs = null;
        if (!"auto".equals(choice)) {
            try {
                float[] hsv = new float[3];
                android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(choice), hsv);
                hs = new float[]{hsv[0], hsv[1]};
            } catch (IllegalArgumentException ignored) {
            }
        } else if (wall != null) {
            hs = dominantHue(wall);
        }
        if (hs == null) hs = new float[]{218f, 0.75f};   // the PS4's own blue
        palette = new Palette(hs[0], hs[1]);
        paletteKey = key;
        return palette;
    }

    /** The wallpaper's dominant hue and saturation: a hue histogram weighted by how vivid each pixel is. */
    private static float[] dominantHue(File file) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 16;
            Bitmap b = BitmapFactory.decodeFile(file.getPath(), o);
            if (b == null) return null;
            float[] weight = new float[36], sumSat = new float[36];
            double[] sx = new double[36], sy = new double[36];
            float[] hsv = new float[3];
            float total = 0;
            for (int y = 0; y < b.getHeight(); y += 2) {
                for (int x = 0; x < b.getWidth(); x += 2) {
                    android.graphics.Color.colorToHSV(b.getPixel(x, y), hsv);
                    if (hsv[1] < 0.25f || hsv[2] < 0.2f) continue;
                    float w = hsv[1] * hsv[2];
                    int bin = Math.min(35, (int) (hsv[0] / 10f));
                    weight[bin] += w;
                    sumSat[bin] += hsv[1] * w;
                    sx[bin] += Math.cos(Math.toRadians(hsv[0])) * w;
                    sy[bin] += Math.sin(Math.toRadians(hsv[0])) * w;
                    total += w;
                }
            }
            b.recycle();
            if (total <= 0) return new float[]{220f, 0.08f};   // a grey picture: a quiet graphite
            int best = 0;
            for (int i = 1; i < 36; i++) if (weight[i] > weight[best]) best = i;
            float hue = (float) Math.toDegrees(Math.atan2(sy[best], sx[best]));
            if (hue < 0) hue += 360;
            return new float[]{hue, sumSat[best] / weight[best]};
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    static int hsl(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float hp = (((h % 360) + 360) % 360) / 60f;
        float x = c * (1 - Math.abs(hp % 2 - 1));
        float r = 0, g = 0, b = 0;
        if (hp < 1) { r = c; g = x; } else if (hp < 2) { r = x; g = c; } else if (hp < 3) { g = c; b = x; }
        else if (hp < 4) { g = x; b = c; } else if (hp < 5) { r = x; b = c; } else { r = c; b = x; }
        float m = l - c / 2;
        return Math.round((r + m) * 255) << 16 | Math.round((g + m) * 255) << 8 | Math.round((b + m) * 255);
    }

    /** The list screens' background: the wallpaper under an accent-tinted scrim, or the PS4 gradient. */
    static Drawable screenBackground(Context ctx) {
        Palette pl = pal(ctx);
        Bitmap wall = wallpaper(ctx);
        if (wall == null) return new Backdrop(pl, true, false);
        android.graphics.drawable.BitmapDrawable bd = new android.graphics.drawable.BitmapDrawable(ctx.getResources(), wall);
        return new android.graphics.drawable.LayerDrawable(new Drawable[]{bd, new Backdrop(pl, true, true)});
    }

    private static long seenStamp = -1;
    private static JSONObject theme = new JSONObject();

    /** The theme's config.json, re-read only when the file changes. */
    private static synchronized JSONObject theme(Context ctx) {
        File dir = Config.dir(ctx);
        File file = dir == null ? null : new File(dir, "config.json");
        long stamp = file != null && file.isFile() ? file.lastModified() ^ file.length() : 0;
        if (stamp != seenStamp) {
            seenStamp = stamp;
            theme = new JSONObject();
            if (stamp != 0) {
                try (InputStream in = new FileInputStream(file)) {
                    byte[] buf = new byte[(int) Math.min(file.length(), 1 << 20)];
                    int n = 0, r;
                    while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
                    theme = new JSONObject(new String(buf, 0, n, StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                }
            }
        }
        return theme;
    }

    static boolean enabled(Context ctx) {
        return theme(ctx).optBoolean("ps4_layout", false);
    }

    static int option(Context ctx, String key, int fallback) {
        return theme(ctx).optInt(key, fallback);
    }

    static boolean flag(Context ctx, String key, boolean fallback) {
        return theme(ctx).optBoolean(key, fallback);
    }

    /** A file the theme pushed (avatar.jpg, sounds/move.ogg), trying each extension; null when none is there. */
    static File themeFile(Context ctx, String base, String... extensions) {
        File dir = Config.dir(ctx);
        if (dir == null) return null;
        for (String ext : extensions) {
            File f = new File(dir, base + "." + ext);
            if (f.isFile()) return f;
        }
        return null;
    }

    static String userName(Context ctx) {
        String name = theme(ctx).optString("ps4_user_name", "").trim();
        if (name.isEmpty()) name = ctx.getString(R.string.greeting_name).trim();
        return name.isEmpty() ? "Usuario" : name;
    }

    /** Pixels for a length measured on the 1920-wide reference. */
    static int px(Context ctx, float ref) {
        return Math.round(ref * ctx.getResources().getDisplayMetrics().widthPixels / 1920f);
    }

    static Typeface light(Context ctx) {
        return UiFont.get(ctx, R.string.font_light);
    }

    static Typeface thin(Context ctx) {
        return UiFont.get(ctx, R.string.font_extralight);
    }

    static TextView text(Context ctx, float sizeRef, Typeface face, int color) {
        TextView t = new TextView(ctx);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(ctx, sizeRef));
        t.setTypeface(face);
        t.setTextColor(color);
        t.setSingleLine(true);
        t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return t;
    }

    static GradientDrawable box(Context ctx, float strokeRef, int stroke, int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (strokeRef > 0) g.setStroke(Math.max(1, px(ctx, strokeRef)), stroke);
        return g;
    }

    static GradientDrawable circle(int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        return g;
    }

    /** The avatar: avatar.jpg/png from the theme cropped round, else the user's initial on a blue badge. */
    static android.view.View avatar(Context ctx, float sizeRef) {
        File photo = themeFile(ctx, "avatar", "jpg", "jpeg", "png", "webp");
        if (photo != null) {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(photo.getPath(), o);
                int target = Math.max(1, px(ctx, sizeRef) * 2);
                o.inSampleSize = 1;
                while (Math.min(o.outWidth, o.outHeight) / (o.inSampleSize * 2) >= target) o.inSampleSize *= 2;
                o.inJustDecodeBounds = false;
                Bitmap b = BitmapFactory.decodeFile(photo.getPath(), o);
                if (b != null) {
                    android.widget.ImageView iv = new android.widget.ImageView(ctx);
                    iv.setImageDrawable(new RoundPhoto(b, Math.max(1, px(ctx, 2))));
                    return iv;
                }
            } catch (Exception | OutOfMemoryError ignored) {
            }
        }
        return initial(ctx, sizeRef);
    }

    /** A photo drawn as a circle (centre-cropped) with a thin light ring, like the PS4's avatar. */
    static final class RoundPhoto extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap bitmap;

        RoundPhoto(Bitmap bitmap, int ringWidth) {
            this.bitmap = bitmap;
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(ringWidth);
            ring.setColor(0xCCFFFFFF);
        }

        @Override
        public void draw(Canvas c) {
            float w = getBounds().width(), h = getBounds().height(), r = Math.min(w, h) / 2f;
            float scale = Math.max(w / bitmap.getWidth(), h / bitmap.getHeight());
            android.graphics.BitmapShader shader = new android.graphics.BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setScale(scale, scale);
            m.postTranslate(getBounds().left + (w - bitmap.getWidth() * scale) / 2, getBounds().top + (h - bitmap.getHeight() * scale) / 2);
            shader.setLocalMatrix(m);
            paint.setShader(shader);
            c.drawCircle(getBounds().exactCenterX(), getBounds().exactCenterY(), r, paint);
            c.drawCircle(getBounds().exactCenterX(), getBounds().exactCenterY(), r - ring.getStrokeWidth() / 2, ring);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
        @SuppressWarnings("deprecation") @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private static TextView initial(Context ctx, float sizeRef) {
        TextView t = text(ctx, sizeRef * 0.55f, light(ctx), TEXT);
        String name = userName(ctx);
        t.setText(name.substring(0, 1).toUpperCase());
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{pal(ctx).avatarTop, pal(ctx).avatarBottom});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(Math.max(1, px(ctx, 2)), 0xCCFFFFFF);
        t.setBackground(g);
        return t;
    }

    static void open(Activity a, Intent intent) {
        try {
            a.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(a, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    static Intent appInfo(String pkg) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
    }

    /** Network name for the status bar; the connection type when the name is hidden. */
    static String networkName(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm != null ? cm.getActiveNetwork() : null;
            NetworkCapabilities caps = n != null ? cm.getNetworkCapabilities(n) : null;
            if (caps == null) return "Sin conexión";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                @SuppressWarnings("deprecation") WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
                String ssid = info != null ? info.getSSID() : null;
                if (ssid != null) {
                    ssid = ssid.replace("\"", "").trim();
                    if (!ssid.isEmpty() && !ssid.contains("unknown") && !ssid.equals("0x")) return ssid;
                }
                return "Wi-Fi";
            }
            return "Conectado";
        } catch (Exception e) {
            return "";
        }
    }

    /** What the network looks like now: type, name, Wi-Fi bars (0-3), signal %, address and internet. */
    static final class Net {
        boolean wifi, ethernet, online, validated;
        String name = "", address = "";
        int bars, percent;
    }

    @SuppressWarnings("deprecation")
    static Net net(Context ctx) {
        Net n = new Net();
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network active = cm.getActiveNetwork();
            NetworkCapabilities caps = active != null ? cm.getNetworkCapabilities(active) : null;
            if (caps == null) return n;
            n.online = true;
            n.validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            n.ethernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
            n.wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            android.net.LinkProperties lp = cm.getLinkProperties(active);
            if (lp != null) {
                for (android.net.LinkAddress la : lp.getLinkAddresses()) {
                    if (la.getAddress() instanceof java.net.Inet4Address) n.address = la.getAddress().getHostAddress();
                }
            }
            if (n.wifi) {
                WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
                if (info != null) {
                    n.percent = Math.max(0, Math.min(100, 2 * (info.getRssi() + 100)));
                    n.bars = WifiManager.calculateSignalLevel(info.getRssi(), 4);
                }
            }
            n.name = networkName(ctx);
        } catch (Exception ignored) {
        }
        return n;
    }

    /** Paired Bluetooth devices, connected ones first: {name, "1" if connected}. Needs BLUETOOTH_CONNECT. */
    static List<String[]> bluetooth(Context ctx) {
        List<String[]> out = new ArrayList<>();
        try {
            android.bluetooth.BluetoothManager bm =
                    (android.bluetooth.BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
            android.bluetooth.BluetoothAdapter ad = bm != null ? bm.getAdapter() : null;
            if (ad == null || !ad.isEnabled()) return out;
            for (android.bluetooth.BluetoothDevice d : ad.getBondedDevices()) {
                boolean connected = false;
                try {
                    connected = (Boolean) d.getClass().getMethod("isConnected").invoke(d);
                } catch (Exception ignored) {
                }
                String name = d.getAlias() != null ? d.getAlias() : d.getName();
                out.add(new String[]{name == null ? d.getAddress() : name, connected ? "1" : ""});
            }
        } catch (SecurityException | NoSuchMethodError e) {
            return out;
        }
        out.sort((x, y) -> y[1].compareTo(x[1]));
        return out;
    }

    /** The theme's wallpaper, decoded near screen size, or null for the default PS4 backdrop. */
    static Bitmap wallpaper(Context ctx) {
        try {
            Config cfg = Config.load(ctx);
            if (!cfg.wallpaperEnabled) return null;
            File f = cfg.wallpaper;
            if ((f == null || !f.isFile()) && cfg.wallpapers != null && !cfg.wallpapers.isEmpty()) f = cfg.wallpapers.get(0);
            if (f == null || !f.isFile()) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            int target = ctx.getResources().getDisplayMetrics().widthPixels;
            o.inSampleSize = 1;
            while (o.outWidth / (o.inSampleSize * 2) >= target) o.inSampleSize *= 2;
            o.inJustDecodeBounds = false;
            o.inPreferredConfig = cfg.wallpaperTrueColor ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(f.getPath(), o);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    /** The app's launcher icon at the highest density it ships, so it stays sharp at 160 px and more. */
    static Drawable sharpIcon(Context ctx, android.content.pm.ResolveInfo ri) {
        android.content.pm.PackageManager pm = ctx.getPackageManager();
        int id = ri.activityInfo.getIconResource();
        if (id != 0) {
            try {
                android.content.res.Resources r = pm.getResourcesForApplication(ri.activityInfo.applicationInfo);
                Drawable d = r.getDrawableForDensity(id, android.util.DisplayMetrics.DENSITY_XXXHIGH, null);
                if (d != null) return d;
            } catch (Exception ignored) {
            }
        }
        return ri.loadIcon(pm);
    }

    /**
     * The tile colour for an icon: its most vivid hue, kept deep so the icon stands out on it. A grey or white icon
     * gets the accent's tile colour instead.
     */
    static int iconColor(Drawable icon, int fallback) {
        if (icon == null) return fallback;
        try {
            Bitmap b = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            icon.setBounds(0, 0, 48, 48);
            icon.draw(c);
            float[] weight = new float[36], sat = new float[36];
            double[] sx = new double[36], sy = new double[36];
            float[] hsv = new float[3];
            float total = 0;
            for (int y = 0; y < 48; y++) {
                for (int x = 0; x < 48; x++) {
                    int px = b.getPixel(x, y);
                    if ((px >>> 24) < 200) continue;
                    android.graphics.Color.colorToHSV(px, hsv);
                    if (hsv[1] < 0.3f || hsv[2] < 0.25f) continue;
                    float w = hsv[1] * hsv[2];
                    int bin = Math.min(35, (int) (hsv[0] / 10f));
                    weight[bin] += w;
                    sat[bin] += hsv[1] * w;
                    sx[bin] += Math.cos(Math.toRadians(hsv[0])) * w;
                    sy[bin] += Math.sin(Math.toRadians(hsv[0])) * w;
                    total += w;
                }
            }
            b.recycle();
            if (total < 20) return fallback;
            int best = 0;
            for (int i = 1; i < 36; i++) if (weight[i] > weight[best]) best = i;
            float hue = (float) Math.toDegrees(Math.atan2(sy[best], sx[best]));
            if (hue < 0) hue += 360;
            float s = Math.min(0.75f, sat[best] / weight[best]);
            return 0xFF000000 | hsl(hue, s, 0.26f);
        } catch (Exception e) {
            return fallback;
        }
    }

    static int lighten(int color, float amount) {
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        r += (int) ((255 - r) * amount);
        g += (int) ((255 - g) * amount);
        b += (int) ((255 - b) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Average colour of a banner's left and right edges, to fill a square tile around a 16:9 banner. */
    static int edgeColor(Drawable banner, int fallback) {
        if (banner == null) return fallback;
        try {
            Bitmap b = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            banner.setBounds(0, 0, 32, 18);
            banner.draw(c);
            long r = 0, g = 0, bl = 0, n = 0;
            for (int y = 0; y < 18; y++) {
                for (int x : new int[]{0, 1, 30, 31}) {
                    int p = b.getPixel(x, y);
                    if ((p >>> 24) < 128) continue;
                    r += (p >> 16) & 255; g += (p >> 8) & 255; bl += p & 255; n++;
                }
            }
            b.recycle();
            if (n == 0) return fallback;
            return 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (bl / n);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * A progress line drawn as a travelling wave up to the position and a flat track after it, like the media player
     * of Android 13 and later. The wave moves only while playing and on screen; paused, it settles flat.
     */
    static final class Wave extends android.view.View {
        private final Paint played = new Paint(Paint.ANTI_ALIAS_FLAG), rest = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private float progress, phase, amp, wavelength;
        private boolean moving;
        private long last;

        Wave(Context ctx, int color, int track) {
            super(ctx);
            float w = px(ctx, 4.5f);
            played.setStyle(Paint.Style.STROKE);
            played.setStrokeWidth(w);
            played.setStrokeCap(Paint.Cap.ROUND);
            played.setColor(color);
            rest.setStyle(Paint.Style.STROKE);
            rest.setStrokeWidth(w);
            rest.setStrokeCap(Paint.Cap.ROUND);
            rest.setColor(track);
            wavelength = px(ctx, 30);
        }

        void setProgress(float p) {
            progress = Math.max(0f, Math.min(1f, p));
            invalidate();
        }

        /** Home's animation timer calls this each frame; true while the wave still has to move or settle. */
        boolean frame() {
            boolean busy = moving || Math.abs(amp) > 0.2f;
            if (busy) invalidate();
            else last = 0;
            return busy;
        }

        void setMoving(boolean on) {
            if (moving == on) return;
            moving = on;
            last = 0;
            if (onChange != null) onChange.run();
        }

        /** Told when the wave starts moving, so the timer can start. */
        Runnable onChange;

        @Override
        protected void onDraw(Canvas c) {
            float h = getHeight(), mid = h / 2f, pad = played.getStrokeWidth();
            float end = pad + (getWidth() - 2 * pad) * progress;
            long now = android.os.SystemClock.uptimeMillis();
            float dt = last == 0 ? 0 : Math.min(64, now - last) / 1000f;
            last = now;
            // The amplitude eases in when playing starts and out when it pauses; the wave travels left to right.
            float target = moving ? h * 0.22f : 0f;
            amp += (target - amp) * Math.min(1f, dt * 6f);
            if (moving) phase += dt * 2.2f * (float) Math.PI;
            if (end > pad) {
                path.reset();
                path.moveTo(pad, mid);
                for (float x = pad; x <= end; x += 2) {
                    path.lineTo(x, mid + amp * (float) Math.sin(2 * Math.PI * x / wavelength - phase));
                }
                c.drawPath(path, played);
            }
            if (end + pad * 2 < getWidth() - pad) c.drawLine(end + pad * 2, mid, getWidth() - pad, mid, rest);
            // Redraws come from the frame timer only: invalidating from here started a new chain on every outside
            // redraw (the progress each second), and the chains added up to a redraw on every display frame.
        }
    }

    /** The PS4 default backdrop: a blue gradient, a soft light at the top left and still particles. */
    static final class Backdrop extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean sweeps, scrim;
        private final Palette pl;

        Backdrop(Palette pl, boolean sweeps, boolean scrim) {
            this.pl = pl;
            this.sweeps = sweeps;
            this.scrim = scrim;
        }

        @Override
        public void draw(Canvas c) {
            float w = getBounds().width(), h = getBounds().height();
            int top = scrim ? (pl.top & 0x00FFFFFF) | 0xC8000000 : pl.top;
            int bottom = scrim ? (pl.bottom & 0x00FFFFFF) | 0xEB000000 : pl.bottom;
            paint.setShader(new LinearGradient(0, 0, w * 0.35f, h, top, bottom, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(new RadialGradient(w * 0.15f, h * 0.05f, w * 0.6f, pl.glow, 0x00000000, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            if (sweeps) {   // the long translucent arcs behind list screens
                paint.setStyle(Paint.Style.STROKE);
                for (int i = 0; i < 3; i++) {
                    paint.setStrokeWidth(w * (0.05f - i * 0.012f));
                    paint.setColor(0x14FFFFFF + (i * 0x06000000));
                    Path p = new Path();
                    p.moveTo(w * (0.30f + i * 0.04f), h * 1.05f);
                    p.cubicTo(w * 0.45f, h * 0.55f, w * 0.70f, h * 0.15f, w * 1.05f, h * (0.02f + i * 0.04f));
                    c.drawPath(p, paint);
                }
                paint.setStyle(Paint.Style.FILL);
                return;
            }
            Random rnd = new Random(4);
            for (int i = 0; i < 90; i++) {
                float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h;
                // The particles gather in a loose band from the lower middle to the right, as on the PS4.
                if (y < h * 0.4f && rnd.nextFloat() < 0.7f) y += h * 0.4f;
                float r = (2 + rnd.nextFloat() * 7) * w / 1920f;
                paint.setColor(((int) (40 + rnd.nextFloat() * 120) << 24) | 0xD8E6FF);
                c.drawCircle(x, y, r, paint);
            }
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @SuppressWarnings("deprecation") @Override public int getOpacity() { return scrim ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE; }
    }

    /** Line icons drawn in code, white, in the PS4's thick rounded style. */
    static final class Icon extends Drawable {
        static final int POWER = 0, SOUND = 1, INFO = 2, CUSTOMIZE = 3, LIBRARY = 4, ARROW_DOWN = 5, PLAY = 6,
                NETWORK = 7, PICTURE = 8, GEAR = 9, OK = 10, BACK = 11, MENU = 12, CHEVRON = 13,
                DISC = 14, PROFILE = 15, PREV = 16, NEXT = 17, PAUSE = 18, BELL = 19, CLEAR = 20, PALETTE = 21, STAR = 22, MOVE = 23, TRASH = 24, ETHERNET = 25, BLUETOOTH = 26,
                VOLUME_UP = 27, VOLUME_DOWN = 28, MUTE = 29;
        /** Wi-Fi bars lit, 0 to 3 (NETWORK only); the unlit ones are drawn faint. */
        int level = 3;
        private final int kind;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final int color;

        Icon(int kind) {
            this(kind, TEXT);
        }

        Icon(int kind, int color) {
            this.kind = kind;
            this.color = color;
            p.setColor(color);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        public void draw(Canvas c) {
            float s = Math.min(getBounds().width(), getBounds().height());
            c.save();
            c.translate(getBounds().exactCenterX() - s / 2, getBounds().exactCenterY() - s / 2);
            float u = s / 24f;
            p.setStrokeWidth(2.2f * u);
            p.setStyle(Paint.Style.STROKE);
            switch (kind) {
                case POWER:
                    c.drawArc(new RectF(4 * u, 5 * u, 20 * u, 21 * u), -60, 300, false, p);
                    c.drawLine(12 * u, 2.5f * u, 12 * u, 11 * u, p);
                    break;
                case SOUND: {
                    Path sp = new Path();
                    sp.moveTo(3 * u, 9 * u); sp.lineTo(7 * u, 9 * u); sp.lineTo(12 * u, 4.5f * u);
                    sp.lineTo(12 * u, 19.5f * u); sp.lineTo(7 * u, 15 * u); sp.lineTo(3 * u, 15 * u); sp.close();
                    p.setStyle(Paint.Style.FILL);
                    c.drawPath(sp, p);
                    p.setStyle(Paint.Style.STROKE);
                    c.drawArc(new RectF(10 * u, 7.5f * u, 17 * u, 16.5f * u), -50, 100, false, p);
                    c.drawArc(new RectF(10 * u, 4 * u, 21 * u, 20 * u), -50, 100, false, p);
                    break;
                }
                case INFO:
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(12 * u, 11 * u, 9.5f * u, p);
                    Path tail = new Path();
                    tail.moveTo(14 * u, 19 * u); tail.lineTo(21 * u, 22 * u); tail.lineTo(18 * u, 16 * u); tail.close();
                    c.drawPath(tail, p);
                    p.setColor(TILE_BLUE);
                    c.drawCircle(12 * u, 6.8f * u, 1.5f * u, p);
                    c.drawRoundRect(new RectF(10.7f * u, 9.3f * u, 13.3f * u, 16.5f * u), u, u, p);
                    p.setColor(color);
                    break;
                case CUSTOMIZE:
                    c.drawLine(3 * u, 6 * u, 15 * u, 6 * u, p);
                    c.drawLine(3 * u, 11 * u, 12 * u, 11 * u, p);
                    c.drawLine(3 * u, 16 * u, 9 * u, 16 * u, p);
                    c.drawLine(13 * u, 21 * u, 21 * u, 13 * u, p);
                    c.drawLine(12 * u, 22 * u, 13 * u, 21 * u, p);
                    break;
                case LIBRARY:
                    p.setStyle(Paint.Style.FILL);
                    for (int r = 0; r < 3; r++) for (int col = 0; col < 3; col++)
                        c.drawRoundRect(new RectF((3 + col * 6.5f) * u, (3 + r * 6.5f) * u, (8 + col * 6.5f) * u, (8 + r * 6.5f) * u), u, u, p);
                    break;
                case ARROW_DOWN: {
                    p.setStyle(Paint.Style.FILL);
                    c.drawRect(10.5f * u, 3 * u, 13.5f * u, 13 * u, p);
                    Path a = new Path();
                    a.moveTo(6 * u, 12 * u); a.lineTo(18 * u, 12 * u); a.lineTo(12 * u, 21 * u); a.close();
                    c.drawPath(a, p);
                    break;
                }
                case PLAY: {
                    p.setStyle(Paint.Style.FILL);
                    Path a = new Path();
                    a.moveTo(7 * u, 4 * u); a.lineTo(20 * u, 12 * u); a.lineTo(7 * u, 20 * u); a.close();
                    c.drawPath(a, p);
                    break;
                }
                case NETWORK: {
                    // Three arcs and a dot, all centred on the dot, as Android draws Wi-Fi.
                    float cx = 12 * u, cy = 19.5f * u;
                    for (int i = 0; i < 3; i++) {
                        float r = (5.5f + i * 5f) * u;
                        p.setAlpha(i < level ? 255 : 70);
                        c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -135, 90, false, p);
                    }
                    p.setAlpha(255);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy, 2 * u, p);
                    break;
                }
                case ETHERNET:
                    c.drawRoundRect(new RectF(5 * u, 4 * u, 19 * u, 15 * u), 1.5f * u, 1.5f * u, p);
                    c.drawLine(9 * u, 4 * u, 9 * u, 7.5f * u, p);
                    c.drawLine(12 * u, 4 * u, 12 * u, 7.5f * u, p);
                    c.drawLine(15 * u, 4 * u, 15 * u, 7.5f * u, p);
                    c.drawLine(12 * u, 15 * u, 12 * u, 21 * u, p);
                    break;
                case BLUETOOTH: {
                    Path b = new Path();
                    b.moveTo(6.5f * u, 7.5f * u); b.lineTo(17 * u, 16.5f * u); b.lineTo(12 * u, 21 * u);
                    b.lineTo(12 * u, 3 * u); b.lineTo(17 * u, 7.5f * u); b.lineTo(6.5f * u, 16.5f * u);
                    c.drawPath(b, p);
                    break;
                }
                case VOLUME_UP:
                case VOLUME_DOWN:
                case MUTE: {
                    Path sp = new Path();
                    sp.moveTo(3 * u, 9 * u); sp.lineTo(7 * u, 9 * u); sp.lineTo(12 * u, 4.5f * u);
                    sp.lineTo(12 * u, 19.5f * u); sp.lineTo(7 * u, 15 * u); sp.lineTo(3 * u, 15 * u); sp.close();
                    p.setStyle(Paint.Style.FILL);
                    c.drawPath(sp, p);
                    p.setStyle(Paint.Style.STROKE);
                    if (kind == MUTE) {
                        c.drawLine(15 * u, 9 * u, 21 * u, 15 * u, p);
                        c.drawLine(21 * u, 9 * u, 15 * u, 15 * u, p);
                    } else {
                        c.drawLine(15 * u, 12 * u, 21 * u, 12 * u, p);
                        if (kind == VOLUME_UP) c.drawLine(18 * u, 9 * u, 18 * u, 15 * u, p);
                    }
                    break;
                }
                case PICTURE:
                    c.drawRoundRect(new RectF(2.5f * u, 4.5f * u, 21.5f * u, 19.5f * u), u, u, p);
                    Path m = new Path();
                    m.moveTo(4 * u, 18 * u); m.lineTo(10 * u, 11 * u); m.lineTo(14 * u, 15 * u);
                    m.lineTo(16.5f * u, 13 * u); m.lineTo(20 * u, 18 * u);
                    c.drawPath(m, p);
                    break;
                case GEAR: {
                    // A cog: eight square teeth on a ring, with a hole.
                    p.setStyle(Paint.Style.FILL);
                    Path g = new Path();
                    g.moveTo(12 * u + (float) Math.cos(-0.38) * 7.6f * u, 12 * u + (float) Math.sin(-0.38) * 7.6f * u);
                    for (int i = 0; i < 8; i++) {
                        double mid = Math.PI * i / 4;
                        for (double off : new double[]{-0.22, 0.22}) {
                            double a1 = mid + off;
                            float rOut = 10.5f * u, rIn = 7.6f * u;
                            if (off < 0) {
                                g.lineTo(12 * u + (float) Math.cos(mid - 0.38) * rIn, 12 * u + (float) Math.sin(mid - 0.38) * rIn);
                                g.lineTo(12 * u + (float) Math.cos(a1) * rOut, 12 * u + (float) Math.sin(a1) * rOut);
                            } else {
                                g.lineTo(12 * u + (float) Math.cos(a1) * rOut, 12 * u + (float) Math.sin(a1) * rOut);
                                g.lineTo(12 * u + (float) Math.cos(mid + 0.38) * rIn, 12 * u + (float) Math.sin(mid + 0.38) * rIn);
                            }
                        }
                    }
                    g.close();
                    g.addCircle(12 * u, 12 * u, 3.4f * u, Path.Direction.CCW);
                    g.setFillType(Path.FillType.EVEN_ODD);
                    c.drawPath(g, p);
                    break;
                }
                case OK:
                case BACK:
                case MENU:
                    p.setStrokeWidth(1.6f * u);
                    c.drawCircle(12 * u, 12 * u, 10.5f * u, p);
                    if (kind == OK) {
                        p.setStyle(Paint.Style.FILL);
                        c.drawCircle(12 * u, 12 * u, 4.2f * u, p);
                    } else if (kind == BACK) {
                        c.drawLine(8 * u, 12 * u, 16.5f * u, 12 * u, p);
                        c.drawLine(8 * u, 12 * u, 11.5f * u, 8.5f * u, p);
                        c.drawLine(8 * u, 12 * u, 11.5f * u, 15.5f * u, p);
                    } else {
                        c.drawLine(7.5f * u, 8.5f * u, 16.5f * u, 8.5f * u, p);
                        c.drawLine(7.5f * u, 12 * u, 16.5f * u, 12 * u, p);
                        c.drawLine(7.5f * u, 15.5f * u, 16.5f * u, 15.5f * u, p);
                    }
                    break;
                case DISC: {
                    float cx = 12 * u, cy = 12 * u, r = 10.5f * u;
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(0xFFD9DEE6);
                    c.drawCircle(cx, cy, r, p);
                    p.setShader(new android.graphics.SweepGradient(cx, cy, new int[]{
                            0x66FF6EC7, 0x6669D2FF, 0x66B6FF7A, 0x66FFE36E, 0x66FF6EC7}, null));
                    c.drawCircle(cx, cy, r, p);
                    p.setShader(null);
                    p.setColor(0xCCFFFFFF);   // two light streaks, opposite each other
                    c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -70, 26, true, p);
                    c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), 110, 26, true, p);
                    p.setColor(0xFF9AA3AE);
                    c.drawCircle(cx, cy, 3.8f * u, p);
                    p.setColor(0xFF2A2F36);
                    c.drawCircle(cx, cy, 1.6f * u, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(0.7f * u);
                    p.setColor(0x55000000);
                    c.drawCircle(cx, cy, r - 0.35f * u, p);
                    p.setColor(color);
                    break;
                }
                case PROFILE:
                    c.drawCircle(12 * u, 8 * u, 4.5f * u, p);
                    c.drawArc(new RectF(3.5f * u, 14 * u, 20.5f * u, 30 * u), 180, 180, false, p);
                    break;
                case PREV:
                case NEXT: {
                    c.save();
                    if (kind == PREV) c.scale(-1, 1, 12 * u, 12 * u);
                    p.setStyle(Paint.Style.FILL);
                    Path a = new Path();
                    a.moveTo(5 * u, 5 * u); a.lineTo(16 * u, 12 * u); a.lineTo(5 * u, 19 * u); a.close();
                    c.drawPath(a, p);
                    c.drawRect(16.5f * u, 5 * u, 19 * u, 19 * u, p);
                    c.restore();
                    break;
                }
                case PAUSE:
                    p.setStyle(Paint.Style.FILL);
                    c.drawRoundRect(new RectF(6 * u, 4.5f * u, 10 * u, 19.5f * u), u, u, p);
                    c.drawRoundRect(new RectF(14 * u, 4.5f * u, 18 * u, 19.5f * u), u, u, p);
                    break;
                case BELL:
                    c.drawArc(new RectF(5 * u, 3.5f * u, 19 * u, 17.5f * u), 180, 180, false, p);
                    c.drawLine(5 * u, 10.5f * u, 5 * u, 16 * u, p);
                    c.drawLine(19 * u, 10.5f * u, 19 * u, 16 * u, p);
                    c.drawLine(3 * u, 17 * u, 21 * u, 17 * u, p);
                    c.drawArc(new RectF(9.5f * u, 17.5f * u, 14.5f * u, 22 * u), 0, 180, false, p);
                    break;
                case CLEAR:
                    c.drawLine(6 * u, 6 * u, 18 * u, 18 * u, p);
                    c.drawLine(18 * u, 6 * u, 6 * u, 18 * u, p);
                    break;
                case STAR: {
                    p.setStyle(Paint.Style.FILL);
                    Path st = new Path();
                    for (int i = 0; i < 10; i++) {
                        double ang = Math.PI / 2 + i * Math.PI / 5;
                        float r = (i % 2 == 0 ? 10.5f : 4.4f) * u;
                        float x = 12 * u - (float) Math.cos(ang) * r, y = 12.8f * u - (float) Math.sin(ang) * r;
                        if (i == 0) st.moveTo(x, y); else st.lineTo(x, y);
                    }
                    st.close();
                    c.drawPath(st, p);
                    break;
                }
                case MOVE:
                    c.drawLine(3 * u, 12 * u, 21 * u, 12 * u, p);
                    c.drawLine(3 * u, 12 * u, 7.5f * u, 7.5f * u, p);
                    c.drawLine(3 * u, 12 * u, 7.5f * u, 16.5f * u, p);
                    c.drawLine(21 * u, 12 * u, 16.5f * u, 7.5f * u, p);
                    c.drawLine(21 * u, 12 * u, 16.5f * u, 16.5f * u, p);
                    break;
                case TRASH:
                    c.drawLine(4 * u, 6.5f * u, 20 * u, 6.5f * u, p);
                    c.drawLine(9.5f * u, 6.5f * u, 9.5f * u, 3.5f * u, p);
                    c.drawLine(9.5f * u, 3.5f * u, 14.5f * u, 3.5f * u, p);
                    c.drawLine(14.5f * u, 3.5f * u, 14.5f * u, 6.5f * u, p);
                    c.drawRoundRect(new RectF(6 * u, 6.5f * u, 18 * u, 21 * u), 1.5f * u, 1.5f * u, p);
                    c.drawLine(10 * u, 10.5f * u, 10 * u, 17 * u, p);
                    c.drawLine(14 * u, 10.5f * u, 14 * u, 17 * u, p);
                    break;
                case PALETTE:
                    c.drawArc(new RectF(2.5f * u, 2.5f * u, 21.5f * u, 21.5f * u), 60, 300, false, p);
                    c.drawLine(18.5f * u, 18.5f * u, 15 * u, 15 * u, p);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(8 * u, 9 * u, 1.8f * u, p);
                    c.drawCircle(12.5f * u, 6.8f * u, 1.8f * u, p);
                    c.drawCircle(16.8f * u, 9.5f * u, 1.8f * u, p);
                    c.drawCircle(7.5f * u, 14 * u, 1.8f * u, p);
                    break;
                case CHEVRON:
                    c.drawLine(5 * u, 9 * u, 12 * u, 16 * u, p);
                    c.drawLine(12 * u, 16 * u, 19 * u, 9 * u, p);
                    break;
                default:
                    break;
            }
            c.restore();
        }

        @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { p.setColorFilter(cf); }
        @SuppressWarnings("deprecation") @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}

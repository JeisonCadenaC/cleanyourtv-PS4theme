package com.gathin.tvhome;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Navigation sounds for the PS4 layout. A theme can bring its own (sounds/move, sounds/ok, sounds/back as ogg, mp3 or
 * wav); otherwise short tones in the same spirit are synthesised once into the cache: a soft high tick for a move, a
 * two-note chime to confirm and a falling pair for back. "ps4_sounds": false in the theme turns them off.
 */
final class Ps4Sounds {
    static final int MOVE = 0, OK = 1, BACK = 2;
    private static Ps4Sounds instance;

    private final SoundPool pool;
    private final int[] ids = new int[3];
    private final boolean on;

    private Ps4Sounds(Context ctx) {
        on = Ps4.flag(ctx, "ps4_sounds", true);
        pool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        if (!on) return;
        String[] names = {"move", "ok", "back"};
        for (int i = 0; i < 3; i++) {
            File f = Ps4.themeFile(ctx, "sounds/" + names[i], "ogg", "mp3", "wav");
            if (f == null) f = synth(ctx, i);
            if (f != null) ids[i] = pool.load(f.getPath(), 1);
        }
    }

    static synchronized Ps4Sounds get(Context ctx) {
        if (instance == null) instance = new Ps4Sounds(ctx.getApplicationContext());
        return instance;
    }

    /** Drops the loaded sounds, so a newly pushed theme's files are picked up. */
    static synchronized void reset() {
        if (instance != null) instance.pool.release();
        instance = null;
    }

    void play(int which) {
        if (!on || ids[which] == 0) return;
        float v = which == MOVE ? 0.35f : 0.5f;
        pool.play(ids[which], v, v, 1, 0, 1f);
    }

    // ---- synthesis -----------------------------------------------------------------------------

    private static final int RATE = 44100;

    private static File synth(Context ctx, int which) {
        File f = new File(ctx.getCacheDir(), "ps4_sound_" + which + "_v1.wav");
        if (f.isFile()) return f;
        float[] pcm;
        switch (which) {
            case MOVE:
                pcm = new float[(int) (RATE * 0.09)];
                tone(pcm, 0, 2350, 0.022f, 0.55f);
                tone(pcm, 0, 4700, 0.012f, 0.12f);
                tone(pcm, (int) (RATE * 0.035), 2350, 0.02f, 0.12f);   // a faint echo, as the PS4's tick has
                break;
            case OK:
                pcm = new float[(int) (RATE * 0.45)];
                tone(pcm, 0, 1318.5f, 0.12f, 0.45f);
                tone(pcm, 0, 2637f, 0.05f, 0.1f);
                tone(pcm, (int) (RATE * 0.07), 1975.5f, 0.16f, 0.45f);
                tone(pcm, (int) (RATE * 0.07), 3951f, 0.06f, 0.08f);
                break;
            default:
                pcm = new float[(int) (RATE * 0.35)];
                tone(pcm, 0, 1568f, 0.09f, 0.4f);
                tone(pcm, (int) (RATE * 0.06), 1046.5f, 0.13f, 0.4f);
                break;
        }
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(wav(pcm));
            return f;
        } catch (IOException e) {
            return null;
        }
    }

    /** Adds a sine with a 3 ms attack and an exponential decay of time constant tau (seconds). */
    private static void tone(float[] pcm, int start, float freq, float tau, float amp) {
        int attack = RATE * 3 / 1000;
        for (int i = start; i < pcm.length; i++) {
            float t = (i - start) / (float) RATE;
            float env = amp * (float) Math.exp(-t / tau) * Math.min(1f, (i - start) / (float) attack);
            if (env < 0.0005f && i - start > attack) break;
            pcm[i] += env * (float) Math.sin(2 * Math.PI * freq * t);
        }
    }

    private static byte[] wav(float[] pcm) {
        int data = pcm.length * 2;
        ByteBuffer b = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[]{'R', 'I', 'F', 'F'}).putInt(36 + data).put(new byte[]{'W', 'A', 'V', 'E'});
        b.put(new byte[]{'f', 'm', 't', ' '}).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(RATE).putInt(RATE * 2).putShort((short) 2).putShort((short) 16);
        b.put(new byte[]{'d', 'a', 't', 'a'}).putInt(data);
        for (float v : pcm) b.putShort((short) Math.round(Math.max(-1f, Math.min(1f, v)) * 32000));
        return b.array();
    }
}

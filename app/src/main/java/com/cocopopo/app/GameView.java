package com.cocopopo.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** Thin Android wrapper: drives the Game with frames and touches, persists data, saves photos. */
final class GameView extends View implements Game.Host {
    final Game game;
    private final SharedPreferences prefs;
    private final MainActivity activity;
    private long last;

    GameView(MainActivity a) {
        super(a);
        activity = a;
        prefs = a.getSharedPreferences("cocopopo", Context.MODE_PRIVATE);
        game = new Game(this);
        setKeepScreenOn(true);
        initSounds(a);
    }

    // ---- sounds, synthesised at start-up (no audio assets)
    private SoundPool pool;
    private final int[] sfx = new int[8];
    private final long[] lastPlay = new long[8];

    @SuppressWarnings("deprecation")
    private void initSounds(Context ctx) {
        try {
            pool = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);
            for (int i = 0; i < sfx.length; i++) {
                File f = new File(ctx.getCacheDir(), "sfx" + i + ".wav");
                writeWav(f, Synth.make(i));
                sfx[i] = pool.load(f.getAbsolutePath(), 1);
            }
        } catch (Exception e) {
            pool = null;
        }
    }

    private static void writeWav(File f, short[] pcm) throws java.io.IOException {
        int rate = Synth.RATE, bytes = pcm.length * 2;
        java.io.DataOutputStream o = new java.io.DataOutputStream(new java.io.BufferedOutputStream(new FileOutputStream(f)));
        o.writeBytes("RIFF"); o.writeInt(Integer.reverseBytes(36 + bytes)); o.writeBytes("WAVEfmt ");
        o.writeInt(Integer.reverseBytes(16)); o.writeShort(Short.reverseBytes((short) 1)); o.writeShort(Short.reverseBytes((short) 1));
        o.writeInt(Integer.reverseBytes(rate)); o.writeInt(Integer.reverseBytes(rate * 2));
        o.writeShort(Short.reverseBytes((short) 2)); o.writeShort(Short.reverseBytes((short) 16));
        o.writeBytes("data"); o.writeInt(Integer.reverseBytes(bytes));
        for (short v : pcm) o.writeShort(Short.reverseBytes(v));
        o.close();
    }

    @Override public void sound(int id) {
        if (pool == null || id < 0 || id >= sfx.length) return;
        long now = System.currentTimeMillis();
        if (now - lastPlay[id] < 60) return;
        lastPlay[id] = now;
        pool.play(sfx[id], .8f, .8f, 1, 0, 0.9f + (float) Math.random() * .2f);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        game.layout(w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0.016f : (now - last) / 1e9f;
        last = now;
        game.update(dt);
        game.draw(canvas);
        postInvalidateOnAnimation();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: game.touch(0, e.getX(), e.getY()); break;
            case MotionEvent.ACTION_MOVE: game.touch(1, e.getX(), e.getY()); break;
            case MotionEvent.ACTION_UP: game.touch(2, e.getX(), e.getY()); break;
            case MotionEvent.ACTION_CANCEL: game.touch(3, e.getX(), e.getY()); break;
            default: break;
        }
        return true;
    }

    // ---- Host
    @Override public String load(String key) { return prefs.getString(key, null); }

    @Override public void save(String key, String value) {
        SharedPreferences.Editor ed = prefs.edit();
        if (value == null) ed.remove(key); else ed.putString(key, value);
        ed.apply();
    }

    @Override public void haptic() { performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); }

    @Override public void quit() { activity.finish(); }

    @Override public void photo() {
        try {
            int w = getWidth(), h = getHeight();
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            game.drawPhoto(new Canvas(bmp));
            String name = "CocoPopo_" + System.currentTimeMillis() + ".png";
            OutputStream out;
            String where;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put("_display_name", name);
                v.put("mime_type", "image/png");
                v.put("relative_path", "Pictures/CocoPopo");
                Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                out = getContext().getContentResolver().openOutputStream(uri);
                where = "Pictures/CocoPopo";
            } else {
                File dir = getContext().getExternalFilesDir("Pictures");
                if (dir != null) dir.mkdirs();
                out = new FileOutputStream(new File(dir, name));
                where = "app Pictures folder";
            }
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            bmp.recycle();
            Toast.makeText(getContext(), "Photo saved to " + where, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(getContext(), "Couldn't save photo", Toast.LENGTH_SHORT).show();
        }
    }
}

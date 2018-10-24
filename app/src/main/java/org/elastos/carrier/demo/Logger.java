package org.elastos.carrier.demo;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class Logger {
    private Logger() {}

    public static final String TAG = "CarrierDemo";

    public static void init(TextView renderer) {
        sRenderer = renderer;
    }

    public static void info(String msg) {
        if(Looper.myLooper() != Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
               info(msg);
            });
            return;
        }

        assert(sRenderer != null);

        CharSequence oldMsg = sRenderer.getText();
        oldMsg = oldMsg + "\n\nI: " + msg;
        sRenderer.setText(oldMsg);

        Log.i(TAG, msg);
    }

    public static void error(String msg) {
        if(Looper.myLooper() != Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                error(msg);
            });
            return;
        }

        assert(sRenderer != null);

        CharSequence oldMsg = sRenderer.getText();
        oldMsg = oldMsg + "\n\nE: " + msg;
        sRenderer.setText(oldMsg);

        Log.e(TAG, msg);
    }

    public static void error(String msg, Throwable tr) {
        if(Looper.myLooper() != Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                error(msg, tr);
            });
            return;
        }

        assert(sRenderer != null);

        CharSequence oldMsg = sRenderer.getText();
        oldMsg = oldMsg + "\n\nE: " + msg;
        StringWriter writer = new StringWriter();
        tr.printStackTrace(new PrintWriter(writer));
        oldMsg = oldMsg + "\nExcepton: " + writer.toString();
        sRenderer.setText(oldMsg);

        Log.e(TAG, msg, tr);
    }

    public static void clear() {
        if(Looper.myLooper() != Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                clear();
            });
            return;
        }

        assert(sRenderer != null);

        sRenderer.setText("");
        sRenderer.scrollTo(0, 0);
    }

    private static TextView sRenderer = null;
}


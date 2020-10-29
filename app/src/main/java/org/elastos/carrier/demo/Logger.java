package org.elastos.carrier.demo;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
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

    public static void debug(String msg) {
        log('D', msg);
    }

    public static void info(String msg) {
        log('I', msg);
    }

    public static void warn(String msg) {
        log('W', msg);
    }

    public static void warn(String msg, Throwable tr) {
        StringWriter writer = new StringWriter();
        tr.printStackTrace(new PrintWriter(writer));
        log('W', msg);
        log('W', "Excepton: " + writer.toString());
    }


    public static void error(String msg) {
        log('E', msg);
    }

    public static void error(String msg, Throwable tr) {
        StringWriter writer = new StringWriter();
        tr.printStackTrace(new PrintWriter(writer));
        log('E', msg);
        log('E', "Excepton: " + writer.toString());
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

    public static void log(char prefix, String msg) {
        if(Looper.myLooper() != Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                log(prefix, msg);
            });
            return;
        }

        int color = Color.BLACK;
        if(prefix == 'D') {
            Log.d(TAG, msg);
            color = Color.GREEN;
        } else if(prefix == 'I') {
            Log.i(TAG, msg);
            color = Color.BLUE;
        } else if(prefix == 'W') {
            Log.w(TAG, msg);
            color = Color.YELLOW;
        } else if(prefix == 'E') {
            Log.e(TAG, msg);
            color = Color.RED;
        }

        assert(sRenderer != null);

        int start = sRenderer.length();
        sRenderer.append("\n " + prefix + ": ");
        sRenderer.append(msg);
        int end = sRenderer.length();

        Spannable spannableText = (Spannable) sRenderer.getText();
        spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);

    }

    private static TextView sRenderer = null;
}


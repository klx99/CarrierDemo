package org.elastos.carrier.demo;

import android.content.Context;
import android.util.Log;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierHandler;

public final class CarrierHelper {
    private CarrierHelper() {}

    public static void startCarrier(Context context) {
        try {
            String dir = context.getFilesDir().getAbsolutePath();
            Carrier.Options options = new DefaultCarrierOptions(dir);
            CarrierHandler handler = new DefaultCarrierHandler();

            Carrier.getInstance(options, handler);
            Carrier carrier = Carrier.getInstance();

            String addr = carrier.getAddress();
            Log.i(TAG, "Carrier Address: " + addr);

            String userID = carrier.getUserId();
            Log.i(TAG, "Carrier UserId: " + userID);

            carrier.start(1000);
            Log.i(TAG, "start carrier.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start carrier.", e);
        }
    }

    public static void stopCarrier() {
        Carrier carrier = Carrier.getInstance();
        if(carrier != null) {
            carrier.kill();
            Log.i(TAG, "stop carrier.");
        }
    }

    public static Carrier getCarrier() {
        return Carrier.getInstance();
    }

    public static final String TAG = "CarrierDemo";
}


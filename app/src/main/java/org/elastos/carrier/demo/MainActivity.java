package org.elastos.carrier.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.demo.session.CarrierSessionHelper;
import org.elastos.carrier.demo.session.CarrierSessionInfo;
import org.elastos.carrier.session.ManagerHandler;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSessionThread = new HandlerThread("CarrierHandleThread");
        mSessionThread.start();

        setContentView(R.layout.activity_main);
        TextView txtMsg = findViewById(R.id.txt_message);
        txtMsg.setMovementMethod(new ScrollingMovementMethod());
        Logger.init(txtMsg);

        Button btnClearMsg = findViewById(R.id.btn_clear_msg);
        btnClearMsg.setOnClickListener((view) -> {
            Logger.clear();
        });

        Button btnMyAddr = findViewById(R.id.btn_my_addr);
        btnMyAddr.setOnClickListener((view) -> {
            showAddress();
        });
        Button btnScanAddr = findViewById(R.id.btn_scan_addr);
        btnScanAddr.setOnClickListener((view) -> {
            scanAddress();
        });
        Button btnSendMsg = findViewById(R.id.btn_send_msg);
        btnSendMsg.setOnClickListener((view) -> {
            sendMessage();
        });

        Button btnCreateSession = findViewById(R.id.btn_create_session);
        btnCreateSession.setOnClickListener((view) -> {
            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                createSession();
            });
        });
        Button btnSendSessionData = findViewById(R.id.btn_send_session_data);
        btnSendSessionData.setOnClickListener((view) -> {
            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                sendSessionData();
            });
        });
        Button btnDeleteSession = findViewById(R.id.btn_delete_session);
        btnDeleteSession.setOnClickListener((view) -> {
            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                deleteSession();
            });
        });

        CarrierHelper.startCarrier(this);
        CarrierSessionHelper.initSessionManager(mSessionManagerHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CarrierSessionHelper.cleanupSessionManager();
        CarrierHelper.stopCarrier();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) {
            Log.d(Logger.TAG,"COULD NOT GET A GOOD RESULT.");
            if(data == null) {
                return;
            }
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if(result == null) {
                return;
            }

            showError("QR Code could not be scanned.");
        }

        if(requestCode == REQUEST_CODE_QR_SCAN) {
            if(data==null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            Log.d(Logger.TAG,"Scan result:"+ result);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Find Address");
            builder.setMessage(result);
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.setPositiveButton("Add Friend", (dialog, which) -> {
                CarrierHelper.addFriend(result);
            });
            builder.create().show();
        }
    }

    private void showAddress() {
        try {
            String address = CarrierHelper.getAddress();
            Log.i(Logger.TAG, "show address: " + address);
            HashMap<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            BitMatrix matrix = new MultiFormatWriter().encode(address, BarcodeFormat.QR_CODE, 512, 512, hintMap);

            //converting bitmatrix to bitmap
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            // All are 0, or black, by default
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);

            TextView txt = new TextView(this);
            txt.setText(address);

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.addView(image);
            root.addView(txt);
            ViewGroup.MarginLayoutParams txtLayout = (ViewGroup.MarginLayoutParams) txt.getLayoutParams();
            txtLayout.setMargins(100, 100, 100, 100);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("My Address");
            builder.setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.setView(root);
            builder.create().show();
        } catch (Exception e) {
            Logger.error("Failed to show address.", e);
        }
    }

    private void scanAddress() {
        Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
        startActivityForResult( i,REQUEST_CODE_QR_SCAN);
    }

    private void sendMessage() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }

        String msg = "Message " + mMsgCounter.getAndIncrement();
        CarrierHelper.sendMessage(msg);
    }

    private void createSession() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo != null) {
            showError("Session has been created.");
            return;
        }

        CarrierSessionInfo sessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId());
        if(sessionInfo == null) {
            Log.e(Logger.TAG, "Failed to new session.");
            return;
        }
        boolean wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_INITIALIZED, 10000);
        if(wait == false) {
            deleteSession();
            Logger.error("Failed to wait session initialize.");
            return;
        }

        CarrierSessionHelper.requestSession(sessionInfo);
        wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_REQUEST_COMPLETED, 10000);
        if(wait == false) {
            deleteSession();
            Logger.error("Failed to wait session request.");
            return;
        }

        CarrierSessionHelper.startSession(sessionInfo);

        mCarrierSessionInfo = sessionInfo;
    }

    private void sendSessionData() {
        if(mCarrierSessionInfo == null) {
            showError("Friend is not online.");
            return;
        }
        boolean connected = mCarrierSessionInfo.mSessionState.isMasked(CarrierSessionInfo.SessionState.SESSION_STREAM_CONNECTED);
        if(connected == false) {
            showError("Session is not connected.");
            return;
        }

        String msg = "Message " + mMsgCounter.getAndIncrement();
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream1, msg.getBytes());
    }

    private void deleteSession() {
        if(mCarrierSessionInfo == null) {
            return;
        }

        CarrierSessionHelper.closeSession(mCarrierSessionInfo);
        mCarrierSessionInfo = null;
    }

    private ManagerHandler mSessionManagerHandler = new ManagerHandler() {
        @Override
        public void onSessionRequest(Carrier carrier, String from, String sdp) {
            CarrierSessionInfo sessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId());
            if(sessionInfo == null) {
                Logger.error("Failed to new session.");
                return;
            }
            boolean wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_INITIALIZED, 10000);
            if(wait == false) {
                deleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            CarrierSessionHelper.replyRequest(sessionInfo);
            wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_TRANSPORTREADY, 10000);
            if(wait == false) {
                deleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            sessionInfo.mSdp = sdp;
            CarrierSessionHelper.startSession(sessionInfo);

            mCarrierSessionInfo = sessionInfo;
        }
    };

    private void showError(String msg) {
        if(Looper.myLooper() != Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                showError(msg);
            });
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(msg);
        builder.setNegativeButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    private HandlerThread mSessionThread;
    private CarrierSessionInfo mCarrierSessionInfo;
    private AtomicInteger mMsgCounter = new AtomicInteger(0);
    private static final int REQUEST_CODE_QR_SCAN = 101;
}

package org.elastos.carrier.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.HandlerThread;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.elastos.carrier.demo.session.CarrierSessionHelper;
import org.elastos.carrier.demo.session.CarrierSessionInfo;
import org.elastos.carrier.session.ManagerHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

public class MenuHelper {
    public static class Carrier {
        public static void ShowAddress() {
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

                ImageView image = new ImageView(mMainActivity);
                image.setImageBitmap(bitmap);

                TextView txt = new TextView(mMainActivity);
                txt.setText(address);

                LinearLayout root = new LinearLayout(mMainActivity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.addView(image);
                root.addView(txt);
                ViewGroup.MarginLayoutParams txtLayout = (ViewGroup.MarginLayoutParams) txt.getLayoutParams();
                txtLayout.setMargins(100, 100, 100, 100);

                AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
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

        public static void ScanAddress() {
            String content = ReadFile(new File("/data/local/tmp/debug-carrier"));
            if(content != null) {
                Intent data = new Intent();
                data.putExtra(QR_SCAN_KEY, content);
                mMainActivity.onActivityResult(REQUEST_CODE_QR_SCAN, Activity.RESULT_OK, data);
                return;
            }

            int hasCameraPermission = ActivityCompat.checkSelfPermission(mMainActivity, Manifest.permission.CAMERA);
            if(hasCameraPermission == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(mMainActivity, QrCodeActivity.class);
                mMainActivity.startActivityForResult(intent, REQUEST_CODE_QR_SCAN);
            } else {
                ActivityCompat.requestPermissions(mMainActivity,
                        new String[]{Manifest.permission.CAMERA},
                        2);
            }
        }

        public static void AddFriend(String peerId) {
            CarrierHelper.addFriend(peerId);
        }

        public static void SendMessage() {
            if(CarrierHelper.getPeerUserId() == null) {
                mMainActivity.showError("Friend is not online.");
                return;
            }

            String msg = "Message " + Math.abs(new Random().nextInt());
            CarrierHelper.sendMessage(msg);
        }

        public static void SendCommand(RPC.Type type) {
            if(CarrierHelper.getPeerUserId() == null) {
                mMainActivity.showError("Friend is not online.");
                return;
            }

            RPC.Request req = RPC.MakeRequest(type);
            byte[] data = MsgPackHelper.PackData(req);
            CarrierHelper.sendMessage(data);
        }

        public static final String QR_SCAN_KEY = "com.blikoon.qrcodescanner.got_qr_scan_relult";
        public static final int REQUEST_CODE_QR_SCAN = 101;
        public static final int REQUEST_PREM_CODE = 102;

        private Carrier() {}
    }

    public static class Session {
        public static void CreateSession() {
            if(CarrierHelper.getPeerUserId() == null) {
                mMainActivity.showError("Friend is not online.");
                return;
            }
            if(mCarrierSessionInfo != null) {
                mMainActivity.showError("Session has been created.");
                return;
            }

            mCarrierSessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId(), onSessionReceivedDataListener);
            if(mCarrierSessionInfo == null) {
                Log.e(Logger.TAG, "Failed to new session.");
                return;
            }
            boolean wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_INITIALIZED, 10000);
            if(wait == false) {
                DeleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            CarrierSessionHelper.requestSession(mCarrierSessionInfo);
            wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_TRANSPORTREADY, 30000);
            if(wait == false) {
                DeleteSession();
                Logger.error("Failed to wait session request transport ready.");
                return;
            }
            wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_REQUEST_COMPLETED, 30000);
            if(wait == false) {
                DeleteSession();
                Logger.error("Failed to wait session request.");
                return;
            }

            CarrierSessionHelper.startSession(mCarrierSessionInfo);
        }

        public static void DeleteSession() {
            if(mCarrierSessionInfo == null) {
                return;
            }

            CarrierSessionHelper.closeSession(mCarrierSessionInfo);
            mCarrierSessionInfo = null;
        }

        public static void WriteData() {
            if(mCarrierSessionInfo == null) {
                mMainActivity.showError("Friend is not online.");
                return;
            }
            boolean connected = mCarrierSessionInfo.mSessionState.isMasked(CarrierSessionInfo.SessionState.SESSION_STREAM_CONNECTED);
            if(connected == false) {
                mMainActivity.showError("Session is not connected.");
                return;
            }

            String msg = "Stream Message Garbage. " + Math.abs(new Random().nextInt());
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());

            msg = "Stream Message Garbage. " + Math.abs(new Random().nextInt());
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());
        }

        public static void SetBinaryData() {
            if(mCarrierSessionInfo == null) {
                mMainActivity.showError("Friend is not online.");
                return;
            }
            boolean connected = mCarrierSessionInfo.mSessionState.isMasked(CarrierSessionInfo.SessionState.SESSION_STREAM_CONNECTED);
            if(connected == false) {
                mMainActivity.showError("Session is not connected.");
                return;
            }

            String msg = "Stream Message Garbage. Stream Message Garbage.";
//        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());

            for(int idx = 0; idx < 2; idx++) {
                SetBinaryDataOnce();
            }

            msg = "Stream Message Garbage. Stream Message Garbage.";
//        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());
        }

        private Session() {}
        private static CarrierSessionInfo mCarrierSessionInfo;
        private static CarrierSessionInfo.OnSessionReceivedDataListener onSessionReceivedDataListener = data -> {
            StringBuilder sb = new StringBuilder(data.length * 2);
            for(byte b: data)
                sb.append(String.format("%02x", b));
            Logger.info("SessionReceivedData: \n" + sb.toString());
            Logger.info("SessionReceivedData: size=" + data.length);
        };

        private static void SetBinaryDataOnce() {
            long magicNumber = 0x0000A5202008275AL;
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ToBytes(magicNumber));

            int version = 10000;
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ToBytes(version));

            RPC.SetBinaryRequest req = new RPC.SetBinaryRequest();
            byte[] head = MsgPackHelper.PackData(req);
            int headSize = head.length;
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ToBytes(headSize));

            int onceSize = 1024;
            long bodySize = onceSize * 1024; // 1MB
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ToBytes(bodySize));

            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, head);

            StringBuffer bodyBuf = new StringBuffer();
            for(int idx = 0; idx < (onceSize / 8); idx++) {
                bodyBuf.append("01234567");
            }
            String body = bodyBuf.toString();
            int sentSize = 0;
            Logger.info("Transfer start. timestamp=" + System.currentTimeMillis());
            for(int idx = 0; idx < (bodySize / onceSize); idx++) {
                int ret = CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, body.getBytes());
                if(ret >= 0) {
                    sentSize += ret;
                }
            }
            String result = (sentSize == bodySize ? "Success" : "Failed");
            Logger.info("Transfer finished. timestamp=" + System.currentTimeMillis());
            Logger.info(result + " to send data. size/total=" + sentSize + "/" + bodySize);
        }

        public static byte[] ToBytes(long value) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(0, value);
            return buffer.array();
        }

        public static byte[] ToBytes(int value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            buffer.putInt(0, value);
            return buffer.array();
        }

        public static long ToLong(byte[] value) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.put(value, 0, value.length);
            buffer.flip(); //need flip
            return buffer.getLong();
        }

    }

    public static void Init(MainActivity activity, CarrierHelper.Listener listener) {
        mMainActivity = activity;

        mSessionThread = new HandlerThread("CarrierHandleThread");
        mSessionThread.start();

        CarrierHelper.startCarrier(activity, listener);
        CarrierSessionHelper.initSessionManager(mSessionManagerHandler);
    }

    public static void Uninit() {
        CarrierSessionHelper.cleanupSessionManager();
        CarrierHelper.stopCarrier();

        mSessionThread.quit();
        mSessionThread = null;
        mMainActivity = null;
    }

    private MenuHelper() {}

    private static String ReadFile(File file) {
        if(file.exists() == false) {
            Logger.info(file.getAbsolutePath() + " is not exists.");
            return null;
        }

        InputStreamReader input_reader = null;
        BufferedReader buf_reader = null;
        try {
            input_reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            buf_reader = new BufferedReader(input_reader);
            String line;
            StringBuffer result = new StringBuffer();
            while ((line = buf_reader.readLine()) != null) {
                if(result.length() > 0) {
                    result.append('\n');
                }

                result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
            Logger.error("Failed to read file: " + file.getAbsolutePath(), e);
        } finally {
            try {
                if (buf_reader != null) buf_reader.close();
                if (input_reader != null) input_reader.close();
            } catch (Exception e) {
            }
        }

        return null;
    }

    private static ManagerHandler mSessionManagerHandler = new ManagerHandler() {
        @Override
        public void onSessionRequest(org.elastos.carrier.Carrier carrier, String from, String sdp) {
            CarrierSessionInfo sessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId(), Session.onSessionReceivedDataListener);
            if(sessionInfo == null) {
                Logger.error("Failed to new session.");
                return;
            }
            boolean wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_INITIALIZED, 10000);
            Logger.error("==================================");
            if(wait == false) {
                Session.DeleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            CarrierSessionHelper.replyRequest(sessionInfo);
            wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_TRANSPORTREADY, 10000);
            if(wait == false) {
                Session.DeleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            sessionInfo.mSdp = sdp;
            CarrierSessionHelper.startSession(sessionInfo);

            Session.mCarrierSessionInfo = sessionInfo;
        }
    };


    private static MainActivity mMainActivity;
    private static HandlerThread mSessionThread;
}

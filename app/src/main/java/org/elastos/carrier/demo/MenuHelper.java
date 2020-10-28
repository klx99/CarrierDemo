package org.elastos.carrier.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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

import org.elastos.carrier.demo.carrier.CarrierHelper;
import org.elastos.carrier.demo.session.CarrierSessionHelper;
import org.elastos.carrier.demo.session.CarrierSessionInfo;
import org.elastos.carrier.session.ManagerHandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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

                ImageView image = new ImageView(mainActivity);
                image.setImageBitmap(bitmap);

                TextView txt = new TextView(mainActivity);
                txt.setText(address);

                LinearLayout root = new LinearLayout(mainActivity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.addView(image);
                root.addView(txt);
                ViewGroup.MarginLayoutParams txtLayout = (ViewGroup.MarginLayoutParams) txt.getLayoutParams();
                txtLayout.setMargins(100, 100, 100, 100);

                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
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
                mainActivity.onActivityResult(REQUEST_CODE_QR_SCAN, Activity.RESULT_OK, data);
                return;
            }

            int hasCameraPermission = ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.CAMERA);
            if(hasCameraPermission == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(mainActivity, QrCodeActivity.class);
                mainActivity.startActivityForResult(intent, REQUEST_CODE_QR_SCAN);
            } else {
                ActivityCompat.requestPermissions(mainActivity,
                        new String[]{Manifest.permission.CAMERA},
                        2);
            }
        }

        public static void AddFriend(String peerId) {
            CarrierHelper.addFriend(peerId);
        }

        public static void SendMessage() {
            if(CarrierHelper.getPeerUserId() == null) {
                mainActivity.showError("Friend is not online.");
                return;
            }

            String msg = "Message " + Math.abs(new Random().nextInt());
            CarrierHelper.sendMessage(msg);
        }

        public static void SendCommand(RPC.Type type) {
            if(CarrierHelper.getPeerUserId() == null) {
                mainActivity.showError("Friend is not online.");
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
            if(Looper.myLooper() != sessionThread.getLooper()) {
                Handler handler = new Handler(sessionThread.getLooper());
                handler.post(() -> CreateSession());
                return;
            }

            if(CarrierHelper.getPeerUserId() == null) {
                mainActivity.showError("Friend is not online.");
                return;
            }
            if(mCarrierSessionInfo != null) {
                mainActivity.showError("Session has been created.");
                return;
            }

            mCarrierSessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId(), carrierSessionListener);
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
            if(Looper.myLooper() != sessionThread.getLooper()) {
                Handler handler = new Handler(sessionThread.getLooper());
                handler.post(() -> DeleteSession());
                return;
            }

            if(mCarrierSessionInfo == null) {
                return;
            }

            CarrierSessionHelper.closeSession(mCarrierSessionInfo);
            mCarrierSessionInfo = null;
        }

        public static void WriteData() {
            if(Looper.myLooper() != sessionThread.getLooper()) {
                Handler handler = new Handler(sessionThread.getLooper());
                handler.post(() -> WriteData());
                return;
            }

            if(mCarrierSessionInfo == null) {
                mainActivity.showError("Friend is not online.");
                return;
            }
            boolean connected = mCarrierSessionInfo.mSessionState.isMasked(CarrierSessionInfo.SessionState.SESSION_STREAM_CONNECTED);
            if(connected == false) {
                mainActivity.showError("Session is not connected.");
                return;
            }

            String msg = "Stream Message Garbage. " + Math.abs(new Random().nextInt());
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());

            msg = "Stream Message Garbage. " + Math.abs(new Random().nextInt());
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());
        }

        public static void SendCommand(RPC.Type type) {
            if(Looper.myLooper() != sessionThread.getLooper()) {
                Handler handler = new Handler(sessionThread.getLooper());
                handler.post(() -> SendCommand(type));
                return;
            }

            if(mCarrierSessionInfo == null) {
                mainActivity.showError("Friend is not online.");
                return;
            }
            boolean connected = mCarrierSessionInfo.mSessionState.isMasked(CarrierSessionInfo.SessionState.SESSION_STREAM_CONNECTED);
            if(connected == false) {
                mainActivity.showError("Session is not connected.");
                return;
            }

            int bodySizeKB = 0;
            if(type == RPC.Type.SetBinary) {
                bodySizeKB = 1024;
            }

            for(int idx = 0; idx < 2; idx++) {
                SendCommandProtocol(type, bodySizeKB);
                if(bodySizeKB >= 0) {
                    SendCommandBody(bodySizeKB);
                }
            }
        }

        private Session() {}
        private static CarrierSessionInfo mCarrierSessionInfo;

        private static void SendCommandProtocol(RPC.Type type, int bodySizeKB) {
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ToBytes(MagicNumber));
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ToBytes(Version));

            RPC.Request req = RPC.MakeRequest(type);
            byte[] headData = MsgPackHelper.PackData(req);
            int headSize = headData.length;
            long bodySize = bodySizeKB * 1024;
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ToBytes(headSize));
            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ToBytes(bodySize));

            CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, headData);
        }

        private static void SendCommandBody(int bodySizeKB) {
            int onceSize = 1024;

            StringBuffer bodyBuf = new StringBuffer();
            for(int idx = 0; idx < (onceSize / 8); idx++) {
                bodyBuf.append("01234567");
            }
            String body = bodyBuf.toString();
            int sentSize = 0;
            Logger.info("Transfer start. timestamp=" + System.currentTimeMillis());
            for(int idx = 0; idx < bodySizeKB; idx++) {
                int ret = CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, body.getBytes());
                if(ret >= 0) {
                    sentSize += ret;
                }
            }
            String result = (sentSize == (bodySizeKB * onceSize) ? "Success" : "Failed");
            Logger.info("Transfer finished. timestamp=" + System.currentTimeMillis());
            Logger.info(result + " to send data. size/total=" + sentSize + "/" + (bodySizeKB * onceSize));
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


        static int ProtocolSize = 32;
        static long MagicNumber = 0x0000A5202008275AL;
        static int Version = 10000;
    }

    public static void Init(MainActivity activity,
                            CarrierHelper.Listener carrierListener,
                            CarrierSessionHelper.Listener sessionListener) {
        MenuHelper.mainActivity = activity;
        MenuHelper.carrierSessionListener = new CarrierSessionHelper.Listener() {
            @Override
            public void onStatus(boolean connected) {
                Handler handler = new Handler(sessionThread.getLooper());
                handler.post(() -> {
                    sessionListener.onStatus(connected);
                });
            }
            @Override
            public void onReceivedData(byte[] data) {
                Handler handler = new Handler(sessionThread.getLooper());
                handler.post(() -> {
                    sessionParser.unpack(data, (headData) -> {
                        sessionListener.onReceivedData(headData);
                    });
                });
            }

            private SessionParser sessionParser = new SessionParser();
        };

        sessionThread = new HandlerThread("CarrierHandleThread");
        sessionThread.start();

        CarrierHelper.startCarrier(activity, carrierListener);
        CarrierSessionHelper.initSessionManager(mSessionManagerHandler);
    }

    public static void Uninit() {
        CarrierSessionHelper.cleanupSessionManager();
        CarrierHelper.stopCarrier();

        sessionThread.quit();
        sessionThread = null;
        mainActivity = null;
    }

    private class SessionProtocol {
        class Info {
            long magicNumber;
            int version;
            int headSize;
            long bodySize;
        };
        class Payload {
            class BodyData {
//                std::filesystem::path filepath;
//                std::fstream stream;
                long receivedBodySize;
            }

            byte[] headData;
            BodyData bodyData;
        }

        Info info = new Info();
        Payload payload = new Payload();

        static final long MagicNumber = 0x00A5202008275AL;
        static final int Version_01_00_00 = 10000;
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
            CarrierSessionInfo sessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId(), carrierSessionListener);
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


    private static MainActivity mainActivity;
    private static CarrierSessionHelper.Listener carrierSessionListener;
    private static HandlerThread sessionThread;
}

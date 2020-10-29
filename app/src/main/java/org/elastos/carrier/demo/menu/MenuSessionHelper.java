package org.elastos.carrier.demo.menu;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import org.elastos.carrier.demo.Logger;
import org.elastos.carrier.demo.MainActivity;
import org.elastos.carrier.demo.MsgPackHelper;
import org.elastos.carrier.demo.RPC;
import org.elastos.carrier.demo.SessionParser;
import org.elastos.carrier.demo.carrier.CarrierHelper;
import org.elastos.carrier.demo.session.CarrierSessionHelper;
import org.elastos.carrier.demo.session.CarrierSessionInfo;
import org.elastos.carrier.session.ManagerHandler;

import java.nio.ByteBuffer;
import java.util.Random;

public class MenuSessionHelper {
    static int ProtocolSize = 32;
    static long MagicNumber = 0x0000A5202008275AL;
    static int Version = 10000;

    static void Init(MainActivity activity,
                     CarrierSessionHelper.Listener sessionListener) {
        MenuSessionHelper.mainActivity = activity;
        MenuSessionHelper.carrierSessionListener = new CarrierSessionHelper.Listener() {
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

        CarrierSessionHelper.initSessionManager(mSessionManagerHandler);
    }

    static void Uninit() {
        CarrierSessionHelper.cleanupSessionManager();

        sessionThread.quit();
        sessionThread = null;
        mainActivity = null;
    }

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
                DeleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            CarrierSessionHelper.replyRequest(sessionInfo);
            wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_TRANSPORTREADY, 10000);
            if(wait == false) {
                DeleteSession();
                Logger.error("Failed to wait session initialize.");
                return;
            }

            sessionInfo.mSdp = sdp;
            CarrierSessionHelper.startSession(sessionInfo);

            mCarrierSessionInfo = sessionInfo;
        }
    };

    protected MenuSessionHelper() {}
    private static CarrierSessionInfo mCarrierSessionInfo;

    private static MainActivity mainActivity;
    private static CarrierSessionHelper.Listener carrierSessionListener;
    private static HandlerThread sessionThread;
}


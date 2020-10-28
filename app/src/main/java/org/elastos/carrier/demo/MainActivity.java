package org.elastos.carrier.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSessionThread = new HandlerThread("CarrierHandleThread");
        mSessionThread.start();

        setContentView(R.layout.activity_main);
        TextView txtMsg = findViewById(R.id.txt_message);
        txtMsg.setMovementMethod(new ScrollingMovementMethod());
        Logger.init(txtMsg);

        /****************************************************/
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
            String address = getAddressFromTmp();
            if(address != null) {
                CarrierHelper.addFriend(address);
                return;
            }

            scanAddress();
        });
        Button btnSendMsg = findViewById(R.id.btn_send_msg);
        btnSendMsg.setOnClickListener((view) -> {
            sendMessage();
        });

        /****************************************************/
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

        /****************************************************/
        Button btnSetBinary = findViewById(R.id.btn_set_binary);
        btnSetBinary.setOnClickListener((view) -> {
            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                setBinaryData();
            });
        });
        Button btnGetBinary = findViewById(R.id.btn_get_binary);
        btnGetBinary.setOnClickListener((view) -> {
            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                getBinaryData();
            });
        });

        Button btnGetVersion = findViewById(R.id.btn_get_service_version);
        btnGetVersion.setOnClickListener((view) -> {
            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                getVersion();
            });
        });

        Button btnReportComment = findViewById(R.id.btn_report_illegal_comment);
        btnReportComment.setOnClickListener((view) -> {
            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                reportIllegalComment();
            });
        });

        Button btnBlockComment = findViewById(R.id.btn_block_comment);
        btnBlockComment.setOnClickListener((view) -> {
            String hint = btnBlockComment.getText().toString();
            boolean blockOrUnblock = hint.startsWith("Block");
            btnBlockComment.setText(blockOrUnblock ? "Unblock\nComment" : "Block\nComment");

            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                blockOrUnblockComment(blockOrUnblock);
            });
        });

        Button btnGetReportedComments = findViewById(R.id.btn_get_reported_comments);
        btnGetReportedComments.setOnClickListener((view) -> {
            Handler handler = new Handler(mSessionThread.getLooper());
            handler.post(() -> {
                getReportedComments();
            });
        });

        /****************************************************/
//        Button btnOpenPFServer = findViewById(R.id.btn_open_pf_svc);
//        btnOpenPFServer.setOnClickListener((view) -> {
//            Handler handler = new Handler(mSessionThread.getLooper());
//            handler.post(() -> {
//                EditText txtIpAddr = new EditText(this);
//                txtIpAddr.setHint("IP Address");
//                txtIpAddr.setText("192.168.33.60");
//                EditText txtPort = new EditText(this);
//                txtPort.setHint("Port");
//                txtPort.setText("8080");
//
//                LinearLayout root = new LinearLayout(this);
//                root.setOrientation(LinearLayout.VERTICAL);
//                root.addView(txtIpAddr);
//                root.addView(txtPort);
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setTitle("PF Server");
//                builder.setView(root);
//                builder.setNegativeButton("Cancel", (dialog, which) -> {
//                    dialog.dismiss();
//                });
//                builder.setPositiveButton("OK", (dialog, which) -> {
//                    String ipaddr = txtIpAddr.getText().toString();
//                    String port = txtPort.getText().toString();
//                    openPFServer(ipaddr, port);
//                });
//                builder.create().show();
//            });
//        });
//        Button btnOpenPFClient = findViewById(R.id.btn_open_pf_cli);
//        btnOpenPFClient.setOnClickListener((view) -> {
//            Handler handler = new Handler(mSessionThread.getLooper());
//            handler.post(() -> {
//                String localIpAddr = getLocalIpAddress();
//                EditText txtIpAddr = new EditText(this);
//                txtIpAddr.setText(localIpAddr);
//                txtIpAddr.setEnabled(false);
//                txtIpAddr.setFocusable(false);
//                txtIpAddr.setFocusableInTouchMode(false);
//                EditText txtPort = new EditText(this);
//                txtPort.setHint("Port");
//                txtPort.setText("12345");
//
//                LinearLayout root = new LinearLayout(this);
//                root.setOrientation(LinearLayout.VERTICAL);
//                root.addView(txtIpAddr);
//                root.addView(txtPort);
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setTitle("PF Client");
//                builder.setView(root);
//                builder.setNegativeButton("Cancel", (dialog, which) -> {
//                    dialog.dismiss();
//                });
//                builder.setPositiveButton("OK", (dialog, which) -> {
//                    String ipaddr = txtIpAddr.getText().toString();
//                    String port = txtPort.getText().toString();
//                    openPFClient(ipaddr, port);
//                });
//                builder.create().show();
//            });
//        });
//        Button btnClosePF = findViewById(R.id.btn_close_pf);
//        btnClosePF.setOnClickListener((view) -> {
//            Handler handler = new Handler(mSessionThread.getLooper());
//            handler.post(() -> {
//                closePF();
//            });
//        });
//        Button btnOpenPFPeerSvc = findViewById(R.id.btn_open_pf_peer_svc);
//        btnOpenPFPeerSvc.setOnClickListener((view) -> {
//            Handler handler = new Handler(mSessionThread.getLooper());
//            handler.post(() -> {
//                EditText txtIpAddr = new EditText(this);
//                txtIpAddr.setHint("IP Address");
//                txtIpAddr.setText("192.168.33.60");
//                EditText txtPort = new EditText(this);
//                txtPort.setHint("Port");
//                txtPort.setText("8080");
//
//                LinearLayout root = new LinearLayout(this);
//                root.setOrientation(LinearLayout.VERTICAL);
//                root.addView(txtIpAddr);
//                root.addView(txtPort);
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setTitle("PF Peer Server");
//                builder.setView(root);
//                builder.setNegativeButton("Cancel", (dialog, which) -> {
//                    dialog.dismiss();
//                });
//                builder.setPositiveButton("OK", (dialog, which) -> {
//                    String ipaddr = txtIpAddr.getText().toString();
//                    String port = txtPort.getText().toString();
//                    openPFPeerServer(ipaddr, port);
//                });
//                builder.create().show();
//            });
//        });

        /****************************************************/
//        Button btnOpenChannel = findViewById(R.id.btn_open_channel);
//        btnOpenChannel.setOnClickListener((view) -> {
//            Handler handler = new Handler(mSessionThread.getLooper());
//            handler.post(() -> {
//                openChannel();
//            });
//        });
//        Button btnSendChannelData = findViewById(R.id.btn_send_channel_data);
//        btnSendChannelData.setOnClickListener((view) -> {
//            Handler handler = new Handler(mSessionThread.getLooper());
//            handler.post(() -> {
//                sendChannelData();
//            });
//        });
//        Button btnCloseChannel = findViewById(R.id.btn_close_channel);
//        btnCloseChannel.setOnClickListener((view) -> {
//            Handler handler = new Handler(mSessionThread.getLooper());
//            handler.post(() -> {
//                closeChannel();
//            });
//        });


        /****************************************************/
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        return super.onKeyUp(keyCode, event);
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
        int hasCameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(hasCameraPermission == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MainActivity.this, QrCodeActivity.class);
            startActivityForResult( intent, REQUEST_CODE_QR_SCAN);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != 1) {
            return;
        }

        for (int idx = 0; idx < permissions.length; idx++) {
            if(permissions[idx].equals(Manifest.permission.CAMERA) == false) {
                continue;
            }

            if (grantResults[idx] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MainActivity.this, QrCodeActivity.class);
                startActivityForResult( intent, REQUEST_CODE_QR_SCAN);
            }
        }
    }

    private String getAddressFromTmp() {
        String content = readFile(new File("/data/local/tmp/debug-carrier"));
        return content;
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

        mCarrierSessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId(), onSessionReceivedDataListener);
        if(mCarrierSessionInfo == null) {
            Log.e(Logger.TAG, "Failed to new session.");
            return;
        }
        boolean wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_INITIALIZED, 10000);
        if(wait == false) {
            deleteSession();
            Logger.error("Failed to wait session initialize.");
            return;
        }

        CarrierSessionHelper.requestSession(mCarrierSessionInfo);
        wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_TRANSPORTREADY, 30000);
        if(wait == false) {
            deleteSession();
            Logger.error("Failed to wait session request transport ready.");
            return;
        }
        wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_REQUEST_COMPLETED, 30000);
        if(wait == false) {
            deleteSession();
            Logger.error("Failed to wait session request.");
            return;
        }

        CarrierSessionHelper.startSession(mCarrierSessionInfo);
    }

    private void sendSetBinaryData() {
        long magicNumber = 0x0000A5202008275AL;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, toBytes(magicNumber));

        int version = 10000;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, toBytes(version));

        byte[] head = makeProtocolSetBinaryRequestHead();
        int headSize = head.length;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, toBytes(headSize));

        int onceSize = 1024;
        long bodySize = onceSize * 1024; // 1MB
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, toBytes(bodySize));

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

    private void sendGetBinaryData() {
        long magicNumber = 0xA5A5202008275A5AL;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, toBytes(magicNumber));

        int version = 10000;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, toBytes(version));

        byte[] head = makeProtocolGetBinaryRequestHead();
        int headSize = head.length;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, toBytes(headSize));

        long bodySize = 0;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, toBytes(bodySize));

        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, head);
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

        String msg = "Stream Message Garbage. Stream Message Garbage.";
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());

        for(int idx = 0; idx < 2; idx++) {
//            sendSessionSection();
        }

        msg = "Stream Message Garbage. Stream Message Garbage.";
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());
    }

    private void setBinaryData() {
        if(mCarrierSessionInfo == null) {
            showError("Friend is not online.");
            return;
        }
        boolean connected = mCarrierSessionInfo.mSessionState.isMasked(CarrierSessionInfo.SessionState.SESSION_STREAM_CONNECTED);
        if(connected == false) {
            showError("Session is not connected.");
            return;
        }

        String msg = "Stream Message Garbage. Stream Message Garbage.";
//        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());

        for(int idx = 0; idx < 2; idx++) {
            sendSetBinaryData();
        }

        msg = "Stream Message Garbage. Stream Message Garbage.";
//        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());
    }

    private void getBinaryData() {
        if(mCarrierSessionInfo == null) {
            showError("Friend is not online.");
            return;
        }
        boolean connected = mCarrierSessionInfo.mSessionState.isMasked(CarrierSessionInfo.SessionState.SESSION_STREAM_CONNECTED);
        if(connected == false) {
            showError("Session is not connected.");
            return;
        }

        String msg = "Stream Message Garbage. Stream Message Garbage.";
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());

        for(int idx = 0; idx < 2; idx++) {
            sendGetBinaryData();
        }

        msg = "Stream Message Garbage. Stream Message Garbage.";
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, msg.getBytes());
    }

    private void getVersion() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }

        GetVersionRequest req = new GetVersionRequest();
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packMapHeader(4)
                    .packString("version").packString(req.version)
                    .packString("method").packString(req.method)
                    .packString("id").packInt(req.jsonrpc_id)
                    .packString("params").packMapHeader(1)
                    .packString("access_token").packString(req.params.access_token);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        CarrierHelper.sendMessage(packer.toByteArray());
    }

    private void reportIllegalComment() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }

        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packMapHeader(4)
                .packString("version").packString("1.0")
                .packString("method").packString("report_illegal_comment")
                .packString("id").packInt(12345)
                .packString("params").packMapHeader(5)
                    .packString("access_token").packString("access-token-test")
                    .packString("channel_id").packLong(1)
                    .packString("post_id").packLong(2)
                    .packString("comment_id").packLong(1)
                    .packString("reasons").packString("just a joke");
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        CarrierHelper.sendMessage(packer.toByteArray());
    }

    private void blockOrUnblockComment(boolean blockOrUnblock) {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }

        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            String method = blockOrUnblock ? "block_comment" : "unblock_comment";
            packer.packMapHeader(4)
                    .packString("version").packString("1.0")
                    .packString("method").packString(method)
                    .packString("id").packInt(12345)
                    .packString("params").packMapHeader(4)
                    .packString("access_token").packString("access-token-test")
                    .packString("channel_id").packLong(1)
                    .packString("post_id").packLong(2)
                    .packString("comment_id").packLong(1);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        CarrierHelper.sendMessage(packer.toByteArray());
    }

    private void getReportedComments() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }

        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packMapHeader(4)
                .packString("version").packString("1.0")
                .packString("method").packString("get_reported_comments")
                .packString("id").packInt(12345)
                .packString("params").packMapHeader(5)
                    .packString("access_token").packString("access-token-test")
                    .packString("by").packLong(3)
                    .packString("upper_bound").packLong(0)
                    .packString("lower_bound").packLong(0)
                    .packString("max_count").packLong(0);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        CarrierHelper.sendMessage(packer.toByteArray());
    }

    private void deleteSession() {
        if(mCarrierSessionInfo == null) {
            return;
        }

        CarrierSessionHelper.closeSession(mCarrierSessionInfo);
        mCarrierSessionInfo = null;
    }

    private void openPFServer(String ipaddr, String port) {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo == null) {
            showError("Session has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mStream == null) {
            showError("Stream has not been created.");
            return;
        }

        CarrierSessionHelper.addServer(mCarrierSessionInfo, ipaddr, port);
        Logger.info("Add server. ipaddr=" + ipaddr + " port=" + port);
    }

    private void openPFClient(String ipaddr, String port) {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo == null) {
            showError("Session has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mStream == null) {
            showError("Stream has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mPortForwarding > 0) {
            showError("PortForwarding has not been created.");
            return;
        }

        mCarrierSessionInfo.mPortForwarding = CarrierSessionHelper.openPortForwarding(mCarrierSessionInfo.mStream, ipaddr, port);
        if(mCarrierSessionInfo.mPortForwarding <= 0) {
            Logger.error("Failed to open port forwarding. retval=" + mCarrierSessionInfo.mPortForwarding);
            return;
        }

        Logger.info("Open port forwarding. id=" + mCarrierSessionInfo.mPortForwarding  + " ipaddr=" + ipaddr + " port=" + port);
    }

    private void closePF() {
        if(mCarrierSessionInfo == null) {
            return;
        }

        CarrierSessionHelper.closePortForwarding(mCarrierSessionInfo.mStream, mCarrierSessionInfo.mPortForwarding);
        mCarrierSessionInfo.mPortForwarding = -1;

        CarrierSessionHelper.removeServer(mCarrierSessionInfo);
    }

    private void openPFPeerServer(String ipaddr, String port) {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo == null) {
            showError("Session has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mStream == null) {
            showError("Stream has not been created.");
            return;
        }

        String ipport = "addServer:" + ipaddr + ":" + port;
        CarrierSessionHelper.sendData(mCarrierSessionInfo.mStream, ipport.getBytes());
        Logger.info("Add peer server. ipaddr=" + ipaddr + " port=" + port);
    }

    private void openChannel() {
        if(CarrierHelper.getPeerUserId() == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo == null) {
            showError("Session has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mStream == null) {
            showError("Stream has not been created.");
            return;
        }
        if(mCarrierSessionInfo.mChannel > 0) {
            showError("Channel has not been created.");
            return;
        }

        mCarrierSessionInfo.mChannel = CarrierSessionHelper.openChannel(mCarrierSessionInfo.mStream,"channel-0");
        if(mCarrierSessionInfo.mChannel <= 0) {
            Logger.error("Failed to open channel. retval=" + mCarrierSessionInfo.mChannel);
            return;
        }
        boolean wait = mCarrierSessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_CHANNEL_OPENED, 10000);
        if(wait == false) {
            closeChannel();
            Logger.error("Failed to wait channel open.");
            return;
        }
    }

    private void sendChannelData() {
        if(mCarrierSessionInfo == null) {
            showError("Friend is not online.");
            return;
        }
        if(mCarrierSessionInfo.mChannel <= 0) {
            showError("Channel is not opened.");
            return;
        }

        String msg = "Channel Message " + mMsgCounter.getAndIncrement();
        CarrierSessionHelper.sendChannelData(mCarrierSessionInfo.mStream, mCarrierSessionInfo.mChannel, msg.getBytes());
    }

    private void closeChannel() {
        if(mCarrierSessionInfo == null) {
            return;
        }

        CarrierSessionHelper.closeChannel(mCarrierSessionInfo.mStream, mCarrierSessionInfo.mChannel);
        mCarrierSessionInfo.mChannel = -1;
    }

    private ManagerHandler mSessionManagerHandler = new ManagerHandler() {
        @Override
        public void onSessionRequest(Carrier carrier, String from, String sdp) {
            CarrierSessionInfo sessionInfo = CarrierSessionHelper.newSessionAndStream(CarrierHelper.getPeerUserId(), onSessionReceivedDataListener);
            if(sessionInfo == null) {
                Logger.error("Failed to new session.");
                return;
            }
            boolean wait = sessionInfo.mSessionState.waitForState(CarrierSessionInfo.SessionState.SESSION_STREAM_INITIALIZED, 10000);
            Logger.error("==================================");
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

    private static String getLocalIpAddress() {
        String ipaddr = null;
        try {
            Enumeration<NetworkInterface> enum_ni = NetworkInterface.getNetworkInterfaces();
            while(enum_ni.hasMoreElements()) {
                NetworkInterface net_if = enum_ni.nextElement();
                String net_name = net_if.getName();
                Enumeration<InetAddress> enum_ia = net_if.getInetAddresses();
                while(enum_ia.hasMoreElements()) {
                    InetAddress ia = enum_ia.nextElement();
                    if (ia.isSiteLocalAddress() == false
                    || ia.isLoopbackAddress() == true
                    || ia.isLinkLocalAddress() == true ) {
                        continue;
                    }

                    if(net_name.startsWith("eth")) {
                        ipaddr = ia.getHostAddress();
                    } else if(ipaddr == null
                            && net_name.startsWith("wlan")) {
                        ipaddr = ia.getHostAddress();
                    }

                    Logger.info("get device name=" + net_name + " ipaddr=" + ipaddr);
                }
            }
        } catch (SocketException ex) {
            Logger.error(ex.toString());
        }

        return ipaddr;
    }

    public static String readFile(File file) {
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

//    private long hton(long value) {
//        if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)) {
//            return value;
//        }
//        return Long.reverseBytes(value);
//    }
//
//    private int hton(int value) {
//        if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)) {
//            return value;
//        }
//        return Integer.reverseBytes(value);
//    }

    public byte[] toBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(0, value);
        return buffer.array();
    }

    public byte[] toBytes(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(0, value);
        return buffer.array();
    }

    public long toLong(byte[] value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(value, 0, value.length);
        buffer.flip(); //need flip
        return buffer.getLong();
    }

    private byte[] makeProtocolSetBinaryRequestHead() {
        SetBinaryRequest req = new SetBinaryRequest();
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packMapHeader(4)
                        .packString("version").packString(req.version)
                        .packString("method").packString(req.method)
                        .packString("id").packInt(req.jsonrpc_id)
                        .packString("params").packMapHeader(4)
                            .packString("access_token").packString(req.params.access_token)
                            .packString("key").packString(req.params.key)
                            .packString("algo").packString(req.params.algo)
                            .packString("checksum").packString(req.params.checksum);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private byte[] makeProtocolGetBinaryRequestHead() {
        GetBinaryRequest req = new GetBinaryRequest();
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packMapHeader(4)
                    .packString("version").packString(req.version)
                    .packString("method").packString(req.method)
                    .packString("id").packInt(req.jsonrpc_id)
                    .packString("params").packMapHeader(2)
                        .packString("access_token").packString(req.params.access_token)
                        .packString("key").packString(req.params.key);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    class SetBinaryRequest {
        String version = "1.0";
        String method = "set_binary";
        int jsonrpc_id = 1010301;
        Params params = new Params();

        class Params {
            String access_token= "access-token-test";
            String key         = "key-test";
            String algo        = "None";
            String checksum    = "";
        }
    }
    class SetBinaryResponse {
        long tsx_id = 1010302;
        Result result = new Result();

        class Result {
            String key         = "key-test";
        }
    }

    class GetBinaryRequest {
        String version = "1.0";
        String method = "get_binary";
        int jsonrpc_id = 1010302;
        Params params = new Params();

        class Params {
            String access_token= "access-token-test";
            String key         = "key-test";
        }
    }
    class GetBinaryResponse {
        long tsx_id = 1010302;
        Result result = new Result();

        class Result {
            String key         = "key-test";
            String algo        = "None";
            String checksum    = "";
        }
    }

    class GetVersionRequest {
        String version = "1.0";
        String method = "get_service_version";
        int jsonrpc_id = 1010302;
        Params params = new Params();

        class Params {
            String access_token= "access-token-test";
        }
    }

    CarrierSessionInfo.OnSessionReceivedDataListener onSessionReceivedDataListener = data -> {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for(byte b: data)
            sb.append(String.format("%02x", b));
        Logger.info("SessionReceivedData: \n" + sb.toString());
        Logger.info("SessionReceivedData: size=" + data.length);
    };

    private HandlerThread mSessionThread;
    private CarrierSessionInfo mCarrierSessionInfo;
    private AtomicInteger mMsgCounter = new AtomicInteger(0);
    private static final int REQUEST_CODE_QR_SCAN = 101;
}

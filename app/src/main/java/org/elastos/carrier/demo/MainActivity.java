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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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

        /****************************************************/

        /****************************************************/
        CarrierHelper.startCarrier(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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

    private HandlerThread mSessionThread;
    private AtomicInteger mMsgCounter = new AtomicInteger(0);
    private static final int REQUEST_CODE_QR_SCAN = 101;
}

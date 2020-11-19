package org.elastos.carrier.demo.menu;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import org.elastos.carrier.demo.Logger;
import org.elastos.carrier.demo.MainActivity;
import org.elastos.carrier.demo.MsgPackHelper;
import org.elastos.carrier.demo.RPC;
import org.elastos.carrier.demo.carrier.CarrierHelper;
import org.elastos.carrier.demo.session.CarrierSessionHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Random;

public class MenuCarrierHelper {
    static void Init(MainActivity activity,
                     CarrierHelper.Listener carrierListener) {
        MenuCarrierHelper.mainActivity = activity;
        CarrierHelper.startCarrier(activity, carrierListener);
    }

    public static void Uninit() {
        CarrierSessionHelper.cleanupSessionManager();
        CarrierHelper.stopCarrier();

        mainActivity = null;
    }

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
        if(type == RPC.Type.SetBinary) {
            ((RPC.SetBinaryRequest)req).params.content = "test bin data".getBytes();
        }
        byte[] data = MsgPackHelper.PackData(req);
        CarrierHelper.sendMessage(data);
    }

    public static final String QR_SCAN_KEY = "com.blikoon.qrcodescanner.got_qr_scan_relult";
    public static final int REQUEST_CODE_QR_SCAN = 101;
    public static final int REQUEST_PREM_CODE = 102;

    protected MenuCarrierHelper() {}

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

    private static MainActivity mainActivity;
}


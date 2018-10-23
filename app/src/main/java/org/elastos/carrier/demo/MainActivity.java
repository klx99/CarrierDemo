package org.elastos.carrier.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
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

import java.util.HashMap;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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
    }

    @Override
    protected void onStart() {
        super.onStart();
        CarrierHelper.startCarrier(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
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

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Scan Error");
            builder.setMessage("QR Code could not be scanned");
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.create().show();
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
                mPeerAddress = result;
                CarrierHelper.addFriend(mPeerAddress);
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
        String msg = "Message " + mMsgCounter++;
        CarrierHelper.sendMessage(msg);
    }

    private int mMsgCounter = 0;
    private String mPeerAddress = null;
    private static final int REQUEST_CODE_QR_SCAN = 101;
}

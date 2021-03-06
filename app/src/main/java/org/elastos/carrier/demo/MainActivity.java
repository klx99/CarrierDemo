package org.elastos.carrier.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;

import org.elastos.carrier.demo.carrier.CarrierHelper;
import org.elastos.carrier.demo.menu.MenuHelper;
import org.elastos.carrier.demo.session.CarrierSessionHelper;

import java.util.Date;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView txtMsg = findViewById(R.id.txt_message);
        txtMsg.setMovementMethod(new ScrollingMovementMethod());
        Logger.init(txtMsg);

        cmdThread = new HandlerThread("command-thread");
        cmdThread.start();
        cmdHandler = new Handler(cmdThread.getLooper());
        cmdHandler.post(() -> {
            MenuHelper.Init(this, carrierListener, sessionListener);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        MenuHelper.Uninit();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        optionsMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_clear_log:
                Logger.clear();
                return true;

            case R.id.action_carrier_my_address:
                MenuHelper.Carrier.ShowAddress();
                return true;
            case R.id.action_carrier_scan_address:
                MenuHelper.Carrier.ScanAddress();
                return true;
            case R.id.action_carrier_send_message:
                MenuHelper.Carrier.SendMessage();
                return true;

            case R.id.action_session_create:
                MenuHelper.Session.CreateSession();
                return true;
            case R.id.action_session_destroy:
                MenuHelper.Session.DeleteSession();
                return true;
            case R.id.action_session_write:
                MenuHelper.Session.WriteData();
                return true;
        }

        RPC.Type type = null;
        switch (id) {
            case R.id.action_carrier_get_version:
                type = RPC.Type.GetVersion;
                break;
            case R.id.action_carrier_backup_service_data:
                type = RPC.Type.BackupServiceData;
                break;
            case R.id.action_carrier_restore_service_data:
                type = RPC.Type.RestoreServiceData;
                break;
            case R.id.action_carrier_download_new_service:
                type = RPC.Type.DownloadNewService;
                break;
            case R.id.action_carrier_start_new_service:
                type = RPC.Type.StartNewService;
                break;
            case R.id.action_carrier_declare_post:
                type = RPC.Type.DeclarePost;
                break;
            case R.id.action_carrier_notify_post:
                type = RPC.Type.NotifyPost;
                break;
            case R.id.action_carrier_report_comment:
                type = RPC.Type.ReportIllegalComment;
                break;
//            case R.id.action_carrier_block_comment:
//                type = RPC.Type.BlockComment;
//                break;
//            case R.id.action_carrier_get_reported_comments:
//                type = RPC.Type.GetReportedComments;
//                break;
            case R.id.action_carrier_sign_in:
                type = RPC.Type.SignIn;
                break;
            case R.id.action_carrier_did_auth:
                type = RPC.Type.DidAuth;
                break;
            case R.id.action_carrier_enable_notify:
                type = RPC.Type.EnableNotify;
                break;
            case R.id.action_carrier_get_multi_comments:
                type = RPC.Type.GetMultiComments;
                break;
            case R.id.action_carrier_get_multi_likes_and_comments_count:
                type = RPC.Type.GetMultiLikesAndCommentsCount;
                break;
            case R.id.action_carrier_get_multi_subscribers_count:
                type = RPC.Type.GetMultiSubscribersCount;
                break;
            case R.id.action_carrier_set_bindata:
                type = RPC.Type.SetBinary;
                break;
            case R.id.action_carrier_get_bindata:
                type = RPC.Type.GetBinary;
                break;
        }
        if(type != null) {
            RPC.Type finalType = type;
            cmdHandler.post(() -> {
                MenuHelper.Carrier.SendCommand(finalType);
            });
            return true;
        }

        switch (id) {
        case R.id.action_session_set_bindata:
            type = RPC.Type.SetBinary;
            break;
        case R.id.action_session_get_bindata:
            type = RPC.Type.GetBinary;
            break;
        }
        if(type != null) {
            RPC.Type finalType = type;
            cmdHandler.post(() -> {
                MenuHelper.Session.SendCommand(finalType);
            });
            return true;
        }

        switch (id) {
            case R.id.action_onedrive_login:
                type = RPC.Type.OneDriveLogin;
                break;
        }
        if(type != null) {
            RPC.Type finalType = type;
            cmdHandler.post(() -> {
                MenuHelper.OneDrive.SendCommand(finalType);
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) {
            if(data == null) {
                return;
            }
            showError("QR Code could not be scanned.");
        }

        if(requestCode == MenuHelper.Carrier.REQUEST_CODE_QR_SCAN) {
            if(data==null)
                return;

            //Getting the passed result
            String result = data.getStringExtra(MenuHelper.Carrier.QR_SCAN_KEY);
            Log.d(Logger.TAG,"Scan result:"+ result);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Find Address");
            builder.setMessage(result);
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.setPositiveButton("Add Friend", (dialog, which) -> {
                MenuHelper.Carrier.AddFriend(result);
            });
            builder.create().show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MenuHelper.Carrier.REQUEST_PREM_CODE) {
            for (int idx = 0; idx < permissions.length; idx++) {
                if (permissions[idx].equals(Manifest.permission.CAMERA) == false) {
                    continue;
                }

                if (grantResults[idx] == PackageManager.PERMISSION_GRANTED) {
                    MenuHelper.Carrier.ScanAddress();
                }
            }
        }
    }

    public void showError(String msg) {
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

    CarrierHelper.Listener carrierListener = new CarrierHelper.Listener() {
        @Override
        public void onStatus(boolean online) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                if(online == false) {
                    optionsMenu.setGroupEnabled(R.id.group_carrier, false);
                } else {
                    optionsMenu.findItem(R.id.action_carrier_my_address).setEnabled(true);
                    optionsMenu.findItem(R.id.action_carrier_scan_address).setEnabled(true);
                }
            });
        }

        @Override
        public void onFriendStatus(boolean online) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                optionsMenu.setGroupEnabled(R.id.group_carrier, online);
                optionsMenu.findItem(R.id.action_carrier_my_address).setEnabled(true);
                optionsMenu.findItem(R.id.action_carrier_scan_address).setEnabled(true);

                optionsMenu.findItem(R.id.action_session_create).setEnabled(online);
                optionsMenu.findItem(R.id.action_session_destroy).setEnabled(online);
            });
        }

        @Override
        public void onReceivedMessage(byte[] data) {
            RPC.Response resp = MsgPackHelper.UnpackData(data);
            if(resp == null) {
                Logger.info("Failed to unpack received data.");
                return;
            }

            Logger.info(resp.toString());
        }
    };

    CarrierSessionHelper.Listener sessionListener = new CarrierSessionHelper.Listener() {
        @Override
        public void onStatus(boolean connected) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                if(connected == false) {
                    MenuItem itemCreate = optionsMenu.findItem(R.id.action_session_create);
                    boolean actionEnabled = itemCreate.isEnabled();
                    optionsMenu.setGroupEnabled(R.id.group_session, false);
                    optionsMenu.findItem(R.id.action_session_create).setEnabled(actionEnabled);
                    optionsMenu.findItem(R.id.action_session_destroy).setEnabled(actionEnabled);
                } else {
                    optionsMenu.setGroupEnabled(R.id.group_session, true);
                }
            });
        }

        @Override
        public void onReceivedData(byte[] data) {
            RPC.Response resp = MsgPackHelper.UnpackData(data);
            if(resp == null) {
                Logger.info("Failed to unpack received data.");
                return;
            }

            Logger.info(resp.toString());
        }
    };

    Menu optionsMenu;
    HandlerThread cmdThread;
    Handler cmdHandler;
}

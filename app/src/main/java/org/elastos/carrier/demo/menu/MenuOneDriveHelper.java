package org.elastos.carrier.demo.menu;

import android.content.Intent;
import android.net.Uri;

import org.elastos.carrier.demo.Logger;
import org.elastos.carrier.demo.MainActivity;
import org.elastos.carrier.demo.RPC;
import org.json.JSONObject;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.OkHttpClient;

public class MenuOneDriveHelper {
    static void Init(MainActivity activity) {
        mainActivity = activity;
    }

    public static void Uninit() {
        mainActivity = null;
    }

    public static String GetAccessToken() {
        return accessToken;
    }

    public static void SendCommand(RPC.Type type) {
        if(type == RPC.Type.OneDriveLogin) {
            Login();
        }
    }

    private static void Login() {
        if(httpServer == null) {
            try {
                httpServer = new HttpServer();
                httpServer.start();
            } catch (Exception e) {
                Logger.error("Failed to strat http server to port:" + REDIRECT_PORT, e);
                return;
            }
        }

        String oauthUrl = AUTH_URL + "/authorize"
                + "?client_id=" + CLIENT_ID
                + "&scope=Files.ReadWrite%20Files.ReadWrite.All%20Sites.ReadWrite.All%20offline_access"
                + "&response_type=code&redirect_uri=" + "http://" + REDIRECT_HOST + ":" + REDIRECT_PORT;
        Logger.info("request url:" + oauthUrl);
        Intent intent= new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(oauthUrl);
        intent.setData(content_url);
        mainActivity.startActivity(intent);

        return;
    }

    private static String GetAccessToken(String authCode) {
        Logger.info("OneDrive Code=" + authCode);

        okhttp3.RequestBody formBody = new okhttp3.FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("redirect_uri", "http://" + REDIRECT_HOST + ":" + REDIRECT_PORT)
//                .add("client_secret", CLIENT_SECRET)
                .add("code", authCode)
                .add("grant_type", "authorization_code")
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(AUTH_URL + "/token")
                .post(formBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        okhttp3.Call call = client.newCall(request);
        try {
            okhttp3.Response response = call.execute();
            String responseBody = response.body().string();
            Logger.info("OneDrive AccessToken response:" + responseBody);

            JSONObject jsonBody = new JSONObject(responseBody);
            String accessToken = jsonBody.getString("access_token");

            return accessToken;
        } catch (Exception e) {
            Logger.error("Failed to get access token from code:" + authCode, e);
            return null;
        }
    }

    static class HttpServer extends NanoHTTPD {
        public HttpServer() {
            super(REDIRECT_HOST, REDIRECT_PORT);
        }

        public Response serve(IHTTPSession session){
            Logger.info("HttpServer request:" + session.getUri());

            String response = null;
            String[] query = session.getQueryParameterString().split("=");
            switch (query[0]) {
            case "code":
                if(accessToken == null) {
                    accessToken = GetAccessToken(query[1]);
                }
                Logger.info("OneDrive AccessToken=" + accessToken);
                response = accessToken;
                break;
            case "error":
            default:
                response = session.getQueryParameterString();
                break;
            }
            String builder = "<!DOCTYPE html><html><body>" + response + "</body></html>\n";
            return newFixedLengthResponse(builder);
        }

    };

    private static MainActivity mainActivity;
    private static HttpServer httpServer;
    private static String accessToken;

    private static final String AUTH_URL = "https://login.microsoftonline.com/common/oauth2/v2.0";
    private static final String CLIENT_ID = "cb60bc27-403e-4128-a3ca-803151b5c09c";
    private static final String REDIRECT_HOST = "localhost";
    private static final int REDIRECT_PORT = 12345;

}


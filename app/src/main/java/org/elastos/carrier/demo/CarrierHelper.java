package org.elastos.carrier.demo;

import android.content.Context;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierHandler;
import org.elastos.carrier.FriendInfo;
import org.elastos.carrier.demo.session.CarrierSessionHelper;

import java.util.List;

public final class CarrierHelper {
    private CarrierHelper() {}

    public static void startCarrier(Context context) {
        try {
            String dir = context.getFilesDir().getAbsolutePath();
            Carrier.Options options = new DefaultCarrierOptions(dir);
            CarrierHandler handler = new DefaultCarrierHandler();

            Carrier.getInstance(options, handler);
            Carrier carrier = Carrier.getInstance();

            String addr = carrier.getAddress();
            Logger.info("Carrier Address: " + addr);

            String userID = carrier.getUserId();
            Logger.info("Carrier UserId: " + userID);

            carrier.start(1000);
            Logger.info("start carrier.");
        } catch (Exception e) {
            Logger.error("Failed to start carrier.", e);
        }
    }

    public static void stopCarrier() {
        Carrier carrier = Carrier.getInstance();
        if(carrier != null) {
            carrier.kill();
            Logger.info("stop carrier.");
        }
    }

    public static String getAddress() {
        String addr = null;
        try {
            addr = Carrier.getInstance().getAddress();
        } catch (Exception e) {
            Logger.error("Failed to get address.", e);
        }
        return addr;
    }

    public static List<FriendInfo> getFriendList() {
        List<FriendInfo> friendList = null;
        try {
            friendList = Carrier.getInstance().getFriends();
        } catch (Exception e) {
            Logger.error("Failed to get friend list.", e);
        }
        return friendList;
    }

    public static void addFriend(String peerAddr) {
        try {
            String userId = Carrier.getIdFromAddress(peerAddr);
            if(Carrier.getInstance().isFriend(userId)) {
                Logger.info("Carrier ignore to add friend address: " + peerAddr);
                return;
            }

            Carrier.getInstance().addFriend(peerAddr, CARRIER_HELLO_AUTH);
            Logger.info("Carrier add friend address: " + peerAddr);
        } catch (Exception e) {
            Logger.error("Failed to add friend.", e);
        }
        return;
    }

    public static void acceptFriend(String peerUserId, String hello) {
        try {
            if (hello.equals(CARRIER_HELLO_AUTH) == false) {
                Logger.error("Ignore to accept friend, not expected.");
                return;
            }

            Carrier.getInstance().AcceptFriend(peerUserId);
            Logger.info("Carrier accept friend UserId: " + peerUserId);
        } catch (Exception e) {
            Logger.error("Failed to add friend.", e);
        }
    }

    public static void sendMessage(String message) {
        if(sPeerUserId == null) {
            Logger.error("Failed to send message, friend not found.");
            return;
        }

        try {
            Carrier.getInstance().sendFriendMessage(sPeerUserId, message);
            Logger.info("Carrier send message to UserId: " + sPeerUserId
                    + "\nmessage: " + message);
        } catch (Exception e) {
            Logger.error("Failed to send message.", e);
        }
    }

    public static void setPeerUserId(String peerUserId) {
        sPeerUserId = peerUserId;
    }

    public static String getPeerUserId() {
        return sPeerUserId;
    }

    private static String sPeerUserId = null;

    private static final String CARRIER_HELLO_AUTH = "auto-auth";
}


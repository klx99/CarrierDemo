package org.elastos.carrier.demo;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.ConnectionStatus;
import org.elastos.carrier.FriendInfo;
import org.elastos.carrier.UserInfo;

public class DefaultCarrierHandler extends AbstractCarrierHandler {
    @Override
    public void onConnection(Carrier carrier, ConnectionStatus status) {
        Logger.info("Carrier connection status: " + status);
    }

    @Override
    public void onFriendRequest(Carrier carrier, String userId, UserInfo info, String hello) {
        Logger.info("Carrier received friend request. peer UserId: " + userId);
        CarrierHelper.acceptFriend(userId, hello);
        CarrierHelper.setPeerUserId(info.getUserId());
    }

    @Override
    public void onFriendAdded(Carrier carrier, FriendInfo info) {
        Logger.info("Carrier friend added. peer UserId: " + info.getUserId());
        CarrierHelper.setPeerUserId(info.getUserId());
    }

    @Override
    public void onFriendMessage(Carrier carrier, String from, String message) {
        Logger.info("Carrier receiver message from UserId: " + from
                + "\nmessage: " + message);
    }
}


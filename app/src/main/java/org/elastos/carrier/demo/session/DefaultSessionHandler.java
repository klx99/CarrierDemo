package org.elastos.carrier.demo.session;

import org.elastos.carrier.session.AbstractStreamHandler;
import org.elastos.carrier.session.CloseReason;
import org.elastos.carrier.session.Session;
import org.elastos.carrier.session.SessionRequestCompleteHandler;
import org.elastos.carrier.session.Stream;
import org.elastos.carrier.session.StreamState;

public class DefaultSessionHandler extends AbstractStreamHandler implements SessionRequestCompleteHandler {
    public interface OnSessionListener {
        void onCompletion(Session session, int state, String reason, String sdp);

        void onStateChanged(Stream stream, StreamState streamState);
        void onStreamData(Stream stream, byte[] data);

        void onChannelOpened(Stream stream, int channel);
        void onChannelClose(Stream stream, int channel, CloseReason reason);
        boolean onChannelData(Stream stream, int channel, byte[] data);
    }

    public void setSessionListener(OnSessionListener listener) {
        mOnSessionListener = listener;
    }

    @Override
    public void onStateChanged(Stream stream, StreamState streamState) {
        super.onStateChanged(stream, streamState);
        if(mOnSessionListener != null) {
            mOnSessionListener.onStateChanged(stream, streamState);
        }

    }

    @Override
    public void onStreamData(Stream stream, byte[] data) {
        if(mOnSessionListener != null) {
            mOnSessionListener.onStreamData(stream, data);
        }
    }

    @Override
    public void onCompletion(Session session, int state, String reason, String sdp) {
        if(mOnSessionListener != null) {
            mOnSessionListener.onCompletion(session, state, reason, sdp);
        }
    }

    public void onChannelOpened(Stream stream, int channel) {
        if(mOnSessionListener != null) {
            mOnSessionListener.onChannelOpened(stream, channel);
        }
    }

    public void onChannelClose(Stream stream, int channel, CloseReason reason) {
        if(mOnSessionListener != null) {
            mOnSessionListener.onChannelClose(stream, channel, reason);
        }
    }

    public boolean onChannelData(Stream stream, int channel, byte[] data) {
        if(mOnSessionListener != null) {
            return mOnSessionListener.onChannelData(stream, channel, data);
        }

        return false;
    }

    private OnSessionListener mOnSessionListener = null;
}


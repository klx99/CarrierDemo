package org.elastos.carrier.demo.session;

import org.elastos.carrier.session.AbstractStreamHandler;
import org.elastos.carrier.session.Session;
import org.elastos.carrier.session.SessionRequestCompleteHandler;
import org.elastos.carrier.session.Stream;
import org.elastos.carrier.session.StreamState;

public class DefaultSessionHandler extends AbstractStreamHandler implements SessionRequestCompleteHandler {
    public interface OnSessionListener {
        void onStateChanged(Stream stream, StreamState streamState);
        void onCompletion(Session session, int state, String reason, String sdp);
        void onStreamData(Stream stream, byte[] data);
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

    public void setSessionListener(OnSessionListener listener) {
        mOnSessionListener = listener;
    }

    private OnSessionListener mOnSessionListener = null;
}


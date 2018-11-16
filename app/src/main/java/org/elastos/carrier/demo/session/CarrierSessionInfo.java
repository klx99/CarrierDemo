package org.elastos.carrier.demo.session;

import org.elastos.carrier.demo.Logger;
import org.elastos.carrier.session.CloseReason;
import org.elastos.carrier.session.Session;
import org.elastos.carrier.session.Stream;
import org.elastos.carrier.session.StreamState;

public class CarrierSessionInfo {
    public static class SessionState {

        public static final int SESSION_CLOSED                = 0x00010000;
        public static final int SESSION_REQUEST_COMPLETED     = 0x00020000;

        public static final int SESSION_STREAM_INITIALIZED    = 0x00000001;
        public static final int SESSION_STREAM_TRANSPORTREADY = 0x00000002;
        public static final int SESSION_STREAM_CONNECTING     = 0x00000004;
        public static final int SESSION_STREAM_CONNECTED      = 0x00000008;
        public static final int SESSION_STREAM_ERROR          = 0x00000010;

        public static final int SESSION_CHANNEL_OPENED        = 0x00000100;

        public static final int SESSION_ALL                     = SESSION_CLOSED | SESSION_REQUEST_COMPLETED
                                                                | SESSION_STREAM_INITIALIZED | SESSION_STREAM_TRANSPORTREADY
                                                                | SESSION_STREAM_CONNECTING | SESSION_STREAM_CONNECTED;

        public void maskState(int state) {
            mState |= state;
            synchronized (mLocker) {
                mLocker.notifyAll();
            }
        }

        public void unmaskState(int state) {
            int tmpState = mState;
            mState &= (~state);
            synchronized (mLocker) {
                mLocker.notifyAll();
            }
        }

        public boolean isMasked(int state) {
            return (mState & state) == state;
        }

        public boolean isUnmasked(int state) {
            return (mState & state) == 0;
        }

        public boolean equals(int state) {
            return mState == state;
        }

        public boolean waitForState(int carrierState) {
            Logger.info("Carrier wait for state : " + getStateBinary(carrierState));
            while (!isMasked(carrierState)) {
                synchronized (mLocker) {
                    if(isMasked(SESSION_CLOSED)) {
                        return false;
                    }

                    try {
                        mLocker.wait();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return true;
        }

        public boolean waitForState(int carrierState, long timeoutMs) {
            Logger.info("Carrier wait for state : " + getStateBinary(carrierState) + " timeout=" + timeoutMs);
            long fromTimeMs = System.currentTimeMillis();
            while (!isMasked(carrierState)) {
                synchronized (mLocker) {
                    long elapsedTimeMs = System.currentTimeMillis() - fromTimeMs;
                    if(elapsedTimeMs >= timeoutMs) {
                        return false;
                    }

                    if(isMasked(SESSION_CLOSED)) {
                        return false;
                    }

                    try {
                        mLocker.wait(timeoutMs - elapsedTimeMs);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return true;
        }

        public String getStateBinary(int state) {
            String strBin = String.format("0x%08X", state);

            return strBin;
        }

        private static Integer mState = 0;
        private static final Object mLocker = new Object();
    }

    public CarrierSessionInfo() {
        mSessionHandler = new DefaultSessionHandler();
        mSessionListener = new DefaultSessionHandler.OnSessionListener() {
            @Override
            public void onStateChanged(Stream stream, StreamState streamState) {
                setStreamState(stream, streamState);
            }

            @Override
            public void onCompletion(Session session, int state, String reason, String sdp) {
                mSdp = sdp;
                mSessionState.maskState(SessionState.SESSION_REQUEST_COMPLETED);
            }

            @Override
            public void onStreamData(Stream stream, byte[] data) {
                String dataStr = new String(data);

                Logger.info("Session received data on stream: " + stream
                        + "\ndata: " + dataStr
                        + "\nlen: " + data.length);

                if(dataStr.startsWith("addServer")) {
                    String[] args = dataStr.split(":");
                   CarrierSessionHelper.addServer(CarrierSessionInfo.this, args[1], args[2]);
                    Logger.info("Add server. ipaddr=" + args[1] + " port=" + args[2]);
                }
            }

            @Override
            public void onChannelOpened(Stream stream, int channel) {
                Logger.info("Carrier session stream " + stream + " channel opened: " + channel);
                mChannel = channel;
                mSessionState.maskState(SessionState.SESSION_CHANNEL_OPENED);
            }

            @Override
            public void onChannelClose(Stream stream, int channel, CloseReason reason) {
                Logger.info("Carrier session stream " + stream + " channel close: " + channel + " reason:" + reason.name());
                mChannel = -1;
                mSessionState.unmaskState(SessionState.SESSION_CHANNEL_OPENED);
            }

            @Override
            public boolean onChannelData(Stream stream, int channel, byte[] data) {
                Logger.info("Session channel received data on stream: " + stream
                        + "\ndata: " + new String(data)
                        + "\nlen: " + data.length);
                return true;
            }
        };
        mSessionHandler.setSessionListener(mSessionListener);

        mSessionState = new SessionState();
        mSession = null;
        mStream = null;
        mChannel = -1;
        mSdp = null;
    }

    public DefaultSessionHandler mSessionHandler;
    public DefaultSessionHandler.OnSessionListener mSessionListener;
    public SessionState mSessionState;
    public Session mSession;
    public Stream mStream;
    public int mPortForwarding = -1;
    public int mChannel = -1;
    public String mSdp;

    private void setStreamState(Stream stream, StreamState streamState) {
        Logger.info("Carrier session stream " + stream + " state change to: " + streamState);
        switch (streamState) {
            case Initialized:
                mSessionState.maskState(SessionState.SESSION_STREAM_INITIALIZED);
                break;
            case TransportReady:
                mSessionState.maskState(SessionState.SESSION_STREAM_TRANSPORTREADY);
                break;
            case Connecting:
                mSessionState.maskState(SessionState.SESSION_STREAM_CONNECTING);
                break;
            case Connected:
                mSessionState.maskState(SessionState.SESSION_STREAM_CONNECTED);
                break;
            case Deactivated:
                mSessionState.unmaskState(SessionState.SESSION_STREAM_CONNECTED);
                break;
            case Closed:
                mSessionState.unmaskState(SessionState.SESSION_STREAM_CONNECTED);
                mSessionState.unmaskState(SessionState.SESSION_STREAM_TRANSPORTREADY);
                break;
            case Error:
                mSessionState.maskState(SessionState.SESSION_STREAM_ERROR);
                break;
        }
    }
}


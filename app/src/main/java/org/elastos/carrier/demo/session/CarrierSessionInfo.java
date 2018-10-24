package org.elastos.carrier.demo.session;

import org.elastos.carrier.demo.Logger;
import org.elastos.carrier.session.Session;
import org.elastos.carrier.session.Stream;
import org.elastos.carrier.session.StreamState;

public class CarrierSessionInfo {
    public static class SessionState {
        public static final int SESSION_STREAM_0_INITIALIZED    = 0x00000001;
        public static final int SESSION_STREAM_1_INITIALIZED    = 0x00000002;
        public static final int SESSION_STREAM_0_TRANSPORTREADY = 0x00000004;
        public static final int SESSION_STREAM_1_TRANSPORTREADY = 0x00000008;
        public static final int SESSION_STREAM_0_CONNECTING     = 0x00000010;
        public static final int SESSION_STREAM_1_CONNECTING     = 0x00000020;
        public static final int SESSION_STREAM_0_CONNECTED      = 0x00000040;
        public static final int SESSION_STREAM_1_CONNECTED      = 0x00000080;
        public static final int SESSION_STREAM_0_ERROR          = 0x00000100;
        public static final int SESSION_STREAM_1_ERROR          = 0x00000200;

        public static final int SESSION_CLOSED                  = 0x00010000;
        public static final int SESSION_REQUEST_COMPLETED       = 0x00020000;
        public static final int SESSION_STREAM_INITIALIZED      = SESSION_STREAM_0_INITIALIZED | SESSION_STREAM_1_INITIALIZED;
        public static final int SESSION_STREAM_TRANSPORTREADY   = SESSION_STREAM_0_TRANSPORTREADY | SESSION_STREAM_1_TRANSPORTREADY;
        public static final int SESSION_STREAM_CONNECTING       = SESSION_STREAM_0_CONNECTING | SESSION_STREAM_1_CONNECTING;
        public static final int SESSION_STREAM_CONNECTED        = SESSION_STREAM_0_CONNECTED | SESSION_STREAM_1_CONNECTED;
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
                Logger.info("Session received data to stream: " + stream
                        + "\ndata: " + new String(data)
                        + "\nlen: " + data.length);
            }
        };
        mSessionHandler.setSessionListener(mSessionListener);

        mSessionState = new SessionState();
        mSession = null;
        mStream0 = null;
        mStream1 = null;
        mSdp = null;
    }

    public DefaultSessionHandler mSessionHandler;
    public DefaultSessionHandler.OnSessionListener mSessionListener;
    public SessionState mSessionState;
    public Session mSession;
    public Stream mStream0;
    public Stream mStream1;
    public String mSdp;

    private void setStreamState(Stream stream, StreamState streamState) {
        Logger.info("Carrier session stream " + stream + " state change to: " + streamState);
        switch (streamState) {
            case Initialized:
                if (stream == mStream0) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_0_INITIALIZED);
                } else if (stream == mStream1) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_1_INITIALIZED);
                }
                break;
            case TransportReady:
                if (stream == mStream0) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_0_TRANSPORTREADY);
                } else if (stream == mStream1) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_1_TRANSPORTREADY);
                }
                break;
            case Connecting:
                if (stream == mStream0) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_0_CONNECTING);
                } else if (stream == mStream1) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_1_CONNECTING);
                }
                break;
            case Connected:
                if (stream == mStream0) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_0_CONNECTED);
                } else if (stream == mStream1) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_1_CONNECTED);
                }
                break;
            case Deactivated:
                if (stream == mStream0) {
                    mSessionState.unmaskState(SessionState.SESSION_STREAM_0_CONNECTED);
                } else if (stream == mStream1) {
                    mSessionState.unmaskState(SessionState.SESSION_STREAM_1_CONNECTED);
                }
                break;
            case Closed:
                if (stream == mStream0) {
                    mSessionState.unmaskState(SessionState.SESSION_STREAM_0_CONNECTED);
                    mSessionState.unmaskState(SessionState.SESSION_STREAM_0_TRANSPORTREADY);
                } else if (stream == mStream1) {
                    mSessionState.unmaskState(SessionState.SESSION_STREAM_1_CONNECTED);
                    mSessionState.unmaskState(SessionState.SESSION_STREAM_1_TRANSPORTREADY);
                }
                break;
            case Error:
                if (stream == mStream0) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_0_ERROR);
                } else if (stream == mStream1) {
                    mSessionState.maskState(SessionState.SESSION_STREAM_1_ERROR);
                }
                break;
        }
    }
}


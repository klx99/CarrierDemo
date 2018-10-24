package org.elastos.carrier.demo.session;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.demo.Logger;
import org.elastos.carrier.exceptions.ElastosException;
import org.elastos.carrier.session.Manager;
import org.elastos.carrier.session.ManagerHandler;
import org.elastos.carrier.session.Stream;
import org.elastos.carrier.session.StreamType;

public final class CarrierSessionHelper {
    private CarrierSessionHelper() {}

    public static void initSessionManager(ManagerHandler handler) {
        try {
            Manager manager = Manager.getInstance();
            if(manager != null) {
                return;
            }

            Manager.getInstance(Carrier.getInstance(), handler);
            Logger.info("Session manager initialized.");
        } catch (Exception e) {
            Logger.error("Failed to init session manager.", e);
        }
    }

    public static void cleanupSessionManager() {
        try {
            Manager manager = Manager.getInstance();
            if(manager == null) {
                return;
            }

            manager.cleanup();
            Logger.info("Session manager cleanup.");
        } catch (Exception e) {
            Logger.error("Failed to cleanup session manager.", e);
        }
    }

    public static CarrierSessionInfo newSessionAndStream(String peer) {
        CarrierSessionInfo sessionInfo = null;

        try {
            sessionInfo = new CarrierSessionInfo();

            Logger.info("Carrier new session. peer:" + peer);
            Manager carrierSessionManager = Manager.getInstance();
            if (carrierSessionManager == null) {
                Logger.error("Failed to new session, manager not initialized.");
                return null;
            }
            sessionInfo.mSession = carrierSessionManager.newSession(peer);

            Logger.info("Carrier add a unreliable stream to session.");
            int dataOptions = 0;
            sessionInfo.mStream0 = sessionInfo.mSession.addStream(StreamType.Application, dataOptions, sessionInfo.mSessionHandler);

            Logger.info("Carrier add a reliable stream to session.");
            dataOptions = Stream.PROPERTY_RELIABLE;
            sessionInfo.mStream1 = sessionInfo.mSession.addStream(StreamType.Text, dataOptions, sessionInfo.mSessionHandler);
        } catch (Exception e) {
            Logger.error("Failed to new session or stream.", e);
        }

        return sessionInfo;
    }

    public static void requestSession(CarrierSessionInfo sessionInfo) {
        try {
            sessionInfo.mSession.request(sessionInfo.mSessionHandler);
        } catch (Exception e) {
            Logger.error("Failed to request session or stream.", e);
        }
    }

    public static void replyRequest(CarrierSessionInfo sessionInfo) {
        try {
            sessionInfo.mSession.replyRequest(0, null);
        } catch (Exception e) {
            Logger.error("Failed to request session or stream.", e);
        }
    }

    public static void startSession(CarrierSessionInfo sessionInfo) {
        try {
            sessionInfo.mSession.start(sessionInfo.mSdp);
        } catch (Exception e) {
            Logger.error("Failed to start session or stream.", e);
        }
    }

    public static void closeSession(CarrierSessionInfo sessionInfo) {
        try {
            sessionInfo.mSession.close();

            sessionInfo.mSessionState.maskState(CarrierSessionInfo.SessionState.SESSION_CLOSED);
            sessionInfo.mSession = null;
            sessionInfo.mStream0 = null;
            sessionInfo.mStream1 = null;
            sessionInfo.mSdp = null;
        } catch (Exception e) {
            Logger.error("Failed to close session or stream.", e);
        }
    }

    public static int sendData(Stream stream, byte[] data) {
        int sent = -1;
        try {
            sent = stream.writeData(data);
            Logger.info("Session send data to stream: " + stream
                    + "\ndata: " + new String(data)
                    + "\nsent: " + sent);
        } catch (Exception e) {
            Logger.error("Failed to send session data.", e);
        }

        return sent;
    }
}


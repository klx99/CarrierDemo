package org.elastos.carrier.demo;

import android.content.Context;

import org.elastos.did.DID;
import org.elastos.did.DIDBackend;
import org.elastos.did.DIDDocument;
import org.elastos.did.DIDStore;
import org.elastos.did.DIDURL;
import org.elastos.did.Issuer;
import org.elastos.did.VerifiableCredential;
import org.elastos.did.VerifiablePresentation;
import org.elastos.did.exception.DIDException;
import org.elastos.did.jwt.Claims;
import org.elastos.did.jwt.Header;
import org.elastos.did.jwt.Jws;
import org.elastos.did.jwt.JwtParser;
import org.elastos.did.jwt.JwtParserBuilder;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class StandardAuth {
    public static void Init(Context context) {
        StandardAuth.context = context;
        try {
            initPrivateIdentity();
        } catch (DIDException e) {
            Logger.error("Failed to init StandardAuth.", e);
        }
    }

    public static void SetChallenge(String challenge) {
        StandardAuth.challenge = challenge;
    }

    public static void SetAccessToken(String token) {
        StandardAuth.accessToken = token;
    }

    public static String GetAccessToken() {
        return StandardAuth.accessToken;
    }

    static String makeDoc() {
        String instanceDoc = null;
        try {
            instanceDoc = userStore.loadDid(instanceDid).toString();
        } catch (Exception e) {
            Logger.error("Failed make jwt.", e);
        }

        return  instanceDoc;
    }

    static String makeVP() {
        String token = "";

        try {
            JwtParser jp = new JwtParserBuilder().build();
            Claims jws = jp.parseClaimsJws(challenge).getBody();;
            String feedsdDid = jws.getIssuer();
            Logger.info("Feedsd Did: " + feedsdDid);
            String feedsdNonce = jws.get("nonce", String.class);
            Logger.info("Feedsd Nonce: " + feedsdNonce);
            String aud = jws.getAudience();
            if(instanceDid.equals(aud) == false) {
                Logger.error("Failed to check challenge audience.");
                return "";
            }
            Logger.info("Audience: " + aud);

            // issue_auth
            {
                DIDDocument userDoc = userStore.loadDid(userDid);
                Issuer userIssuer = new Issuer(userDoc);

                HashMap<String, String> subject= new HashMap<>();
                subject.put("appDid", appDid.toString());
                subject.put("name", "TestName");
//                subject.put("email", "test@email.com");

                Issuer.CredentialBuilder icb = userIssuer.issueFor(instanceDid);
                VerifiableCredential vc = icb.id("didsdk")
                        .type("AppIdCredential")
                        .properties(subject)
                        .expirationDate(userDoc.getExpires())
                        .seal(storePassword);

                VerifiablePresentation.Builder vpb = VerifiablePresentation.createFor(instanceDid, userStore);
                VerifiablePresentation vp = vpb.credentials(vc)
                        .realm(feedsdDid)
                        .nonce(feedsdNonce)
                        .seal(storePassword);
                Logger.info("VerifiablePresentation: " + vp);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.MILLISECOND, 0);
                cal.add(Calendar.SECOND, 60 * 5);
                Date exp = cal.getTime();
                DIDDocument instanceDoc = userStore.loadDid(instanceDid);
                token = instanceDoc.jwtBuilder()
                        .addHeader(Header.TYPE, Header.JWT_TYPE)
                        .addHeader("version", "1.0")
                        .setAudience(feedsdDid)
                        .setExpiration(exp)
                        .claimWithJson("presentation", vp.toString())
                        .sign(storePassword)
                        .compact();
            }

        } catch (Exception e) {
            Logger.error("Failed make jwt.", e);
        }
        Logger.info("jwt: " + token);

        return token;
    }

    private static void initPrivateIdentity() throws DIDException {
        final String cacheDir = context.getFilesDir().getAbsolutePath() + "/did";
        final String storePath = cacheDir + "/store";

        DIDBackend.initialize("http://api.elastos.io:20606", cacheDir);

        userStore = DIDStore.open("filesystem", storePath, (payload, memo) -> {
            Logger.info("Create ID transaction with:");
            Logger.info("  Payload = " + payload);
        });

        if (userStore.containsPrivateIdentity() == false) {
            userStore.initPrivateIdentity(null, mnemonic, null, storePassword);
            userStore.synchronize(storePassword);
        }

        userDid = userStore.getDid(0);
        if(userStore.containsDid(userDid) == false) {
            userStore.newDid(0, storePassword);
        }
        appDid = userStore.getDid(1);
        if(userStore.containsDid(appDid) == false) {
            userStore.newDid(1, storePassword);
        }
        instanceDid = userStore.getDid(2);
        if(userStore.containsDid(instanceDid) == false) {
            userStore.newDid(2, storePassword);
        }
        Logger.info("AppDID:" + appDid);
        Logger.info("UserDID:" + userDid);
        Logger.info("InstanceDID:" + instanceDid);
    }

    private static Context context;
    private static DIDStore userStore;
    private static DID appDid;
    private static DID userDid;
    private static DID instanceDid;
    private static String challenge = null;
    private static String accessToken = "invalid-token";
    private final static String mnemonic = "voice kingdom wall sword pair unusual artefact opera keen aware stay game";
    private final static String storePassword = "0";
}

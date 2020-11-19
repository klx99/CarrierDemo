package org.elastos.carrier.demo;

import android.content.Context;

import org.elastos.did.DID;
import org.elastos.did.DIDBackend;
import org.elastos.did.DIDDocument;
import org.elastos.did.DIDStore;
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
import java.util.List;

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
            instanceDoc = instanceStore.loadDid(instanceDid).toString();
        } catch (Exception e) {
            Logger.error("Failed make jwt.", e);
        }

        return  instanceDoc;
    }

    static String makeJwt() {
        String token = "";

        try {
            JwtParser jp = new JwtParserBuilder().build();
            Claims jws = jp.parseClaimsJws(challenge).getBody();;
            String feedsdDid = jws.getIssuer();
            Logger.info("Feedsd Did: " + feedsdDid);
            String feedsdNonce = jws.get("nonce", String.class);
            Logger.info("Feedsd Nonce: " + feedsdNonce);
            String feedsAud = jws.getAudience();
            Logger.info("Feedsd Audience: " + feedsAud);

            // issue_auth
            {
                DIDDocument instanceDoc = instanceStore.loadDid(instanceDid);
                Issuer instanceIssuer = new Issuer(instanceDoc);

                HashMap<String, String> subject= new HashMap<>();
                subject.put("appDid", "did:elastos:i000000000000000000000000000000000");

                Issuer.CredentialBuilder icb = instanceIssuer.issueFor(instanceDid);
                VerifiableCredential vc = icb.id("didapp")
                        .type("AppIdCredential")
                        .properties(subject)
                        .expirationDate(instanceDoc.getExpires())
                        .seal(storePassword);


                VerifiablePresentation.Builder vpb = VerifiablePresentation.createFor(instanceDid, instanceStore);
                VerifiablePresentation vp = vpb.credentials(vc)
                        .realm(instanceDid.toString())
                        .nonce(feedsdNonce)
                        .seal(storePassword);
                Logger.info("VerifiablePresentation: " + vp);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.MILLISECOND, 0);
                Date iat = cal.getTime();
                Date nbf = cal.getTime();
                cal.add(Calendar.SECOND, 60);
                Date exp = cal.getTime();
                token = instanceDoc.jwtBuilder()
                        .addHeader(Header.TYPE, Header.JWT_TYPE)
                        .addHeader("version", "1.0")
                        .setAudience(feedsdDid)
                        .setIssuedAt(iat)
                        .setNotBefore(nbf)
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

        instanceStore = DIDStore.open("filesystem", storePath, (payload, memo) -> {
            Logger.info("Create ID transaction with:");
            Logger.info("  Payload = " + payload);
        });

        if (instanceStore.containsPrivateIdentity() == false) {
            instanceStore.initPrivateIdentity(null, mnemonic, null, storePassword);
            instanceStore.synchronize(storePassword);
        }
        List<DID> dids = instanceStore.listDids(DIDStore.DID_HAS_PRIVATEKEY);
        if (dids.size() > 0) {
            for (DID did : dids) {
                Logger.info("did:" + did);
                Logger.info("diddoc:" + instanceStore.loadDid(did));
            }
            instanceDid = dids.get(0);
        } else {
            Logger.info("No dids restored.");
        }
    }

    private static Context context;
    private static DIDStore instanceStore;
    private static DID instanceDid;
    private static String challenge = null;
    private static String accessToken = "invalid-token";
    private final static String mnemonic = "voice kingdom wall sword pair unusual artefact opera keen aware stay game";
    private final static String storePassword = "0";
}

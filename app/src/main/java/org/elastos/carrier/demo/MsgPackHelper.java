package org.elastos.carrier.demo;

import org.elastos.did.DIDDocument;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.util.HashMap;
import java.util.Map;

public class MsgPackHelper {
    public static byte[] PackData(RPC.Request req) {
        byte[] data = null;

        if(req instanceof RPC.SetBinaryRequest) {
            data = PackData((RPC.SetBinaryRequest) req);
            responseMap.put(req.id, new RPC.SetBinaryResponse());
        } else if(req instanceof RPC.GetBinaryRequest) {
            data = PackData((RPC.GetBinaryRequest) req);
            responseMap.put(req.id, new RPC.GetBinaryResponse());
        } else if(req instanceof RPC.GetVersionRequest) {
            data = PackData((RPC.GetVersionRequest) req);
            responseMap.put(req.id, new RPC.GetVersionResponse());
        } else if(req instanceof RPC.DeclarePostRequest) {
            data = PackData((RPC.DeclarePostRequest) req);
            responseMap.put(req.id, new RPC.DeclarePostResponse());
        } else if(req instanceof RPC.NotifyPostRequest) {
            data = PackData((RPC.NotifyPostRequest) req);
            responseMap.put(req.id, new RPC.NotifyPostResponse());
        } else if(req instanceof RPC.ReportIllegalCommentRequest) {
            data = PackData((RPC.ReportIllegalCommentRequest) req);
            responseMap.put(req.id, new RPC.ReportIllegalCommentResponse());
        } else if(req instanceof RPC.SignInRequest) {
            data = PackData((RPC.SignInRequest) req);
            responseMap.put(req.id, new RPC.SignInResponse());
        } else if(req instanceof RPC.DidAuthRequest) {
            data = PackData((RPC.DidAuthRequest) req);
            responseMap.put(req.id, new RPC.DidAuthResponse());
        }

        return data;
    }

    public static RPC.Response UnpackData(byte[] data) {
        RPC.Response resp = null;
        Map<Value, Value> map = null;
        try {
            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
            map = unpacker.unpackValue().asMapValue().map();
            long id = map.get(ValueFactory.newString("id")).asIntegerValue().asLong();
            resp = responseMap.remove(id);

            Value error = map.get(ValueFactory.newString("error"));
            if(error != null) {
                resp = new RPC.ErrorResponse();
            }

            resp.id = id;

            unpacker.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(resp instanceof RPC.ErrorResponse) {
            UnpackData(map, (RPC.ErrorResponse) resp);
        } else if(resp instanceof RPC.SetBinaryResponse) {
            UnpackData(map, (RPC.SetBinaryResponse) resp);
        } else if(resp instanceof RPC.GetBinaryResponse) {
            UnpackData(map, (RPC.GetBinaryResponse) resp);
        } else if(resp instanceof RPC.GetVersionResponse) {
            UnpackData(map, (RPC.GetVersionResponse) resp);
        } else if(resp instanceof RPC.DeclarePostResponse) {
            UnpackData(map, (RPC.DeclarePostResponse) resp);
        } else if(resp instanceof RPC.NotifyPostResponse) {
            UnpackData(map, (RPC.NotifyPostResponse) resp);
        } else if(resp instanceof RPC.ReportIllegalCommentResponse) {
            UnpackData(map, (RPC.ReportIllegalCommentResponse) resp);
        } else if(resp instanceof RPC.SignInResponse) {
            UnpackData(map, (RPC.SignInResponse) resp);
        } else if(resp instanceof RPC.DidAuthResponse) {
            UnpackData(map, (RPC.DidAuthResponse) resp);
        }

        return resp;
    }

    private static MessageBufferPacker MakeMsgPackerWithToken(RPC.Request req, int paramsExtCnt) {
        return MakeMsgPacker(req, paramsExtCnt, true);
    }

    private static MessageBufferPacker MakeMsgPacker(RPC.Request req, int paramsExtCnt, boolean withToken) {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packMapHeader(4)
                .packString("version").packString(req.version)
                .packString("method").packString(req.getMethod())
                .packString("id").packLong(req.id)
                .packString("params").packMapHeader(withToken ? paramsExtCnt + 1 : paramsExtCnt);
            if(withToken) {
                packer.packString("access_token").packString(req.params.accessToken);
            }
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer;
    }

    private static void UnpackData(Map<Value, Value> map, RPC.ErrorResponse resp) {
        Map<Value, Value> error = map.get(ValueFactory.newString("error")).asMapValue().map();
        resp.error.code = error.get(ValueFactory.newString("code")).asIntegerValue().asInt();;
        resp.error.message = error.get(ValueFactory.newString("message")).asStringValue().asString();
    }

    private static byte[] PackData(RPC.SetBinaryRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 4);
        try {
            int contentSize = req.params.content != null ? req.params.content.length : 0;
            packer.packString("key").packString(req.params.key)
                  .packString("algo").packString(req.params.algo)
                  .packString("checksum").packString(req.params.checksum)
                  .packString("content").packBinaryHeader(contentSize).addPayload(req.params.content);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.SetBinaryResponse resp) {
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.key = result.get(ValueFactory.newString("key")).asStringValue().asString();
    }

    private static byte[] PackData(RPC.GetBinaryRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 1);
        try {
            packer.packString("key").packString(req.params.key);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.GetBinaryResponse resp) {
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.key = result.get(ValueFactory.newString("key")).asStringValue().asString();
        resp.result.algo = result.get(ValueFactory.newString("algo")).asStringValue().asString();
        resp.result.checksum = result.get(ValueFactory.newString("checksum")).asStringValue().asString();
        resp.result.content = result.get(ValueFactory.newString("content")).asBinaryValue().asByteArray();
    }

    private static byte[] PackData(RPC.GetVersionRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 0);
        try {
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.GetVersionResponse resp) {
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.version = result.get(ValueFactory.newString("version")).asStringValue().asString();
    }

    private static byte[] PackData(RPC.DeclarePostRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 3);
        try {
            packer.packString("channel_id").packLong(req.params.channelId);
            packer.packString("content").packBinaryHeader(req.params.content.length).addPayload(req.params.content);
            packer.packString("with_notify").packBoolean(req.params.with_notify);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.DeclarePostResponse resp) {
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.postId = result.get(ValueFactory.newString("id")).asIntegerValue().asInt();
    }

    private static byte[] PackData(RPC.NotifyPostRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 2);
        try {
            packer.packString("channel_id").packLong(req.params.channelId);
            packer.packString("post_id").packLong(req.params.postId);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.NotifyPostResponse resp) {
        // do nothing
    }


    private static void UnpackData(Map<Value, Value> map, RPC.ReportIllegalCommentResponse resp) {
        // do nothing
    }

    private static byte[] PackData(RPC.ReportIllegalCommentRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 4);
        try {
            packer.packString("channel_id").packLong(req.params.channelId)
                  .packString("post_id").packLong(req.params.postId)
                  .packString("comment_id").packLong(req.params.commentId)
                  .packString("reasons").packString(req.params.reasons);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

//    private void blockOrUnblockComment(boolean blockOrUnblock) {
//        if(CarrierHelper.getPeerUserId() == null) {
//            showError("Friend is not online.");
//            return;
//        }
//
//        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
//        try {
//            String method = blockOrUnblock ? "block_comment" : "unblock_comment";
//            packer.packMapHeader(4)
//                    .packString("version").packString("1.0")
//                    .packString("method").packString(method)
//                    .packString("id").packInt(12345)
//                    .packString("params").packMapHeader(4)
//                    .packString("access_token").packString("access-token-test")
//                    .packString("channel_id").packLong(1)
//                    .packString("post_id").packLong(2)
//                    .packString("comment_id").packLong(1);
//            packer.close(); // Never forget to close (or flush) the buffer
//        } catch (Exception e) {
//            e.printStackTrace();
//            assert(false);
//        }
//
//        CarrierHelper.sendMessage(packer.toByteArray());
//    }
//
//    private void getReportedComments() {
//        if(CarrierHelper.getPeerUserId() == null) {
//            showError("Friend is not online.");
//            return;
//        }
//
//        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
//        try {
//            packer.packMapHeader(4)
//                    .packString("version").packString("1.0")
//                    .packString("method").packString("get_reported_comments")
//                    .packString("id").packInt(12345)
//                    .packString("params").packMapHeader(5)
//                    .packString("access_token").packString("access-token-test")
//                    .packString("by").packLong(3)
//                    .packString("upper_bound").packLong(0)
//                    .packString("lower_bound").packLong(0)
//                    .packString("max_count").packLong(0);
//            packer.close(); // Never forget to close (or flush) the buffer
//        } catch (Exception e) {
//            e.printStackTrace();
//            assert(false);
//        }
//
//        CarrierHelper.sendMessage(packer.toByteArray());
//    }

    private static byte[] PackData(RPC.SignInRequest req) {
        MessageBufferPacker packer = MakeMsgPacker(req, 1, false);
        try {
            packer.packString("document").packString(req.params.document);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.SignInResponse resp) {
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.challenge = result.get(ValueFactory.newString("challenge")).asStringValue().asString();
        StandardAuth.SetChallenge(resp.result.challenge);
    }

    private static byte[] PackData(RPC.DidAuthRequest req) {
        MessageBufferPacker packer = MakeMsgPacker(req, 1, false);
        try {
            packer.packString("vp").packString(req.params.vp);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.DidAuthResponse resp) {
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.accessToken = result.get(ValueFactory.newString("access_token")).asStringValue().asString();
        StandardAuth.SetAccessToken(resp.result.accessToken);
    }

    private MsgPackHelper() {}
    private static Map<Long, RPC.Response> responseMap = new HashMap<>();
}

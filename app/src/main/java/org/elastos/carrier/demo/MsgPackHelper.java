package org.elastos.carrier.demo;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.io.IOException;
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
        } else if(req instanceof RPC.ReportIllegalCommentRequest) {
            data = PackData((RPC.ReportIllegalCommentRequest) req);
            responseMap.put(req.id, new RPC.ReportIllegalCommentResponse());
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
            resp.id = id;

            unpacker.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(resp instanceof RPC.SetBinaryResponse) {
//            data = PackData((RPC.SetBinaryResponse) resp);
        } else if(resp instanceof RPC.GetBinaryResponse) {
//            data = PackData((RPC.GetBinaryResponse) resp);
        } else if(resp instanceof RPC.GetVersionResponse) {
            UnpackData(map, (RPC.GetVersionResponse) resp);
        } else if(resp instanceof RPC.ReportIllegalCommentResponse) {
            UnpackData(map, (RPC.ReportIllegalCommentResponse) resp);
        }

        return resp;
    }

    private static MessageBufferPacker MakeMsgPacker(RPC.Request req, int paramsExtCnt) {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packMapHeader(4)
                .packString("version").packString(req.version)
                .packString("method").packString(req.getMethod())
                .packString("id").packLong(req.id)
                .packString("params").packMapHeader(paramsExtCnt + 1)
                    .packString("access_token").packString(req.params.accessToken);
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        return packer;
    }

    private static byte[] PackData(RPC.SetBinaryRequest req) {
        MessageBufferPacker packer = MakeMsgPacker(req, 3);
        try {
            packer.packString("key").packString(req.params.key)
                  .packString("algo").packString(req.params.algo)
                  .packString("checksum").packString(req.params.checksum);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static byte[] PackData(RPC.GetBinaryRequest req) {
        MessageBufferPacker packer = MakeMsgPacker(req, 1);
        try {
            packer.packString("key").packString(req.params.key);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static byte[] PackData(RPC.GetVersionRequest req) {
        MessageBufferPacker packer = MakeMsgPacker(req, 0);
        try {
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.GetVersionResponse resp) {
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.version = result.get(ValueFactory.newString("version")).asStringValue().asString();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.ReportIllegalCommentResponse resp) {
        // do nothing
    }

    private static byte[] PackData(RPC.ReportIllegalCommentRequest req) {
        MessageBufferPacker packer = MakeMsgPacker(req, 4);
        try {
            packer.packString("channel_id").packLong(req.params.channelId)
                  .packString("post_id").packLong(req.params.postId)
                  .packString("comment_id").packLong(req.params.commentId)
                  .packString("reasons").packString(req.params.reasons);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (IOException e) {
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
//        } catch (IOException e) {
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
//        } catch (IOException e) {
//            e.printStackTrace();
//            assert(false);
//        }
//
//        CarrierHelper.sendMessage(packer.toByteArray());
//    }

    private MsgPackHelper() {}
    private static Map<Long, RPC.Response> responseMap = new HashMap<>();
}

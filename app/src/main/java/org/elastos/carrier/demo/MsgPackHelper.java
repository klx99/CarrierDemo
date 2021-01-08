package org.elastos.carrier.demo;

import org.elastos.did.DIDDocument;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ArrayValue;
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
        } else if(req instanceof RPC.DownloadNewServiceRequest) {
            data = PackData((RPC.DownloadNewServiceRequest) req);
            responseMap.put(req.id, new RPC.DownloadNewServiceResponse());
        } else if(req instanceof RPC.StartNewServiceRequest) {
            data = PackData((RPC.StartNewServiceRequest) req);
            responseMap.put(req.id, new RPC.StartNewServiceResponse());
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
        } else if(req instanceof RPC.EnableNotifyRequest) {
            data = PackData((RPC.EnableNotifyRequest) req);
            responseMap.put(req.id, new RPC.EnableNotifyResponse());
        } else if(req instanceof RPC.GetMultiCommentsRequest) {
            data = PackData((RPC.GetMultiCommentsRequest) req);
            responseMap.put(req.id, new RPC.GetMultiCommentsResponse());
        } else if(req instanceof RPC.GetMultiLikesAndCommentsCountRequest) {
            data = PackData((RPC.GetMultiLikesAndCommentsCountRequest) req);
            responseMap.put(req.id, new RPC.GetMultiLikesAndCommentsCountResponse());
        } else if(req instanceof RPC.GetMultiSubscribersCountRequest) {
            data = PackData((RPC.GetMultiSubscribersCountRequest) req);
            responseMap.put(req.id, new RPC.GetMultiSubscribersCountResponse());
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
        } else if(resp instanceof RPC.DownloadNewServiceResponse) {
            UnpackData(map, (RPC.DownloadNewServiceResponse) resp);
        } else if(resp instanceof RPC.StartNewServiceResponse) {
            UnpackData(map, (RPC.StartNewServiceResponse) resp);
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
        } else if(resp instanceof RPC.EnableNotifyResponse) {
            UnpackData(map, (RPC.EnableNotifyResponse) resp);
        } else if(resp instanceof RPC.GetMultiCommentsResponse) {
            UnpackData(map, (RPC.GetMultiCommentsResponse) resp);
        } else if(resp instanceof RPC.GetMultiLikesAndCommentsCountResponse) {
            UnpackData(map, (RPC.GetMultiLikesAndCommentsCountResponse) resp);
        } else if(resp instanceof RPC.GetMultiSubscribersCountResponse) {
            UnpackData(map, (RPC.GetMultiSubscribersCountResponse) resp);
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
        resp.result.versionCode = result.get(ValueFactory.newString("version_code")).asIntegerValue().asLong();
    }

    private static byte[] PackData(RPC.DownloadNewServiceRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 6);
        try {
            packer.packString("new_version").packString(req.params.new_version);
            packer.packString("base_url").packString(req.params.base_url);
            packer.packString("macosx").packMapHeader(3)
                    .packString("name").packString(req.params.macosx.name)
                    .packString("size").packLong(req.params.macosx.size)
                    .packString("md5").packString(req.params.macosx.md5);
            packer.packString("ubuntu_1804").packMapHeader(3)
                    .packString("name").packString(req.params.ubuntu_1804.name)
                    .packString("size").packLong(req.params.ubuntu_1804.size)
                    .packString("md5").packString(req.params.ubuntu_1804.md5);
            packer.packString("ubuntu_2004").packMapHeader(3)
                    .packString("name").packString(req.params.ubuntu_2004.name)
                    .packString("size").packLong(req.params.ubuntu_2004.size)
                    .packString("md5").packString(req.params.ubuntu_2004.md5);
            packer.packString("raspbian").packMapHeader(3)
                    .packString("name").packString(req.params.raspbian.name)
                    .packString("size").packLong(req.params.raspbian.size)
                    .packString("md5").packString(req.params.raspbian.md5);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.DownloadNewServiceResponse resp) {
        // do nothing
    }

    private static byte[] PackData(RPC.StartNewServiceRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 1);
        try {
            packer.packString("new_version").packString(req.params.new_version);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.StartNewServiceResponse resp) {
        // do nothing
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
        resp.result.jwtChallenge = result.get(ValueFactory.newString("jwt_challenge")).asStringValue().asString();
        StandardAuth.SetChallenge(resp.result.jwtChallenge);
    }

    private static byte[] PackData(RPC.DidAuthRequest req) {
        MessageBufferPacker packer = MakeMsgPacker(req, 2, false);
        try {
            packer.packString("user_name").packString(req.params.userName);
            packer.packString("jwt_vp").packString(req.params.jwtVP);
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

    private static byte[] PackData(RPC.EnableNotifyRequest req) {
        MessageBufferPacker packer = MakeMsgPacker(req, 0, true);
        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.EnableNotifyResponse resp) {
        // do nothing
    }

    private static byte[] PackData(RPC.GetMultiCommentsRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 6);
        try {
            packer.packString("channel_id").packLong(req.params.channel_id);
            packer.packString("post_id").packLong(req.params.post_id);
            packer.packString("by").packLong(req.params.by);
            packer.packString("upper_bound").packLong(req.params.upper_bound);
            packer.packString("lower_bound").packLong(req.params.lower_bound);
            packer.packString("max_count").packLong(req.params.max_count);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.GetMultiCommentsResponse resp) {
        Value aa = map.get(ValueFactory.newString("result"));
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.is_last = result.get(ValueFactory.newString("is_last")).asBooleanValue().getBoolean();
        ArrayValue comments = result.get(ValueFactory.newString("comments")).asArrayValue();
        resp.result.comments = new RPC.GetMultiCommentsResponse.ExtResult.Comment[comments.size()];
        for(int idx = 0; idx < comments.size(); idx++) {
            Map<Value, Value> comment = comments.get(idx).asMapValue().map();;
            resp.result.comments[idx] = new RPC.GetMultiCommentsResponse.ExtResult.Comment();
            resp.result.comments[idx].channel_id = comment.get(ValueFactory.newString("channel_id")).asIntegerValue().asLong();
            resp.result.comments[idx].post_id = comment.get(ValueFactory.newString("post_id")).asIntegerValue().asLong();
            resp.result.comments[idx].comment_id = comment.get(ValueFactory.newString("comment_id")).asIntegerValue().asLong();
            resp.result.comments[idx].refer_comment_id = comment.get(ValueFactory.newString("refer_comment_id")).asIntegerValue().asLong();
            resp.result.comments[idx].status = comment.get(ValueFactory.newString("status")).asIntegerValue().asLong();
            resp.result.comments[idx].user_did = comment.get(ValueFactory.newString("user_did")).asStringValue().asString();
            resp.result.comments[idx].user_name = comment.get(ValueFactory.newString("user_name")).asStringValue().asString();
            resp.result.comments[idx].content = comment.get(ValueFactory.newString("content")).asBinaryValue().asByteArray();
            resp.result.comments[idx].likes = comment.get(ValueFactory.newString("likes")).asIntegerValue().asLong();
            resp.result.comments[idx].created_at = comment.get(ValueFactory.newString("created_at")).asIntegerValue().asLong();
            resp.result.comments[idx].updated_at = comment.get(ValueFactory.newString("updated_at")).asIntegerValue().asLong();
        }
    }

    private static byte[] PackData(RPC.GetMultiLikesAndCommentsCountRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 6);
        try {
            packer.packString("channel_id").packLong(req.params.channel_id);
            packer.packString("post_id").packLong(req.params.post_id);
            packer.packString("by").packLong(req.params.by);
            packer.packString("upper_bound").packLong(req.params.upper_bound);
            packer.packString("lower_bound").packLong(req.params.lower_bound);
            packer.packString("max_count").packLong(req.params.max_count);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.GetMultiLikesAndCommentsCountResponse resp) {
        Value aa = map.get(ValueFactory.newString("result"));
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.is_last = result.get(ValueFactory.newString("is_last")).asBooleanValue().getBoolean();
        ArrayValue posts = result.get(ValueFactory.newString("posts")).asArrayValue();
        resp.result.posts = new RPC.GetMultiLikesAndCommentsCountResponse.ExtResult.Post[posts.size()];
        for(int idx = 0; idx < posts.size(); idx++) {
            Map<Value, Value> post = posts.get(idx).asMapValue().map();;
            resp.result.posts[idx] = new RPC.GetMultiLikesAndCommentsCountResponse.ExtResult.Post();
            resp.result.posts[idx].channel_id = post.get(ValueFactory.newString("channel_id")).asIntegerValue().asLong();
            resp.result.posts[idx].post_id = post.get(ValueFactory.newString("post_id")).asIntegerValue().asLong();
            resp.result.posts[idx].comments_count = post.get(ValueFactory.newString("comments_count")).asIntegerValue().asLong();
            resp.result.posts[idx].likes_count = post.get(ValueFactory.newString("likes_count")).asIntegerValue().asLong();
        }
    }

    private static byte[] PackData(RPC.GetMultiSubscribersCountRequest req) {
        MessageBufferPacker packer = MakeMsgPackerWithToken(req, 1);
        try {
            packer.packString("channel_id").packLong(req.params.channel_id);
            packer.close(); // Never forget to close (or flush) the buffer
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }

        return packer.toByteArray();
    }

    private static void UnpackData(Map<Value, Value> map, RPC.GetMultiSubscribersCountResponse resp) {
        Value aa = map.get(ValueFactory.newString("result"));
        Map<Value, Value> result = map.get(ValueFactory.newString("result")).asMapValue().map();
        resp.result.is_last = result.get(ValueFactory.newString("is_last")).asBooleanValue().getBoolean();
        ArrayValue channels = result.get(ValueFactory.newString("channels")).asArrayValue();
        resp.result.channels = new RPC.GetMultiSubscribersCountResponse.ExtResult.Channel[channels.size()];
        for(int idx = 0; idx < channels.size(); idx++) {
            Map<Value, Value> channel = channels.get(idx).asMapValue().map();;
            resp.result.channels[idx] = new RPC.GetMultiSubscribersCountResponse.ExtResult.Channel();
            resp.result.channels[idx].channel_id = channel.get(ValueFactory.newString("channel_id")).asIntegerValue().asLong();
            resp.result.channels[idx].subscribers_count = channel.get(ValueFactory.newString("subscribers_count")).asIntegerValue().asLong();
        }
    }


    private MsgPackHelper() {}
    private static Map<Long, RPC.Response> responseMap = new HashMap<>();
}

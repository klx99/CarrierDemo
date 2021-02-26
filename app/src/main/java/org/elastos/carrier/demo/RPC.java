package org.elastos.carrier.demo;

import org.elastos.carrier.demo.menu.MenuOneDriveHelper;

import java.lang.reflect.Field;
import java.util.Random;

public class RPC {
    public enum Type {
        SetBinary,
        GetBinary,
        GetVersion,
        BackupServiceData,
        RestoreServiceData,
        DownloadNewService,
        StartNewService,
        DeclarePost,
        NotifyPost,
        ReportIllegalComment,
        BlockComment,
        GetReportedComments,
        SignIn,
        DidAuth,
        EnableNotify,
        GetMultiComments,
        GetMultiLikesAndCommentsCount,
        GetMultiSubscribersCount,

        OneDriveLogin,
    }

    public static Request MakeRequest(Type type) {
        Request req = null;

        switch (type) {
        case SetBinary:
            req = new SetBinaryRequest();
            break;
        case GetBinary:
            req = new GetBinaryRequest();
            break;
        case GetVersion:
            req = new GetVersionRequest();
            break;
        case BackupServiceData:
            req = new BackupServiceDataRequest();
            break;
        case RestoreServiceData:
            req = new RestoreServiceDataRequest();
            break;
        case DownloadNewService:
            req = new DownloadNewServiceRequest();
            break;
        case StartNewService:
            req = new StartNewServiceRequest();
            break;
        case DeclarePost:
            req = new DeclarePostRequest();
            break;
        case NotifyPost:
            req = new NotifyPostRequest();
            break;
        case ReportIllegalComment:
            req = new ReportIllegalCommentRequest();
            break;
        case SignIn:
            req = new SignInRequest();
            break;
        case DidAuth:
            req = new DidAuthRequest();
            break;
        case EnableNotify:
            req = new EnableNotifyRequest();
            break;
        case GetMultiComments:
            req = new GetMultiCommentsRequest();
            break;
        case GetMultiLikesAndCommentsCount:
            req = new GetMultiLikesAndCommentsCountRequest();
            break;
        case GetMultiSubscribersCount:
            req = new GetMultiSubscribersCountRequest();
            break;
        }

        return req;
    }

    public static class SetBinaryRequest extends Request {
        String method = "set_binary";
        public ExtParams params = new ExtParams();

        public class ExtParams extends Params {
            String key         = "key-test";
            String algo        = "None";
            String checksum    = "sss";
            public byte[] content     = null;
        }
    }
    static class SetBinaryResponse extends Response {
        ExtResult result = new ExtResult();

        class ExtResult extends Result {
            String key         = "key-test";
        }
    }

    static class GetBinaryRequest extends Request {
        String method = "get_binary";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            String key         = "key-test";
        }
    }
    static class GetBinaryResponse extends Response {
        ExtResult result = new ExtResult();

        class ExtResult extends Result {
            String key         = "key-test";
            String algo        = "None";
            String checksum    = "";
            public byte[] content     = null;
        }
    }

    static class GetVersionRequest extends Request {
        String method = "get_service_version";
    }

    static class GetVersionResponse extends Response {
        ExtResult result = new ExtResult();

        class ExtResult extends Result {
            String version     = null;
            long versionCode   = 0;
        }
    }

    static class BackupServiceDataRequest extends Request {
        String method = "backup_service_data";
        BackupServiceDataRequest.ExtParams params = new BackupServiceDataRequest.ExtParams();

        class ExtParams extends Params {
            String drive_name = "OneDrive";
            String drive_url = "https://graph.microsoft.com/v1.0/me/drive";
            String drive_dir = "/feeds-service/backup";
            String drive_access_token = MenuOneDriveHelper.GetAccessToken();
        }
    }

    static class BackupServiceDataResponse extends Response {
        ExtResult result = new ExtResult();
        class ExtResult extends Result {
        }
    }


    static class RestoreServiceDataRequest extends Request {
        String method = "restore_service_data";
        RestoreServiceDataRequest.ExtParams params = new RestoreServiceDataRequest.ExtParams();

        class ExtParams extends Params {
            String drive_name = "OneDrive";
            String drive_url = "https://graph.microsoft.com/v1.0/me/drive";
            String drive_dir = "/feeds-service/backup";
            String drive_access_token = MenuOneDriveHelper.GetAccessToken();
        }
    }

    static class RestoreServiceDataResponse extends Response {
        ExtResult result = new ExtResult();
        class ExtResult extends Result {
        }
    }

    static class DownloadNewServiceRequest extends Request {
        String method = "download_new_service";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            class Tarball {
                String name;
                long size;
                String md5;
            }

            String new_version    = "1.6.0";
            String base_url = "http://192.168.1.99/";
//            String base_url = "http://10.211.55.2/";
            Tarball macosx = new Tarball() {{
//                name = "Feeds.Service.app.macos.1.6.0.zip";
                name = "output.zip";
                size = 31566889;
                md5 = "6b0ff53fea9f2f0b9c867cc398b8ec26";

            }};
            Tarball ubuntu_1804 = new Tarball() {{
                name = "feedsd_1.6.0_amd64_ubuntu_1804.deb";
                size = 16467278;
                md5 = "5e6d60d3c510122cd576579d7ece168e";

            }};
            Tarball ubuntu_2004 = new Tarball() {{
                name = "feedsd_1.4.0_amd64_ubuntu_2004.deb";
                size = 0;
                md5 = "";

            }};
            Tarball raspbian = new Tarball() {{
                name = "feedsd_1.6.0_armhf_raspbian.deb";
                size = 17641852;
                md5 = "ee10fdd2de64b9008155cb152447bdc5";
            }};
        }
    }

    static class DownloadNewServiceResponse extends Response {
    }

    static class StartNewServiceRequest extends Request {
        String method = "start_new_service";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            String new_version    = "1.6.0";
        }
    }

    static class StartNewServiceResponse extends Response {
    }

    static class DeclarePostRequest extends Request {
        String method = "declare_post";
        DeclarePostRequest.ExtParams params = new DeclarePostRequest.ExtParams();

        class ExtParams extends Params {
            int channelId         = 1;
            byte[] content        = "just a content".getBytes();
            boolean with_notify   = false;
        }
    }

    static class DeclarePostResponse extends Response {
        ExtResult result = new ExtResult();

        class ExtResult extends Result {
            int postId     = 0;
        }
    }


    static class NotifyPostRequest extends Request {
        String method = "notify_post";
        NotifyPostRequest.ExtParams params = new NotifyPostRequest.ExtParams();

        class ExtParams extends Params {
            int channelId      = 1;
            int postId         = 5;
        }
    }

    static class NotifyPostResponse extends Response {
    }


    static class SignInRequest extends Request {
        String method = "standard_sign_in";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            String document      = StandardAuth.makeDoc();
        }
    }

    static class SignInResponse extends Response {
        ExtResult result = new ExtResult();

        class ExtResult extends Result {
            String jwtChallenge     = null;
        }
    }

    static class DidAuthRequest extends Request {
        String method = "standard_did_auth";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            String userName = "CarrierDemo";
            String jwtVP = StandardAuth.makeVP();
        }
    }

    static class DidAuthResponse extends Response {
        ExtResult result = new ExtResult();

        class ExtResult extends Result {
            String accessToken     = null;
        }
    }

    static class EnableNotifyRequest extends Request {
        String method = "enable_notification";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            String accessToken= StandardAuth.GetAccessToken();
        }
    }

    static class EnableNotifyResponse extends Response {
    }

    static class GetMultiCommentsRequest extends Request {
        String method = "get_multi_comments";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            long channel_id = 0;
            long post_id = 0;
            long by = 1;
            long upper_bound = 10000;
            long lower_bound = 1;
            long max_count = 0;
        }
    }

    static class GetMultiCommentsResponse extends Response {
        ExtResult result = new ExtResult();

        static class ExtResult extends Result {
            static class Comment {
                long channel_id = -1;
                long post_id = -1;
                long comment_id = -1;
                long refer_comment_id = -1;
                long status = -1;
                String user_did;
                String user_name;
                byte[] content;
                long likes = -1;
                long created_at = -1;
                long updated_at = -1;
            }

            Boolean is_last     = null;
            Comment[] comments;
        }
    }

    static class GetMultiLikesAndCommentsCountRequest extends Request {
        String method = "get_multi_likes_and_comments_count";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            long channel_id = 0;
            long post_id = 0;
            long by = 1;
            long upper_bound = 10000;
            long lower_bound = 1;
            long max_count = 0;
        }
    }

    static class GetMultiLikesAndCommentsCountResponse extends Response {
        ExtResult result = new ExtResult();

        static class ExtResult extends Result {
            static class Post {
                long channel_id = -1;
                long post_id = -1;
                long comments_count = -1;
                long likes_count = -1;
            }

            Boolean is_last     = null;
            Post[] posts;
        }
    }


    static class GetMultiSubscribersCountRequest extends Request {
        String method = "get_multi_subscribers_count";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            long channel_id = 0;
        }
    }

    static class GetMultiSubscribersCountResponse extends Response {
        ExtResult result = new ExtResult();

        static class ExtResult extends Result {
            static class Channel {
                long channel_id = -1;
                long subscribers_count = -1;
            }

            Boolean is_last     = null;
            Channel[] channels;
        }
    }

    static class ReportIllegalCommentRequest extends Request {
        String method = "report_illegal_comment";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            int channelId         = 1;
            int postId            = 2;
            int commentId         = 1;
            String reasons         = "just a joke";
        }
    }

    static class ReportIllegalCommentResponse extends Response {
    }

    static class ErrorResponse extends Response {
        ExtResult error = new ExtResult();

        class ExtResult extends Result {
            long code = -1;
            String message = null;
        }
    }

    public static class Request {
        String version = "1.0";
        String method = null;
        long id = Math.abs(new Random().nextInt());
        Params params = new Params();

        String getMethod() {
            String method = null;
            try {
                Field field = this.getClass().getDeclaredField("method");
                method = (String) field.get(this);
            } catch (Exception e) {
                e.printStackTrace();
                assert (false);
            }

            return method;
        }
    }

    static class Response {
        long id = 0;

        static class Result {
            public String toString() {
                return ToString(this);
            }
        }

        public String toString() {
            return ToString(this);
        }
    }

    static class Params {
        String accessToken= StandardAuth.GetAccessToken();
    }

    private static String ToString(Object obj) {
        String ret = obj.getClass().getSimpleName() + "{";
        Field[] fieldArray = obj.getClass().getDeclaredFields();
        for(Field field: fieldArray) {
            try {
                if(field.getName().contains("this$")) { // ignore
                    continue;
                }

                ret += field.getName() + "=" + field.get(obj).toString() + ";";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        ret += "}";
        return ret;
    }

    private RPC() {}
}

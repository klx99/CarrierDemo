package org.elastos.carrier.demo;

import java.lang.reflect.Field;
import java.util.Random;

public class RPC {
    public enum Type {
        SetBinary,
        GetBinary,
        GetVersion,
        DeclarePost,
        NotifyPost,
        ReportIllegalComment,
        BlockComment,
        GetReportedComments,
        SignIn,
        DidAuth,
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
        }
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
            String challenge     = null;
        }
    }

    static class DidAuthRequest extends Request {
        String method = "standard_did_auth";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            String vp = StandardAuth.makeVP();
        }
    }

    static class DidAuthResponse extends Response {
        ExtResult result = new ExtResult();

        class ExtResult extends Result {
            String accessToken     = null;
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

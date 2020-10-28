package org.elastos.carrier.demo;

import java.lang.reflect.Field;
import java.util.Random;

public class RPC {
    enum Type {
        SetBinary,
        GetBinary,
        GetVersion,
        ReportIllegalComment,
        BlockComment,
        GetReportedComments,
    }

    static Request MakeRequest(Type type) {
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
        case ReportIllegalComment:
            req = new ReportIllegalCommentRequest();
            break;
        }

        return req;
    }

    static class SetBinaryRequest extends Request {
        String method = "set_binary";
        ExtParams params = new ExtParams();

        class ExtParams extends Params {
            String key         = "key-test";
            String algo        = "None";
            String checksum    = "";
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

    static class Request {
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
        String accessToken= "access-token-test";
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

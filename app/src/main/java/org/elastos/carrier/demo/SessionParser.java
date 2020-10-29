package org.elastos.carrier.demo;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.elastos.carrier.demo.Logger.TAG;

public class SessionParser {
    public interface OnUnpackedListener {
        void onUnpacked(byte[] headData);
    }

    class Protocol {
        class Info {
            long magicNumber;
            int version;
            int headSize;
            long bodySize;
        };
        class Payload {
            class BodyData {
                //                std::filesystem::path filepath;
//                std::fstream stream;
                long receivedBodySize;
            }

            byte[] headData = new byte[0];
            BodyData bodyData = new BodyData();
        }

        Info info = new Info();
        Payload payload = new Payload();

        static final int InfoSize = 24;
        static final long MagicNumber = 0x00A5202008275AL;
        static final int Version_01_00_00 = 10000;
    }

    class ErrCode {
        static final int CarrierSessionDataNotEnough      = -138;
        static final int CarrierSessionUnsuppertedVersion = -139;
        static final int CarrierSessionReleasedError      = -140;
        static final int CarrierSessionSendFailed         = -141;
        static final int CarrierSessionErrorExists        = -142;
    }

    public int unpack(byte[] data, OnUnpackedListener listener) {
        // Log::W(Log::TAG, "%s datasize=%d", __PRETTY_FUNCTION__, data.size());

        int dataPos = 0;

        do {
            int ret = unpackProtocol(data, dataPos);
            if(ret == ErrCode.CarrierSessionDataNotEnough) {
                return 0; // Ignore to dump backtrace
            }
            if(ret < 0) {
                throw new RuntimeException("unpack session protocol error.");
            }
            dataPos += ret;

            ret = unpackBodyData(data, dataPos, listener);
            if(ret < 0) {
                throw new RuntimeException("unpack session body data error.");
            }
            dataPos += ret;
            // Log::D(Log::TAG, "%s datapos=%d", __PRETTY_FUNCTION__, dataPos);
        } while(dataPos < data.length);

        return 0;
    }

    int unpackProtocol(byte[] data, int offset) {
        // protocal info has been parsed, value data is body payload, return directly.
        if(protocol != null
        && protocol.info.headSize == protocol.payload.headData.length) {
            // Log::D(Log::TAG, "Protocol has been parsed.");
            return 0;
        }

        int cachingDataPrevSize = cachingData.length;
        byte[] tmp = new byte[cachingData.length + data.length - offset];
        System.arraycopy(cachingData, 0, tmp, 0, cachingData.length);
        System.arraycopy(data, offset, tmp, cachingData.length, data.length - offset);
        cachingData = tmp;

        if(protocol == null) {
            // find first magic number and remove garbage data.
            byte[] searchMagicNum = ToBytes(Protocol.MagicNumber);
            int garbageIdx;
            for(garbageIdx = 0; garbageIdx <= (cachingData.length - Protocol.InfoSize); garbageIdx++) {
                boolean found = equals(searchMagicNum, 0, 7,
                                       cachingData, garbageIdx, 7);
                if(found) {
                    break;
                }
            }
            if(garbageIdx > 0) {
                Log.w(TAG, "Remove garbage size " + garbageIdx);

                tmp = new byte[cachingData.length - garbageIdx];
                System.arraycopy(cachingData, garbageIdx, tmp, 0, cachingData.length - garbageIdx);
                cachingData = tmp;
                cachingDataPrevSize -= garbageIdx; // recompute valid data of previous cache.
            }

            // return and parse next time if data is not enough to parse info.
            if(cachingData.length < Protocol.InfoSize) {
                Log.d(TAG, "Protocol info data is not enough.");
                return ErrCode.CarrierSessionDataNotEnough;
            }

//            if(std::filesystem::exists(bodyCacheDir) == false) {
//                bool created = std::filesystem::create_directories(bodyCacheDir);
//                CHECK_ASSERT(created, ErrCode::FileNotExistsError);
//            }
//            auto bodyPath = bodyCacheDir / (BodyCacheName + std::to_string(Random::Gen<uint32_t>()));
            protocol = new Protocol();

            int dataPtr = 0;

            byte[] netOrderMagicNum = Arrays.copyOfRange(cachingData, dataPtr, dataPtr + 8);;
            protocol.info.magicNumber = ToLong(netOrderMagicNum);
            dataPtr += 8;

            byte[] netOrderVersion = Arrays.copyOfRange(cachingData, dataPtr, dataPtr + 4);;
            protocol.info.version = ToInt(netOrderVersion);
            if(protocol.info.version != Protocol.Version_01_00_00) {
                Log.w(TAG, "Unsupperted version " + protocol.info.version);
                return ErrCode.CarrierSessionUnsuppertedVersion;
            }
            dataPtr += 4;

            byte[] netOrderHeadSize = Arrays.copyOfRange(cachingData, dataPtr, dataPtr + 4);
            protocol.info.headSize = ToInt(netOrderHeadSize);
            dataPtr += 4;

            byte[] netOrderBodySize = Arrays.copyOfRange(cachingData, dataPtr, dataPtr + 8);
            protocol.info.bodySize = ToLong(netOrderBodySize);
            dataPtr += 8;

            Log.d(TAG, "Transfer start. timestamp=" + System.currentTimeMillis());
        }

        // return and parse next time if data is not enough to save as head data.
        if(cachingData.length < (Protocol.InfoSize + protocol.info.headSize)) {
            Log.d(TAG, "Protocol head data is not enough.");
            return ErrCode.CarrierSessionDataNotEnough;
        }

        // store head data and clear cache.
        int headDataPtr = Protocol.InfoSize;
        protocol.payload.headData = Arrays.copyOfRange(cachingData, headDataPtr, headDataPtr + protocol.info.headSize);;
        cachingData = new byte[0];

        // body offset of input data.
        int bodyStartIdx = (Protocol.InfoSize + protocol.info.headSize - cachingDataPrevSize);

        return bodyStartIdx;
    }

    int unpackBodyData(byte[] data, int offset, OnUnpackedListener listener) {
        int neededData = (int) (protocol.info.bodySize - protocol.payload.bodyData.receivedBodySize);
        int realSize = (neededData < (data.length - offset)
                ? neededData : (data.length - offset));

//        protocol.payload.bodyData.stream.write((char*)data.data() + offset, realSize);
        // protocol.payload.bodyData.insert(protocol.payload.bodyData.end(),
        //                                   data.begin() + offset, data.begin() + offset + realSize);
        protocol.payload.bodyData.receivedBodySize += realSize;

        if(protocol.payload.bodyData.receivedBodySize == protocol.info.bodySize) {
            Log.d(TAG, "Transfer finished. timestamp=" + System.currentTimeMillis());

            if(listener != null) {
//                protocol.payload.bodyData.stream.flush();
//                protocol.payload.bodyData.stream.close();

//                (*listener)(protocol.payload.headData, protocol.payload.bodyData.filepath);
                listener.onUnpacked(protocol.payload.headData);

            }
            protocol = null;
        }

        return realSize;
    }

//    private SessionParser() {}

    public static byte[] ToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(0, value);
        return buffer.array();
    }

    public static long ToLong(byte[] value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(value, 0, 8);
        buffer.flip(); //need flip
        return buffer.getLong();
    }

    public static int ToInt(byte[] value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(value, 0, 4);
        buffer.flip(); //need flip
        return buffer.getInt();
    }

    private boolean equals(byte[] arr1, int from1, int to1, byte[] arr2, int from2, int to2) {
        if (to1 - from1 < 0 || to1 - from1 != to2 - from2)
            return false;
        int i1 = from1, i2 = from2;
        while (i1 <= to1) {
            if (arr1[i1] != arr2[i2])
                return false;
            ++i1;
            ++i2;
        }
        return true;
    }

    private Protocol protocol;
    private byte[] cachingData = new byte[0];
}

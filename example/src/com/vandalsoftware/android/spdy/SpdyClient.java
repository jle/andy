package com.vandalsoftware.android.spdy;

import android.util.Log;

import com.vandalsoftware.android.net.SSLSocketChannel;
import com.vandalsoftware.android.net.SocketReadHandler;
import org.apache.http.params.HttpConnectionParams;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class SpdyClient {
    private static final String TAG = "spdy";

    private int mVersion;
    private SSLSocketFactory mSocketFactory;
    AtomicInteger mCurrentStreamId;

    public SpdyClient(SSLSocketFactory socketFactory) {
        this(3, socketFactory);
    }

    public SpdyClient(int version, SSLSocketFactory socketFactory) {
        mVersion = version;
        mSocketFactory = socketFactory;
        mCurrentStreamId = new AtomicInteger(1);
    }

    public void execute(SpdyHttpRequest request) throws IOException {
        SessionHandler handler = new SessionHandler(mVersion);
        handler.addFrameHandler(new HttpStreamHandler());
        SSLSocketChannel channel = SSLSocketChannel.open(mSocketFactory);
        channel.setSocketReadHandler(handler);
        final URI uri = request.getURI();
        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }
        int socketTimeout = HttpConnectionParams.getSoTimeout(request.getParams());
        channel.connect(new InetSocketAddress(uri.getHost(), port), socketTimeout);
        Log.d(TAG, "socket connected.");
        final DefaultSpdySynStreamFrame synStreamFrame = new DefaultSpdySynStreamFrame(mCurrentStreamId.getAndAdd(2), 0, (byte) 0);
        synStreamFrame.addHeader(":method", request.getMethod());
        synStreamFrame.addHeader(":path", uri.getPath());
        synStreamFrame.addHeader(":version", request.getProtocolVersion());
        synStreamFrame.addHeader(":host", uri.getHost());
        synStreamFrame.addHeader(":scheme", uri.getScheme());
        synStreamFrame.setLast(true);
        Log.d(TAG, "Wrote to socket.");
        handler.send(channel, synStreamFrame);
    }

    class HttpStreamHandler implements FrameHandler {
        private IdentityHashMap<Integer, List<Map.Entry<String, String>>> mMap = new IdentityHashMap<Integer, List<Map.Entry<String, String>>>();

        public HttpStreamHandler() {
        }

        @Override
        public void handleFrame(Object frame) {
            try {
                if (frame instanceof SpdySynReplyFrame) {
                    // We've made a network request and now we are receiving the reply.
                    // Gather the stream Id and the headers.
                    SpdySynReplyFrame synReplyFrame = (SpdySynReplyFrame) frame;
                    final int streamId = synReplyFrame.getStreamId();
                    if (streamId > 0) {
                        mMap.put(streamId, synReplyFrame.getHeaders());
                    }
                } else if (frame instanceof SpdyDataFrame) {
                    Log.d(TAG, "Got data frame");
                    SpdyDataFrame dataFrame = (SpdyDataFrame) frame;
                    final int streamId = dataFrame.getStreamId();
                    if (streamId == 0) {
                        return;
                    }
                    List<Map.Entry<String, String>> headers;
                    if (dataFrame.isLast()) {
                        headers = mMap.remove(streamId);
                    } else {
                        headers = mMap.get(streamId);
                    }
                    // Check the content-encoding header
                    String encoding = null;
                    for (Map.Entry<String, String> header : headers) {
                        if ("content-encoding".equals(header.getKey())) {
                            encoding = header.getValue();
                            break;
                        }
                    }
                    ChannelBuffer data = dataFrame.getData();
                    StringBuilder sb = new StringBuilder();
                    int len = data.readableBytes();
                    if ("deflate".equals(encoding)) {
                        Log.d(TAG, "deflating: " + len);
                        Inflater inflater = new Inflater(false);
                        byte[] compressed = new byte[4096];
                        byte[] decompressed = new byte[4096];
                        while (data.readable()) {
                            int length = Math.min(data.readableBytes(), compressed.length);
                            data.readBytes(compressed, 0, length);
                            if (inflater.needsInput()) {
                                inflater.setInput(compressed, 0, length);
                            }
                            int inflated = inflater.inflate(decompressed);
                            sb.append(new String(decompressed, 0, inflated));
                        }
                        inflater.end();
                    } else {
                        Log.d(TAG, "raw: " + len);
                        byte[] compressed = new byte[4096];
                        while (data.readable()) {
                            int length = Math.min(data.readableBytes(), compressed.length);
                            data.readBytes(compressed, 0, length);
                            sb.append(new String(compressed, 0, length));
                        }
                    }
                    Log.d(TAG, "done, len=" + len + ", res=" + sb.toString());
                }
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private class SessionHandler implements SocketReadHandler, FrameHandler {
        private SpdyFrameCodec mSpdyFrameCodec;
        private SpdySessionHandler mSpdySessionHandler;
        final CopyOnWriteArrayList<FrameHandler> mFrameHandlers = new CopyOnWriteArrayList<FrameHandler>();

        public SessionHandler(int version) {
            mSpdyFrameCodec = new SpdyFrameCodec(version);
            mSpdySessionHandler = new SpdySessionHandler(version, false);
        }

        public void send(SSLSocketChannel channel, Object frame) {
            try {
                mSpdySessionHandler.handleDownstream(null, null, frame);
                mSpdyFrameCodec.handleDownstream(null, null, channel, frame);
            } catch (Exception e) {
                // TODO attach this exception to the response reason for failing
                e.printStackTrace();
            }
        }

        public void handleRead(byte[] in, int index, int length) {
            Log.d(TAG, "handleRead " + length);
            ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(in, index, length);
            try {
                Log.d(TAG, "read index: " + channelBuffer.readerIndex());
                mSpdyFrameCodec.handleUpstream(null, null, channelBuffer, this);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "handleRead done");
        }

        @Override
        public void handleFrame(Object frame) {
            try {
                mSpdySessionHandler.messageReceived(null, null, frame);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Hand this off to the next frame handler
            for (FrameHandler handler : mFrameHandlers) {
                handler.handleFrame(frame);
            }
        }

        public void addFrameHandler(FrameHandler handler) {
            mFrameHandlers.add(handler);
        }
    }
}

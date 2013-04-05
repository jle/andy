package com.vandalsoftware.android.spdyexample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.vandalsoftware.android.net.SSLSocketChannel;
import com.vandalsoftware.android.net.SocketReadHandler;
import com.vandalsoftware.android.spdy.DefaultSpdySynStreamFrame;
import com.vandalsoftware.android.spdy.FrameHandler;
import com.vandalsoftware.android.spdy.SpdyDataFrame;
import com.vandalsoftware.android.spdy.SpdyFrameCodec;
import com.vandalsoftware.android.spdy.SpdySessionHandler;
import com.vandalsoftware.android.spdy.SpdySynReplyFrame;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ByteChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

public class SpdyActivity extends Activity {
    private static final String TAG = "spdy";
    private LogView mLogView;
    private Socket mSock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mLogView = (LogView) findViewById(R.id.logview);
        Button startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ConnectTask().execute();
            }
        });
        Button stopBtn = (Button) findViewById(R.id.stop);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("Closing.");
                Socket s = mSock;
                if (s != null) {
                    try {
                        s.close();
                        Log.d(TAG, "socket closed.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                log("Finished.");
            }
        });
        log("Ready.");
    }

    private void log(String message) {
        Log.d(TAG, message);
        mLogView.log(message);
    }

    class ConnectTask extends AsyncTask<Void, Void, Socket> implements SocketReadHandler, FrameHandler {
        private SpdyFrameCodec mSpdyFrameCodec;
        private SpdySessionHandler mSpdySessionHandler;
        private IdentityHashMap<Integer, List<Map.Entry<String, String>>> mMap = new IdentityHashMap<Integer, List<Map.Entry<String, String>>>();

        @Override
        protected void onPreExecute() {
            mSpdyFrameCodec = new SpdyFrameCodec(3);
            mSpdySessionHandler = new SpdySessionHandler(3, false);
        }

        @Override
        protected void onPostExecute(Socket s) {
            mSock = s;
        }

        public void handleRead(ByteChannel channel, byte[] in, int index, int length) {
            Log.d(TAG, "handleRead " + length);
            ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(in, index, length);
            try {
                Log.d(TAG, "read index: " + channelBuffer.readerIndex());
                mSpdyFrameCodec.handleUpstream(null, null, channelBuffer, this);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            Log.d(TAG, "handleRead done");
        }

        @Override
        protected Socket doInBackground(Void... voids) {
            Socket s = null;
            try {
                SSLSocketChannel connector = SSLSocketChannel.open("TLS");
                connector.setSocketReadHandler(this);
                connector.connect(new InetSocketAddress("api.twitter.com", 443), 15000);
                Log.d(TAG, "socket connected.");
                SpdyFrameCodec codec = mSpdyFrameCodec;
                final DefaultSpdySynStreamFrame synStreamFrame = new DefaultSpdySynStreamFrame(1, 0, (byte) 0);
                synStreamFrame.addHeader(":method", "GET");
                synStreamFrame.addHeader(":path", "/1.1/statuses/home_timeline.json");
                synStreamFrame.addHeader(":version", "HTTP/1.1");
                synStreamFrame.addHeader(":host", "api.twitter.com:443");
                synStreamFrame.addHeader(":scheme", "https");
                synStreamFrame.setLast(true);
                mSpdySessionHandler.handleDownstream(null, null, synStreamFrame);
                codec.handleDownstream(null, null, connector, synStreamFrame);
                Log.d(TAG, "Wrote to socket.");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (false && s != null) {
                    try {
                        s.close();
                        Log.d(TAG, "socket closed.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return s;
        }

        @Override
        public void handleFrame(Object frame) {
            try {
                mSpdySessionHandler.messageReceived(null, null, frame);
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
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}

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
import com.vandalsoftware.android.spdy.SpdyFrameCodec;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

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

    class ConnectTask extends AsyncTask<Void, Void, Socket> implements SocketReadHandler {
        private SpdyFrameCodec mSpdyFrameCodec;

        @Override
        protected void onPreExecute() {
            mSpdyFrameCodec = new SpdyFrameCodec(3);
        }

        @Override
        protected void onPostExecute(Socket s) {
            mSock = s;
        }

        public void handleRead(ByteChannel channel) {
            Log.d(TAG, "handleRead");
            byte[] buf = new byte[1024];
            ByteBuffer b = ByteBuffer.wrap(buf);
            ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(b);
            int bytesRead;
            try {
                do {
                    bytesRead = channel.read(b);
                    Log.v(TAG, "bytes read " + bytesRead);
                    b.flip();
                    channelBuffer.setIndex(b.position(), b.limit());
                    Log.d(TAG, "read index: " + channelBuffer.readerIndex());
                    mSpdyFrameCodec.handleUpstream(null, null, channelBuffer);
                    b.clear();
                } while (bytesRead > 0);
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
    }
}

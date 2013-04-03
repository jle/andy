package com.vandalsoftware.android.spdyexample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.vandalsoftware.android.net.SSLSocketChannel;
import com.vandalsoftware.android.net.SocketReadHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
        @Override
        protected void onPostExecute(Socket s) {
            mSock = s;
        }

        public void handleRead(ByteChannel channel) {
            byte[] buf = new byte[512];
            ByteBuffer b = ByteBuffer.wrap(buf);
            StringBuilder sb = new StringBuilder();
            int bytesRead;
            try {
                do {
                    bytesRead = channel.read(b);
                    b.flip();
                    sb.append(new String(buf, b.position(), b.remaining()));
                    b.clear();
                } while (bytesRead > 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "proc " + sb);
        }

        @Override
        protected Socket doInBackground(Void... voids) {
            Socket s = null;
            try {
                SSLSocketChannel connector = SSLSocketChannel.open("TLS");
                connector.setSocketReadHandler(this);
                connector.connect(new InetSocketAddress("api.twitter.com", 443), 15000);
                Log.d(TAG, "socket connected.");
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos));
                writer.println("GET /1/statuses/home_timeline.json HTTP/1.1");
                writer.println("User-Agent: jle-baby/1.0.0");
                writer.println("Accept: */*");
                writer.println("Host: api.twitter.com");
                writer.print("\r\n");
                writer.flush();
                ByteBuffer outBuffer = ByteBuffer.allocate(baos.size());
                outBuffer.put(baos.toByteArray());
                connector.write(outBuffer);
                Log.d(TAG, "Wrote to socket.");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
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

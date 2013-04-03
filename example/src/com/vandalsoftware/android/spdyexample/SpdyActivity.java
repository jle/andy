package com.vandalsoftware.android.spdyexample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

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
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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

    class ConnectTask extends AsyncTask<Void, Void, Socket> {
        @Override
        protected void onPostExecute(Socket s) {
            mSock = s;
        }

        @Override
        protected Socket doInBackground(Void... voids) {
            Socket s = null;
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, null, new SecureRandom());
                SSLSocketFactory factory = ctx.getSocketFactory();
                s = factory.createSocket();
                s.connect(new InetSocketAddress("api.twitter.com", 443), 15000);
                final InputStream is = s.getInputStream();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Start reading.");
                        try {
                            byte[] buf = new byte[8192];
                            final int len = buf.length;
                            int bytesRead;
                            while ((bytesRead = is.read(buf, 0, len)) != -1) {
                                proc(buf, 0, bytesRead);
                            }
                        } catch (IOException e) {
                            Log.d(TAG, "Read interrupted.");
                            e.printStackTrace();
                        } finally {
                            Log.d(TAG, "Finish reading.");
                        }
                    }

                    // Writes to some handler
                    private void proc(byte[] buf, int index, int length) {
                        Log.d(TAG, "proc");
                        Log.d(TAG, new String(buf, index, length));
                    }
                }).start();
                new Thread().start();
                Log.d(TAG, "socket connected.");
                SocketWriteChannel sc = new SocketWriteChannel(s);
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
                sc.write(outBuffer);
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
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
            return s;
        }
    }

    static class SocketWriteChannel implements WritableByteChannel {
        private final OutputStream mOutputStream;
        private boolean mOpen;

        public SocketWriteChannel(Socket sock) throws IOException {
            mOutputStream = sock.getOutputStream();
            mOpen = true;
        }

        public int write(final ByteBuffer buffer) {
            int writtenBytes = 0;
            try {
                buffer.flip();
                byte[] buf = new byte[8192];
                Log.d(TAG, "begin writing..." + buffer.remaining());
                while (buffer.hasRemaining()) {
                    final int byteCount = Math.min(buffer.remaining(), buf.length);
                    buffer.get(buf, 0, byteCount);
                    mOutputStream.write(buf, 0, byteCount);
                    writtenBytes += byteCount;
                }
                buffer.compact();
                Log.d(TAG, "done writing = " + writtenBytes);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return writtenBytes;
        }

        @Override
        public boolean isOpen() {
            return mOpen;
        }

        @Override
        public void close() throws IOException {
            try {
                mOutputStream.close();
            } finally {
                mOpen = false;
            }
        }
    }
}

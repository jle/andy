package com.vandalsoftware.android.net;

import android.util.Log;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public final class SSLSocketChannel implements ByteChannel {
    private static final String TAG = "spdy";
    private final Socket mSocket;
    private SocketReadHandler mSocketReadHandler;
    private boolean mConnected;
    private OutputStream mOutputStream;
    private byte[] mOutBuf;
    protected ByteBuffer mInBuffer;

    private SSLSocketChannel(Socket sock) {
        mSocket = sock;
    }

    public static SSLSocketChannel open(String protocol)
            throws IOException, KeyManagementException, NoSuchAlgorithmException {
        SSLContext ctx = SSLContext.getInstance(protocol);
        ctx.init(null, null, null);
        return open(ctx);
    }

    public static SSLSocketChannel open(SSLContext ctx) throws IOException {
        return open(ctx.getSocketFactory());
    }

    public static SSLSocketChannel open(SSLSocketFactory factory) throws IOException {
        return new SSLSocketChannel(factory.createSocket());
    }

    public void connect(InetSocketAddress remoteAddress, int timeout) throws IOException {
        mSocket.connect(remoteAddress, timeout);
        if (mSocketReadHandler != null) {
            new Thread(new SocketReadRunnable(mSocket)).start();
        }
        mOutputStream = mSocket.getOutputStream();
        mOutBuf = new byte[8192];
        mInBuffer = ByteBuffer.wrap(new byte[8192]);
        mConnected = true;
    }

    public void setSocketReadHandler(SocketReadHandler handler) {
        if (mConnected) {
            throw new IllegalStateException("Cannot attach handler after socket is connected.");
        }
        mSocketReadHandler = handler;
    }

    public int write(ByteBuffer src) {
        int writtenBytes = 0;
        try {
            final byte[] buf = mOutBuf;
            Log.d(TAG, "begin writing..." + src.remaining());
            while (src.hasRemaining()) {
                final int byteCount = Math.min(src.remaining(), buf.length);
                src.get(buf, 0, byteCount);
                mOutputStream.write(buf, 0, byteCount);
                writtenBytes += byteCount;
            }
            src.compact();
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
        return mConnected;
    }

    @Override
    public void close() throws IOException {
        try {
            mOutputStream.close();
            mSocket.close();
        } finally {
            mConnected = false;
        }
    }

    protected final void handleRead() {
        mSocketReadHandler.handleRead(this);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int remaining = mInBuffer.remaining();
        if (remaining > 0) {
            return remaining - dst.put(mInBuffer).remaining();
        } else {
            return 0;
        }
    }

    private class SocketReadRunnable implements Runnable {
        private static final String TAG = "SocketReadRunnable";

        private final InputStream mInputStream;

        public SocketReadRunnable(Socket sock) throws IOException {
            mInputStream = sock.getInputStream();
        }

        @Override
        public void run() {
            Log.d(TAG, "Start reading.");
            try {
                final ByteBuffer buffer = mInBuffer;
                final byte[] buf = buffer.array();
                final int len = buf.length;
                int index = 0;
                int bytesRead;
                while ((bytesRead = mInputStream.read(buf, index, len - index)) != -1) {
                    buffer.position(index).limit(index + bytesRead);
                    handleRead();
                    if (buffer.hasRemaining()) {
                        index = bytesRead;
                    } else {
                        index = 0;
                        buffer.clear();
                    }
                }
            } catch (IOException e) {
                Log.d(TAG, "Read interrupted.");
                e.printStackTrace();
            } finally {
                Log.d(TAG, "Finish reading.");
            }
        }
    }
}

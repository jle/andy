package com.vandalsoftware.android.net;

import android.util.Log;

import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public final class SSLSocketChannel implements ByteChannel {
    private static final String TAG = "net";
    private final Socket mSocket;
    private SocketReadHandler mSocketReadHandler;
    private boolean mConnected;
    private OutputStream mOutputStream;
    private byte[] mOutBuf;
    protected ByteBuffer mInBuffer;

    private SSLSocketChannel(Socket sock) {
        mSocket = sock;
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

    public int write(ByteBuffer src) throws IOException {
        if (!mConnected) {
            return 0;
        }
        int writtenBytes = 0;
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
        return writtenBytes;
    }

    @Override
    public boolean isOpen() {
        return mConnected;
    }

    @Override
    public void close() throws IOException {
        try {
            mSocket.close();
        } finally {
            mConnected = false;
        }
    }

    protected final void handleRead(byte[] in, int index, int length) {
        mSocketReadHandler.handleRead(this, in, index, length);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int remaining = mInBuffer.remaining();
        if (remaining > 0) {
            dst.put(mInBuffer);
            return remaining - mInBuffer.remaining();
        } else {
            return 0;
        }
    }

    private class SocketReadRunnable implements Runnable {
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
                    Log.v(TAG, "read from socket = " + bytesRead);
                    buffer.position(index).limit(index + bytesRead);
                    Log.v(TAG, "buf " + buffer.position() + ", " + buffer.limit() + ", " + buffer.remaining());
                    handleRead(buf, index, bytesRead);
                    if (buffer.hasRemaining()) {
                        index = buffer.position();
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
                try {
                    mInputStream.close();
                    close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}

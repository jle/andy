package com.vandalsoftware.android.net;

import android.util.Log;

import org.jboss.netty.buffer.ChannelBuffer;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class SSLSocketChannel {
    private static final String TAG = "net";
    private final Socket mSocket;
    private SocketReadHandler mSocketReadHandler;
    private boolean mConnected;
    private OutputStream mOutputStream;
    private byte[] mOutBuf;
    private byte[] mInBuf;

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
        mInBuf = new byte[8192];
        mConnected = true;
    }

    public void setSocketReadHandler(SocketReadHandler handler) {
        if (mConnected) {
            throw new IllegalStateException("Cannot attach handler after socket is connected.");
        }
        mSocketReadHandler = handler;
    }

    public int write(ChannelBuffer src) throws IOException {
        if (!mConnected) {
            return 0;
        }
        int writtenBytes = 0;
        final byte[] buf = mOutBuf;
        Log.d(TAG, "begin writing..." + src.readableBytes());
        while (src.readable()) {
            final int byteCount = Math.min(src.readableBytes(), buf.length);
            src.readBytes(buf, 0, byteCount);
            mOutputStream.write(buf, 0, byteCount);
            writtenBytes += byteCount;
        }
        Log.d(TAG, "done writing = " + writtenBytes);
        return writtenBytes;
    }

    public boolean isConnected() {
        return mConnected;
    }

    public void close() throws IOException {
        try {
            mSocket.close();
        } finally {
            mConnected = false;
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
                final byte[] buf = mInBuf;
                final int len = buf.length;
                int index = 0;
                int bytesRead;
                while ((bytesRead = mInputStream.read(buf, index, len - index)) != -1) {
                    Log.v(TAG, "read from socket = " + bytesRead);
                    if (mSocketReadHandler != null) {
                        mSocketReadHandler.handleRead(buf, index, bytesRead);
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

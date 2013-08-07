package com.vandalsoftware.android.net;

public interface SocketReadHandler {
    void handleRead(byte[] in, int index, int length);
}

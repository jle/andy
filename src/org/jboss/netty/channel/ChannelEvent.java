package org.jboss.netty.channel;

import java.net.SocketAddress;

public interface ChannelEvent {
    public ChannelFuture getFuture();

    Object getMessage();

    SocketAddress getRemoteAddress();

    Channel getChannel();

    Exception getCause();

    Bung getState();

    Object getValue();

    enum Bung {
        CONNECTED, BOUND, OPEN

    }
}

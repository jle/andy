package org.jboss.netty.channel;

import java.net.SocketAddress;

public class Channels {
    public static ChannelFuture future(Channel channel) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void write(ChannelHandlerContext ctx, ChannelFuture future, Object message, SocketAddress address) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static ChannelFuture succeededFuture(Channel channel) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void close(ChannelHandlerContext ctx, ChannelFuture future) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void fireMessageReceived(ChannelHandlerContext ctx, Object message, SocketAddress address) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void fireExceptionCaught(ChannelHandlerContext ctx, Exception e) {
        throw new UnsupportedOperationException("Not implemented");
    }
}

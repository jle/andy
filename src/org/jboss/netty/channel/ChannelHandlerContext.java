package org.jboss.netty.channel;

public interface ChannelHandlerContext {
    void sendDownstream(ChannelEvent evt);

    Channel getChannel();
}

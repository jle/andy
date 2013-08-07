package org.jboss.netty.channel;

public interface ChannelStateEvent extends ChannelEvent {
    ChannelFuture getFuture();

    Channel getChannel();
}

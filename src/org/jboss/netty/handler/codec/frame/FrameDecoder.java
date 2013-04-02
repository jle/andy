package org.jboss.netty.handler.codec.frame;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;

public class FrameDecoder {
    public FrameDecoder(boolean b) {
        //To change body of created methods use File | Settings | File Templates.
    }

    protected Object decodeLast(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
            throws Exception {
        return null;
    }

    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
            throws Exception {
        return null;
    }

    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) {

    }
}

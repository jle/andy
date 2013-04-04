package org.jboss.netty.handler.codec.frame;

import android.util.Log;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.CompositeChannelBuffer;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;

import java.nio.ByteOrder;

public abstract class FrameDecoder {
    public static final int DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS = 1024;

    private int maxCumulationBufferComponents = DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS;
    protected ChannelBuffer cumulation;
    private int copyThreshold;

    public FrameDecoder(boolean unfold) {
    }

    protected Object decodeLast(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        return decode(ctx, channel, buffer);
    }

    protected abstract Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer)
            throws Exception;

    private void callDecode(
            ChannelHandlerContext context, Channel channel,
            ChannelBuffer cumulation) throws Exception {

        while (cumulation.readable()) {
            int oldReaderIndex = cumulation.readerIndex();
            Object frame = decode(context, channel, cumulation);
            if (frame == null) {
                if (oldReaderIndex == cumulation.readerIndex()) {
                    // Seems like more data is required.
                    // Let us wait for the next notification.
                    break;
                } else {
                    // Previous data has been discarded.
                    // Probably it is reading on.
                    continue;
                }
            } else if (oldReaderIndex == cumulation.readerIndex()) {
                throw new IllegalStateException(
                        "decode() method must read at least one byte " +
                                "if it returned a frame (caused by: " + getClass() + ')');
            }
            Log.v("jle", "frame decoded: " + frame);
        }
    }

    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e, Object m) throws Exception {

        ChannelBuffer input = (ChannelBuffer) m;
        if (!input.readable()) {
            return;
        }

        if (cumulation == null) {
            try {
                // the cumulation buffer is not created yet so just pass the input to callDecode(...) method
                callDecode(ctx, null, input);
            } finally {
                updateCumulation(ctx, input);
            }

        } else {
            input = appendToCumulation(input);
            try {
                callDecode(ctx, null, input);
            } finally {
                updateCumulation(ctx, input);
            }
        }
    }

    /**
     * Create a new {@link ChannelBuffer} which is used for the cumulation.
     * Sub-classes may override this.
     *
     * @param ctx {@link ChannelHandlerContext} for this handler
     * @return buffer the {@link ChannelBuffer} which is used for cumulation
     */
    protected ChannelBuffer newCumulationBuffer(
            ChannelHandlerContext ctx, int minimumCapacity) {
        ChannelBufferFactory factory = HeapChannelBufferFactory.getInstance(ByteOrder.BIG_ENDIAN);
        return factory.getBuffer(Math.max(minimumCapacity, 256));
    }

    protected ChannelBuffer appendToCumulation(ChannelBuffer input) {
        ChannelBuffer cumulation = this.cumulation;
        assert cumulation.readable();
        if (cumulation instanceof CompositeChannelBuffer) {
            // Make sure the resulting cumulation buffer has no more than the configured components.
            CompositeChannelBuffer composite = (CompositeChannelBuffer) cumulation;
            if (composite.numComponents() >= maxCumulationBufferComponents) {
                cumulation = composite.copy();
            }
        }

        this.cumulation = input = ChannelBuffers.wrappedBuffer(cumulation, input);
        return input;
    }

    protected ChannelBuffer updateCumulation(ChannelHandlerContext ctx, ChannelBuffer input) {
        ChannelBuffer newCumulation;
        int readableBytes = input.readableBytes();
        if (readableBytes > 0) {
            int inputCapacity = input.capacity();

            // If input.readableBytes() == input.capacity() (i.e. input is full),
            // there's nothing to save from creating a new cumulation buffer
            // even if input.capacity() exceeds the threshold, because the new cumulation
            // buffer will have the same capacity and content with input.
            if (readableBytes < inputCapacity && inputCapacity > copyThreshold) {
                // At least one byte was consumed by callDecode() and input.capacity()
                // exceeded the threshold.
                cumulation = newCumulation = newCumulationBuffer(ctx, input.readableBytes());
                cumulation.writeBytes(input);
            } else {
                // Nothing was consumed by callDecode() or input.capacity() did not
                // exceed the threshold.
                if (input.readerIndex() != 0) {
                    cumulation = newCumulation = input.slice();
                } else {
                    cumulation = newCumulation = input;
                }
            }
        } else {
            cumulation = newCumulation = null;
        }
        return newCumulation;
    }
}

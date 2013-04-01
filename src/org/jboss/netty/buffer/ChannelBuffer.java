package org.jboss.netty.buffer;

public interface ChannelBuffer {
    public byte getByte(int offset);

    int readableBytes();

    void writeBytes(byte[] out, int i, int index);

    void readBytes(byte[] in);

    int readerIndex();

    ChannelBuffer readSlice(int bytes);

    ChannelBuffer readBytes(int length);

    void skipBytes(int bytes);

    short getShort(int i);

    short readUnsignedShort();

    int readInt();

    void markReaderIndex();

    void resetReaderIndex();

    void discardReadBytes();

    byte readByte();
}

package org.jboss.netty.channel;

public interface ChannelFuture {
    void setFailure(Exception e);

    void addListener(ChannelFutureListener close);

    void setSuccess();

    Exception getCause();

    boolean isSuccess();
}

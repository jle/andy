package com.vandalsoftware.android.spdy;

import org.apache.http.ProtocolVersion;
import org.apache.http.message.AbstractHttpMessage;

public abstract class SpdyHttpMessage extends AbstractHttpMessage {
    private static final String PROTOCOL_HTTP = "HTTP";
    private final ProtocolVersion mProtocolVersion;

    public SpdyHttpMessage() {
        this(PROTOCOL_HTTP, 1, 1);
    }

    public SpdyHttpMessage(String protocol, int majorVers, int minorVers) {
        mProtocolVersion = new ProtocolVersion(protocol, majorVers, minorVers);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return mProtocolVersion;
    }
}

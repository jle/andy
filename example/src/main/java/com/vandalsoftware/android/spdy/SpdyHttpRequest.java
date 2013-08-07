package com.vandalsoftware.android.spdy;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class SpdyHttpRequest extends SpdyHttpMessage {
    private final URI mUri;

    public SpdyHttpRequest(String uri) throws URISyntaxException {
        this(new URI(uri));
    }

    public SpdyHttpRequest(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be null");
        }
        mUri = uri;
    }

    /**
     * Returns the URI for this message.
     */
    public URI getURI() {
        return mUri;
    }

    public abstract String getMethod();
}

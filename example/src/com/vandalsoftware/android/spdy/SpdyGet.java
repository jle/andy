package com.vandalsoftware.android.spdy;

import java.net.URI;
import java.net.URISyntaxException;

public class SpdyGet extends SpdyHttpRequest {
    public SpdyGet(String uri) throws URISyntaxException {
        super(uri);
    }

    public SpdyGet(URI uri) {
        super(uri);
    }

    @Override
    public String getMethod() {
        return "GET";
    }
}

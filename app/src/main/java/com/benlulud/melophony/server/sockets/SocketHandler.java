package com.benlulud.melophony.server.sockets;

import java.io.IOException;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.protocols.websockets.WebSocket;

public class SocketHandler extends NanoWSD {

    private static final int SOCKET_READ_TIMEOUT = 60000;

    private DefaultSocket socket;
    private SocketCallbacks socketCallbacks;

    public SocketHandler() throws IOException {
        super(1805);
        this.socket = null;
        start(SOCKET_READ_TIMEOUT, false);
    }

    public void setSocketCallbacks(final SocketCallbacks callbacks) {
        this.socketCallbacks = callbacks;
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession session) {
        this.socket = new DefaultSocket(session, socketCallbacks);
        return socket;
    }

    public DefaultSocket getSocket() {
        return this.socket;
    }
}
package com.benlulud.melophony.server.sockets;

import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.WebSocketFrame;


public class SocketCallbacks {
    protected void onOpen(final DefaultSocket socket) { }
    protected void onClose(final DefaultSocket socket, final CloseCode code, final String reason, final boolean initiatedByRemote) { }
    protected void onMessage(final DefaultSocket socket, final WebSocketFrame webSocketFrame) { }
}
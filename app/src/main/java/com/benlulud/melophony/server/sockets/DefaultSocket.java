package com.benlulud.melophony.server.sockets;

import java.io.IOException;

import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;


public class DefaultSocket extends WebSocket {

    private static final String TAG = DefaultSocket.class.getSimpleName();

    private SocketCallbacks socketCallbacks;

    public DefaultSocket(final IHTTPSession handshakeRequest, final SocketCallbacks callbacks) {
        super(handshakeRequest);
        this.socketCallbacks = callbacks;
    }

    protected void onOpen() {
        Log.i(TAG, "onOpen");
        if (socketCallbacks != null) {
            socketCallbacks.onOpen(this);
        }
    }

    protected void onClose(final CloseCode code, final String reason, final boolean initiatedByRemote) {
        Log.i(TAG, "onClose: " + code + ", reason: " + reason + ", initiatedByRemote: " + initiatedByRemote);
        if (socketCallbacks != null) {
            socketCallbacks.onClose(this, code, reason, initiatedByRemote);
        }
    }

    protected void onPong(final WebSocketFrame pong) {
        Log.i(TAG, "onPong: " + pong);
    }

    protected void onException(final IOException exception) {
        Log.e(TAG, "Exception while using socket: ", exception);
    }

    @Override
    protected void onMessage(final WebSocketFrame webSocketFrame) {
        if (socketCallbacks != null) {
            socketCallbacks.onMessage(this, webSocketFrame);
        }
    }

    public void sendMessage(final String data) {
        try {
            send(data);
        } catch (IOException e) {
            Log.e(TAG, "Unable to send message through socket: ", e);
        }
    }
}
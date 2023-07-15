package com.benlulud.melophony.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.router.RouterNanoHTTPD;

import com.benlulud.melophony.server.handlers.StaticFileHandler;
public class Router extends RouterNanoHTTPD {

    private static final String TAG = Router.class.getSimpleName();

    private Context context;
    public Router(final Context context) throws IOException {
        super(1804);
        this.context = context;
        addMappings();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public void addMappings() {
        super.addMappings();
        addRoute("(index.html)?", StaticFileHandler.class);
        addRoute("public/.*", StaticFileHandler.class);
    }


    @Override
    public Response serve(IHTTPSession session) {
        Log.i(TAG, "Received [" + session.getMethod() + " " + session.getUri() + "]");
        if (Method.OPTIONS.equals(session.getMethod())) {
            Log.i(TAG, "Options");
            return withCorsHeaders(Response.newFixedLengthResponse(Status.OK, "text/html", new byte[0]));
        }

        final Response response = withCorsHeaders(super.serve(new SessionAdapter(session, context, socketHandler)));
        Log.i(TAG, "Response [" + response.getStatus() + " " + response.getMimeType() + "]");
        return response;
    }

    private Response withCorsHeaders(final Response response) {
        response.addHeader("allow", "GET,POST,PATCH,DELETE,OPTIONS");
        response.addHeader("access-control-allow-headers", "*");
        response.addHeader("access-control-allow-origin", "*");
        response.addHeader("access-control-allow-methods", "GET,POST,PATCH,DELETE,OPTIONS");
        return response;
    }
}
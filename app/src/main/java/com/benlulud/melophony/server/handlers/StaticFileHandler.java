package com.benlulud.melophony.server.handlers;

import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.router.RouterNanoHTTPD.DefaultStreamHandler;
import org.nanohttpd.router.RouterNanoHTTPD.UriResource;

import com.benlulud.melophony.server.SessionAdapter;


public class StaticFileHandler extends DefaultStreamHandler {

    private static Map<String, String> MIME_TYPES = new HashMap<String, String>();
    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("js", "text/javascript");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("m4a", "audio/m4a");
    }

    private String uri = "";

    @Override
    public String getMimeType() {
        return MIME_TYPES.getOrDefault(uri.substring(uri.lastIndexOf(".") + 1), "text/plain");
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }

    @Override
    public InputStream getData() {
        return null;
    }

    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        try {
            final SessionAdapter sessionAdapter = (SessionAdapter) session;
            final Context context = sessionAdapter.getContext();
            uri = session.getUri();
            if (uri == null || "".equals(uri) || "/".equals(uri)) {
                uri = "/index.html";
            }
            return Response.newChunkedResponse(getStatus(), getMimeType(), context.getAssets().open("www" + uri));
        } catch (Exception e) {
            return Response.newFixedLengthResponse("Error");
        }
    }
}
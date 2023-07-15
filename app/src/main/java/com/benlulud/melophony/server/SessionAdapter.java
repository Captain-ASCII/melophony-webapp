package com.benlulud.melophony.server;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.content.Context;

import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD.ResponseException;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.router.RouterNanoHTTPD;



public class SessionAdapter implements IHTTPSession {

    private final IHTTPSession session;
    private final Context context;
    public SessionAdapter(final IHTTPSession session, final Context context) {
        this.session = session;
        this.context = context;
    }

    // Adapter methods

    public Context getContext() {
        return this.context;
    }


    // Forwarded methods

    public void execute() throws IOException {
        session.execute();
    }

    public CookieHandler getCookies() {
        return session.getCookies();
    }

    public Map<String, String> getHeaders() {
        return session.getHeaders();
    }

    public InputStream getInputStream() {
        return session.getInputStream();
    }

    public Method getMethod() {
        return session.getMethod();
    }

    public Map<String, String> getParms() {
        return session.getParms();
    }

    public Map<String, List<String>> getParameters() {
        return session.getParameters();
    }

    public String getQueryParameterString() {
        return session.getQueryParameterString();
    }

    public String getUri() {
        return session.getUri();
    }

    public void parseBody(Map<String, String> files) throws IOException, ResponseException {
        session.parseBody(files);
    }

    public String getRemoteIpAddress() {
        return session.getRemoteIpAddress();
    }
}
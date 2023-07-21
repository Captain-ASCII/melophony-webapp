package com.benlulud.melophony.server.handlers;

import java.util.Map;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.router.RouterNanoHTTPD.DefaultHandler;
import org.nanohttpd.router.RouterNanoHTTPD.UriResource;

import com.google.gson.Gson;

import com.benlulud.melophony.database.Database;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.webapp.Constants;


public class KeysHandler extends DefaultHandler {

    private static final String TAG = KeysHandler.class.getSimpleName();

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }

    @Override
    public String getText() {
        return "{\"result\": \"Synchronization has started\"}";
    }

    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        final Database db = Database.getDatabase();
        return ServerUtils.response(Status.OK, "Keys obtained", db.getPersistedData(Constants.SECRET_KEYS_KEY), null);
    }
}
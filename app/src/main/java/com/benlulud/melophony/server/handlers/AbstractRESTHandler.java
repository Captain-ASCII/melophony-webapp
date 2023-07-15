package com.benlulud.melophony.server.handlers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.google.gson.Gson;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.router.RouterNanoHTTPD.DefaultHandler;
import org.nanohttpd.router.RouterNanoHTTPD.UriResource;

import com.benlulud.melophony.api.client.ApiResponse;
import com.benlulud.melophony.database.Database;
import com.benlulud.melophony.database.DatabaseAspect;
import com.benlulud.melophony.database.IModel;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.server.SessionAdapter;



public abstract class AbstractRESTHandler<T extends IModel> extends DefaultHandler {

    private final String TAG = AbstractRESTHandler.class.getSimpleName();
    private final String POST_DATA = "postData";
    private final String MESSAGE_SUCCESS = "Success";

    private static interface AuthCallback {
        Response onAuthorization(final int id);
    }

    public static interface IPathResponder {
        abstract Response respond(final int id, final String data);
    }

    // Handler context
    private Map<String, Collection<Method>> paths;
    private Map<String, IPathResponder> responders;
    protected String aspectName;
    protected Database db;
    protected DatabaseAspect<T> aspect;
    protected Gson gson;
    protected Class<T> cls;

    // Request context
    protected SessionAdapter session;
    protected String data;

    public AbstractRESTHandler(Class<T> cls, String aspectName) {
        this.cls = cls;
        this.aspectName = aspectName;
        this.gson = new Gson();
        this.db = Database.getDatabase();
        this.aspect = getDatabaseAspect();
        this.paths = getPaths();
        this.responders = getResponders();
        this.session = null;
        this.data = "";
    }

    public Map<String, Collection<Method>> getPaths() {
        final Map<String, Collection<Method>> paths = new HashMap<String, Collection<Method>>();
        paths.put(String.format("api/%s", aspectName), Arrays.asList(Method.POST, Method.GET));
        // If a parameter is used, it must be at the end or be followed by a '/'
        paths.put(String.format("api/%s/:id", aspectName), Arrays.asList(Method.GET, Method.PATCH, Method.DELETE));
        return paths;
    }

    private boolean isAuthorized(final String matchedUri, final Method method) {
        for (final Map.Entry<String, Collection<Method>> entry : paths.entrySet()) {
            Log.i("#dbl#", "path: " + matchedUri + ", test: " + entry.getKey());
            if (entry.getKey().equals(matchedUri)) {
                boolean r = entry.getValue().contains(method);
                Log.i("#dbl#", "" + r);
                return r;
            }
        }
        return false;
    }

    private Map<String, IPathResponder> getResponders() {
        final Map<String, IPathResponder> responders = new HashMap<String, IPathResponder>();
        responders.put(String.format("POST_api/%s", aspectName), new IPathResponder() {
            public Response respond(final int id, final String object) {
                return create(gson.fromJson(object, cls));
            }
        });
        responders.put(String.format("GET_api/%s/:id", aspectName), new IPathResponder() {
            public Response respond(final int id, final String object) {
                return get(id);
            }
        });
        responders.put(String.format("LIST_api/%s", aspectName), new IPathResponder() {
            public Response respond(final int id, final String object) {
                return list();
            }
        });
        responders.put(String.format("PUT_api/%s/:id", aspectName), new IPathResponder() {
            public Response respond(final int id, final String object) {
                return update(id, gson.fromJson(object, cls));
            }
        });
        responders.put(String.format("DELETE_api/%s/:id", aspectName), new IPathResponder() {
            public Response respond(final int id, final String object) {
                return delete(id);
            }
        });

        return responders;
    }

    protected void setResponder(final String operationId, final IPathResponder responder) {
        responders.put(operationId, responder);
    }

    abstract DatabaseAspect<T> getDatabaseAspect();

    Response create(final T object) {
        if (aspect.create(object)) {
            return ServerUtils.response(Status.CREATED, MESSAGE_SUCCESS);
        } else {
            return ServerUtils.response(Status.BAD_REQUEST, "Unable to create.");
        }
    }

    Response get(final int id) {
        final T object = aspect.get(id);
        if (object != null) {
            return ServerUtils.response(Status.OK, MESSAGE_SUCCESS, object);
        }
        return ServerUtils.response(Status.NOT_FOUND, "Not found.");
    }

    Response list() {
        return ServerUtils.response(Status.OK, "Success", aspect.getAll());
    }

    Response update(final int id, final T object) {
        if (aspect.update(id, object)) {
            return ServerUtils.response(Status.OK, MESSAGE_SUCCESS, object);
        } else {
            return ServerUtils.response(Status.BAD_REQUEST, "Unable to update.");
        }
    }

    Response delete(final int id) {
        if (aspect.delete(id)) {
            return ServerUtils.response(Status.OK, MESSAGE_SUCCESS);
        } else {
            return ServerUtils.response(Status.BAD_REQUEST, "Not found.");
        }
    }

    public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return checkAuthorization(session, urlParams, uriResource.getUri(), false, new AuthCallback() {
            public Response onAuthorization(final int id) {
                return callResponder(String.format("POST_%s", uriResource.getUri()), id, data);
            }
        });
    }

    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return checkAuthorization(session, urlParams, uriResource.getUri(), urlParams.containsKey("id"), new AuthCallback() {
            public Response onAuthorization(final int id) {
                if (id > 0) {
                    return callResponder(String.format("GET_%s", uriResource.getUri()), id, null);
                }
                return callResponder(String.format("LIST_%s", uriResource.getUri()), id, null);
            }
        });
    }

    public Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return checkAuthorization(session, urlParams, uriResource.getUri(), true, new AuthCallback() {
            public Response onAuthorization(final int id) {
                return callResponder(String.format("PUT_%s", uriResource.getUri()), id, data);
            }
        });
    }

    public Response other(String method, UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        if ("PATCH".equals(method)) {
            return checkAuthorization(session, urlParams, uriResource.getUri(), true, new AuthCallback() {
                public Response onAuthorization(final int id) {
                    return callResponder(String.format("PATCH_%s", uriResource.getUri()), id, data);
                }
            });
        }
        return ServerUtils.notFound();
    }

    public Response delete(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return checkAuthorization(session, urlParams, uriResource.getUri(), true, new AuthCallback() {
            public Response onAuthorization(final int id) {
                return callResponder(String.format("DELETE_%s", uriResource.getUri()), id, null);
            }
        });
    }

    private Response callResponder(final String operationId, final int id, final String requestData) {
        final IPathResponder responder = responders.get(operationId);
        Log.d(TAG, "operationId: " + operationId + " -> responder: " + responder);
        if (responder != null) {
            return responder.respond(id, requestData);
        }
        return ServerUtils.notFound();
    }

    private int parseIdFromParams(final Map<String, String> urlParams) {
        if (urlParams.containsKey("id")) {
            try {
                return Integer.parseInt(urlParams.get("id"));
            } catch (Exception e) {
                Log.e(TAG, "Unable to parse id: " + urlParams.get("id"));
            }
        }
        return -1;
    }

    private Response checkAuthorization(final IHTTPSession session, final Map<String, String> urlParams,
        final String matchedUri, final boolean withId, final AuthCallback cb) {
        prepare(session);
        if (!isAuthorized(matchedUri, session.getMethod())) {
            Log.e(TAG, "Endpoint " + session.getUri() + " is not authorized");
            return ServerUtils.notFound();
        }
        if (withId) {
            final int id = parseIdFromParams(urlParams);
            if (id > 0) {
                return cb.onAuthorization(id);
            }
            return ServerUtils.notFound();
        } else {
            return cb.onAuthorization(-1);
        }
    }

    protected String ifChange(final String newValue, final String oldValue) {
        return newValue == null ? oldValue : newValue;
    }

    protected <T> Set<T> ifChange(final Set<T> newValue, final Set<T> oldValue) {
        return newValue.isEmpty() ? oldValue : newValue;
    }

    protected Integer ifChange(final Integer newValue, final Integer oldValue) {
        return newValue == null ? oldValue : newValue;
    }

    protected void prepare(final IHTTPSession session) {
        Log.i(TAG, "Using " + aspectName + " handler to respond");
        try {
            this.session = (SessionAdapter) session;

            Integer contentLength = 0;
            try {
                contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
                Log.d(TAG, "Read Content-Length: " + contentLength);
            } catch (Exception e) {}
            byte[] buffer = new byte[contentLength];
            session.getInputStream().read(buffer, 0, contentLength);
            Log.d(TAG, "RequestBody: " + new String(buffer));
            this.data = new String(buffer);
        } catch (Exception e) {
            Log.e(TAG, "Unable to parse body: ", e);
            this.data = "";
        }
    }

    // Default implementation for NanoHTTPD

    @Override
    public String getText() {
        return "";
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public IStatus getStatus() {
        return Status.BAD_REQUEST;
    }
}
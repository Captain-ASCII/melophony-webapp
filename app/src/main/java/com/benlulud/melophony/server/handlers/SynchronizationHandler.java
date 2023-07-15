package com.benlulud.melophony.server.handlers;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.router.RouterNanoHTTPD.DefaultHandler;
import org.nanohttpd.router.RouterNanoHTTPD.UriResource;

import com.benlulud.melophony.api.client.ApiException;
import com.benlulud.melophony.api.interfaces.UserApi;
import com.benlulud.melophony.api.model.User;
import com.benlulud.melophony.database.Database;
import com.benlulud.melophony.database.Database.ISynchronizationListener;
import com.benlulud.melophony.database.DatabaseAspect;
import com.benlulud.melophony.database.Synchronizer;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.server.SessionAdapter;
import com.benlulud.melophony.server.sockets.DefaultSocket;
import com.benlulud.melophony.server.sockets.SocketCallbacks;
import com.benlulud.melophony.server.sockets.SocketHandler;


public class SynchronizationHandler extends DefaultHandler {

    private static final String TAG = SynchronizationHandler.class.getSimpleName();

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
        if ("/api/synchronization/capabilities".equals(session.getUri())) {
            try {
                final DatabaseAspect<User> userAspect = db.getUserAspect();
                final Collection<User> users = userAspect.getAll();
                if (users.size() > 0) {
                    final User user = new UserApi().userRead(users.toArray(new User[1])[0].getId());
                    return ServerUtils.response(Status.OK, "Able to synchronize");
                }
            } catch (ApiException e) {
                Log.e(TAG, "Error while trying to reconnect to server");
            }
            return ServerUtils.response(Status.FORBIDDEN, "Unable to connect anymore, must login again");
        } else {
            final SessionAdapter adaptedSession = (SessionAdapter) session;
            new Synchronizer().synchronize(db, adaptedSession.getSocketHandler());
            return super.get(uriResource, urlParams, session);
        }
    }
}
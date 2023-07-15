package com.benlulud.melophony.server.handlers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import android.util.Log;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import com.benlulud.melophony.api.client.ApiClient;
import com.benlulud.melophony.api.client.ApiException;
import com.benlulud.melophony.api.client.ApiResponse;
import com.benlulud.melophony.api.client.Configuration;
import com.benlulud.melophony.api.interfaces.UserApi;
import com.benlulud.melophony.api.model.User;
import com.benlulud.melophony.api.model.UserLoginRequest;
import com.benlulud.melophony.database.DatabaseAspect;
import com.benlulud.melophony.database.Synchronizer;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.webapp.Constants;


public class UserHandler extends AbstractRESTHandler<User> {

    private static final String TAG = UserHandler.class.getSimpleName();

    private UserApi userApi;

    public UserHandler() {
        super(User.class, "user");
        this.userApi = new UserApi();

        setResponder("PATCH_api/%s/:id", new IPathResponder() {
            public Response respond(final int id, final String data) {
                final User user = aspect.get(id);
                final User modifications = gson.fromJson(data, User.class);
                if (user != null) {
                    return update(id,
                        new User(user.getId())
                        .firstName(ifChange(modifications.getFirstName(), user.getFirstName()))
                        .lastName(ifChange(modifications.getLastName(), user.getLastName()))
                        .email(ifChange(modifications.getEmail(), user.getEmail()))
                    );
                }
                return ServerUtils.notFound();
            }
        });
        setResponder("POST_api/user/login", new IPathResponder() {
            public Response respond(final int id, final String data) {
                try {
                    final UserLoginRequest loginRequest = gson.fromJson(data, UserLoginRequest.class);
                    final ApiResponse<User> response = userApi.userLoginWithHttpInfo(loginRequest);
                    final Map<String, String> headers = ServerUtils.flattenHeaders(response);
                    final String token = headers.get(Constants.TOKEN_HEADER);
                    if (token != null) {
                        final ApiClient client = Configuration.getDefaultApiClient();
                        client.addDefaultHeader(Constants.AUTHORIZATION_HEADER, token);
                        db.saveUser(response.getData(), token);
                        new Synchronizer().synchronize(db, session.getSocketHandler());
                        return ServerUtils.proxyResponse(response);
                    }
                    return ServerUtils.response(Status.BAD_REQUEST, "Unable to get token from response");
                } catch (ApiException e) {
                    Log.e(TAG, "Unable to request login: ", e);
                    return ServerUtils.response(Status.BAD_REQUEST, "Unable to login");
                }
            }
        });
    }

    public Map<String, Collection<Method>> getPaths() {
        final Map<String, Collection<Method>> paths = super.getPaths();
        paths.put("api/user/login", Arrays.asList(Method.POST));
        return paths;
    }

    @Override
    public DatabaseAspect<User> getDatabaseAspect() {
        return db.getUserAspect();
    }
}
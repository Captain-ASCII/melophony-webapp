package com.benlulud.melophony.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.util.Log;

import okhttp3.Call;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import com.benlulud.melophony.api.client.ApiCallback;
import com.benlulud.melophony.api.client.ApiClient;
import com.benlulud.melophony.api.client.ApiException;
import com.benlulud.melophony.api.client.ApiResponse;
import com.benlulud.melophony.api.client.Configuration;
import com.benlulud.melophony.api.client.Pair;
import com.benlulud.melophony.database.Database.SynchronizationItem;


public class ServerUtils {

    private static final String TAG = ServerUtils.class.getSimpleName();

    public static Map<String, String> flattenHeaders(final ApiResponse apiResponse) {
        final Map<String, String> flattenedHeaders = new HashMap<String, String>();
        final Map<String, List<String>> headers = apiResponse.getHeaders();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            flattenedHeaders.put(entry.getKey(), String.join(",", entry.getValue()));
        }
        return flattenedHeaders;
    }

    public static Response notFound() {
        return response(Status.NOT_FOUND, "Not found.");
    }

    public static Response response(final IStatus status, final String message) {
        return response(status, message, null);
    }

    public static Response response(final IStatus status, final String message, final Object data) {
        return response(status, message, data, null);
    }

    public static Response proxyResponse(final ApiResponse apiResponse) {
        final Map<String, String> flattenedHeaders = ServerUtils.flattenHeaders(apiResponse);
        return response(Status.lookup(apiResponse.getStatusCode()), flattenedHeaders.get("message"), apiResponse.getData(), flattenedHeaders);
    }

    public static Response response(final IStatus status, final String message, final Object data, final Map<String, String> headers) {
        return response(status, message, new Gson().toJson(data), headers);
    }

    public static Response response(final IStatus status, final String message, final String dataString, final Map<String, String> headers) {
        final Response response = Response.newFixedLengthResponse(status, "application/json", dataString);
        response.addHeader("Message", message);
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (!"content-length".equals(header.getKey())) {
                    response.addHeader(header.getKey(), header.getValue());
                }
            }
        }
        return response;
    }

    public static Response fileResponse(final File file) {
        try {
            if (file.exists()) {
                return Response.newChunkedResponse(Status.OK, "image/webp", new FileInputStream(file));
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to send file: ", e);
        }
        return ServerUtils.notFound();
    }

    public static String downloadImageIfRequired(final String url, final File directory, final String previousUrl, final String previousImageName) {
        if (url == null || url.equals("") || url.equals(previousUrl)) {
            return previousImageName;
        }
        try {
            final String imageName = UUID.randomUUID().toString();
            final ApiClient apiClient = Configuration.getDefaultApiClient();
            final ApiResponse<File> result = apiClient.execute(getCall(apiClient, url), new TypeToken<File>(){}.getType());
            final File resultFile = result.getData();
            if (resultFile != null &&  resultFile.exists()) {
                if (previousImageName != null) {
                    final File previousImage = new File(directory, previousImageName);
                    if (previousImage.exists()) {
                        previousImage.delete();
                    }
                }
                if (resultFile.renameTo(new File(directory, imageName))) {
                    return imageName;
                }
            }
        } catch (ApiException e) {
            Log.e(TAG, "Exception while downloading image: ", e);
        }
        return null;
    }

    private static Call getCall(final ApiClient apiClient, final String url) throws ApiException {
        return apiClient.buildCall(url, "", "GET", new ArrayList<Pair>(), new ArrayList<Pair>(), null, new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, Object>(), new String[] {}, null);
    }
}
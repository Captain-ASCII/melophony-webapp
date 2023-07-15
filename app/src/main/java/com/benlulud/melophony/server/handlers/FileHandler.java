package com.benlulud.melophony.server.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import android.util.Log;

import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.http.request.Method;

import com.benlulud.melophony.api.interfaces.FileApi;
import com.benlulud.melophony.api.model.ModelFile;
import com.benlulud.melophony.database.DatabaseAspect;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.webapp.Constants;


public class FileHandler extends AbstractRESTHandler<ModelFile> {

    private static final String TAG = FileHandler.class.getSimpleName();

    private FileApi fileApi;

    public FileHandler() {
        super(ModelFile.class, "file");
        this.fileApi = new FileApi();

        setResponder("GET_api/file/:id/download", new IPathResponder() {
            public Response respond(final int id, final String data) {
                final File tracksDir = new File(session.getContext().getFilesDir(), Constants.TRACKS_DIR);
                final ModelFile file = aspect.get(id);
                if (file != null && tracksDir.exists()) {
                    try {
                        return Response.newChunkedResponse(Status.OK, "audio/x-m4a", new FileInputStream(new File(tracksDir, file.getFileId() + ".m4a")));
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to send file: ", e);
                    }
                }
                return ServerUtils.notFound();
            }
        });
    }

    public Map<String, Collection<Method>> getPaths() {
        final Map<String, Collection<Method>> paths = super.getPaths();
        paths.put("api/file/:id/download", Arrays.asList(Method.GET));
        return paths;
    }

    @Override
    public DatabaseAspect<ModelFile> getDatabaseAspect() {
        return db.getFileAspect();
    }
}
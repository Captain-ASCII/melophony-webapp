package com.benlulud.melophony.server.handlers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import android.util.Log;

import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;

import com.benlulud.melophony.api.client.ApiException;
import com.benlulud.melophony.api.client.ApiException;
import com.benlulud.melophony.api.interfaces.ArtistApi;
import com.benlulud.melophony.api.model.Artist;
import com.benlulud.melophony.database.DatabaseAspect;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.webapp.Constants;


public class ArtistHandler extends AbstractRESTHandler<Artist> {

    private static final String TAG = ArtistHandler.class.getSimpleName();

    private ArtistApi artistApi;

    public ArtistHandler() {
        super(Artist.class, "artist");
        this.artistApi = new ArtistApi();

        setResponder("PATCH_api/artist/:id", new IPathResponder() {
            public Response respond(final int id, final String data) {
                final Artist artist = aspect.get(id);
                final Artist modifications = gson.fromJson(data, Artist.class);
                if (artist != null && modifications != null) {
                    return update(id,
                        new Artist(artist.getUser()).id(artist.getId())
                        .name(ifChange(modifications.getName(), artist.getName()))
                        .imageUrl(ifChange(modifications.getImageUrl(), artist.getImageUrl()))
                        .imageName(ifChange(modifications.getImageName(), artist.getImageName()))
                    );
                }
                return ServerUtils.notFound();
            }
        });
        setResponder("GET_api/artist/:id/image/:imageName", new IPathResponder() {
            public Response respond(final int id, final String data) {
                final Artist artist = aspect.get(id);
                if (artist != null && artist.getImageName() != null) {
                    return ServerUtils.fileResponse(db.getFile(Constants.ARTIST_IMAGES_DIR, artist.getImageName()));
                }
                return ServerUtils.notFound();
            }
        });
    }

    public Map<String, Collection<Method>> getPaths() {
        final Map<String, Collection<Method>> paths = super.getPaths();
        paths.put("api/artist/:id/image/:imageName", Arrays.asList(Method.GET));
        return paths;
    }

    @Override
    public DatabaseAspect<Artist> getDatabaseAspect() {
        return db.getArtistAspect();
    }
}
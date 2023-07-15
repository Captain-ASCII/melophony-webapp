package com.benlulud.melophony.server.handlers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import android.util.Log;

import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import com.benlulud.melophony.api.interfaces.PlaylistApi;
import com.benlulud.melophony.api.model.Playlist;
import com.benlulud.melophony.database.DatabaseAspect;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.webapp.Constants;


public class PlaylistHandler extends AbstractRESTHandler<Playlist> {

    private static final String TAG = PlaylistHandler.class.getSimpleName();

    private PlaylistApi playlistApi;

    public PlaylistHandler() {
        super(Playlist.class, "playlist");
        this.playlistApi = new PlaylistApi();

        setResponder("PATCH_api/playlist/:id", new IPathResponder() {
            public Response respond(final int id, final String data) {
                final Playlist playlist = aspect.get(id);
                final Playlist modifications = gson.fromJson(data, Playlist.class);
                if (playlist != null && modifications != null) {
                    return update(id,
                        new Playlist(playlist.getId(), ifChange(modifications.getTracks(), playlist.getTracks()), playlist.getUser())
                        .name(ifChange(modifications.getName(), playlist.getName()))
                        .imageUrl(ifChange(modifications.getImageUrl(), playlist.getImageUrl()))
                        .imageName(ifChange(modifications.getImageName(), playlist.getImageName()))
                    );
                }
                return ServerUtils.notFound();
            }
        });
        setResponder("GET_api/playlist/:id/image", new IPathResponder() {
            public Response respond(final int id, final String data) {
                final Playlist playlist = aspect.get(id);
                if (playlist != null && playlist.getImageName() != null) {
                    return ServerUtils.fileResponse(db.getFile(Constants.PLAYLIST_IMAGES_DIR, playlist.getImageName()));
                }
                return ServerUtils.notFound();
            }
        });
    }

    public Map<String, Collection<Method>> getPaths() {
        final Map<String, Collection<Method>> paths = super.getPaths();
        paths.put("api/playlist/:id/image", Arrays.asList(Method.GET));
        return paths;
    }

    @Override
    public DatabaseAspect<Playlist> getDatabaseAspect() {
        return db.getPlaylistAspect();
    }
}
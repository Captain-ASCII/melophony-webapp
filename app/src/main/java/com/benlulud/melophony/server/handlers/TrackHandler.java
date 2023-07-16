package com.benlulud.melophony.server.handlers;

import java.util.Map;

import android.util.Log;

import org.nanohttpd.protocols.http.response.Response;

import com.benlulud.melophony.api.interfaces.TrackApi;
import com.benlulud.melophony.api.model.Track;
import com.benlulud.melophony.database.DatabaseAspect;
import com.benlulud.melophony.server.ServerUtils;


public class TrackHandler extends AbstractRESTHandler<Track> {

    private static final String TAG = UserHandler.class.getSimpleName();

    private TrackApi trackApi;

    public TrackHandler() {
        super(Track.class, "track");
        this.trackApi = new TrackApi();

        setResponder("PATCH_api/track/:id", new IPathResponder() {
            public Response respond(final int id, final String data) {
                final Track track = aspect.get(id);
                final Track modifications = gson.fromJson(data, Track.class);
                if (track != null && modifications != null) {
                    return update(id,
                        new Track(track.getCreationDate(), track.getLastPlay(), track.getUser()).id(track.getId())
                        .title(ifChange(modifications.getTitle(), track.getTitle()))
                        ._file(ifChange(modifications.getFile(), track.getFile()))
                        .artists(ifChange(modifications.getArtists(), track.getArtists()))
                        .duration(ifChange(modifications.getDuration(), track.getDuration()))
                        .startTime(ifChange(modifications.getStartTime(), track.getStartTime()))
                        .endTime(ifChange(modifications.getEndTime(), track.getEndTime()))
                        .playCount(ifChange(modifications.getPlayCount(), track.getPlayCount()))
                        .rating(ifChange(modifications.getRating(), track.getRating()))
                    );
                }
                return ServerUtils.notFound();
            }
        });
    }

    @Override
    public DatabaseAspect<Track> getDatabaseAspect() {
        return db.getTrackAspect();
    }
}
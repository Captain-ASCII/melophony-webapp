package com.benlulud.melophony.server.handlers;

import java.util.Map;

import android.util.Log;

import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import com.benlulud.melophony.api.client.ApiException;
import com.benlulud.melophony.api.client.ApiResponse;
import com.benlulud.melophony.api.interfaces.ArtistApi;
import com.benlulud.melophony.api.interfaces.FileApi;
import com.benlulud.melophony.api.interfaces.TrackApi;
import com.benlulud.melophony.api.model.ModelFile;
import com.benlulud.melophony.api.model.Track;
import com.benlulud.melophony.database.DatabaseAspect;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.webapp.Constants;


public class TrackHandler extends AbstractRESTHandler<Track> {

    private static final String TAG = UserHandler.class.getSimpleName();

    private TrackApi trackApi;
    private ArtistApi artistApi;
    private FileApi fileApi;

    public TrackHandler() {
        super(Track.class, "track");
        this.trackApi = new TrackApi();
        this.artistApi = new ArtistApi();
        this.fileApi = new FileApi();

        setResponder("POST_api/track", new IPathResponder() {
            public Response respond(final int id, final String data) {
                try {
                    final ApiResponse<Track> response = trackApi.trackCreateWithHttpInfo(file, data);
                    final Track createdTrack = response.getData();

                    final ModelFile fileData = fileApi.fileRead(createdTrack.getFile());
                    db.getFileAspect().insert(fileData);
                    file.renameTo(db.getFile(Constants.TRACKS_KEY, fileData.getFileId() + ".m4a"));

                    for (final int artistId : createdTrack.getArtists()) {
                        db.getArtistAspect().insert(artistApi.artistRead(artistId));
                    }

                    aspect.insert(createdTrack);

                    return ServerUtils.response(Status.CREATED, "Track created (remotely & locally)", createdTrack);
                } catch (ApiException e) {
                    Log.e(TAG, "Unable to create Track: ", e);
                }
                return ServerUtils.response(Status.INTERNAL_ERROR, "Unable to create track on server side");
            }
        });
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
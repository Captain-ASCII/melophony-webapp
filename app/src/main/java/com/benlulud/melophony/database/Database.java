package com.benlulud.melophony.database;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.benlulud.melophony.api.client.ApiException;
import com.benlulud.melophony.api.interfaces.ArtistApi;
import com.benlulud.melophony.api.interfaces.FileApi;
import com.benlulud.melophony.api.interfaces.PlaylistApi;
import com.benlulud.melophony.api.interfaces.TrackApi;
import com.benlulud.melophony.api.model.Artist;
import com.benlulud.melophony.api.model.ModelFile;
import com.benlulud.melophony.api.model.Playlist;
import com.benlulud.melophony.api.model.Track;
import com.benlulud.melophony.api.model.User;
import com.benlulud.melophony.webapp.Constants;


public class Database {

    private static final String TAG = Database.class.getSimpleName();
    private static Database INSTANCE;

    public final static Database getDatabase(final Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Database(context);
        }
        return INSTANCE;
    }

    public final static Database getDatabase() {
        return getDatabase(null);
    }

    public interface ISynchronizationListener {
        void onItemSynchronized(SynchronizationItem modifiedItem);
    }

    private interface IListSynchronizer<T> {
        List<T> getRemoteList() throws ApiException;
    }

    private interface IDownloader<T> {
        File getTargetFile(T object);
        File download(T object) throws ApiException;
    }

    private Context context;
    private Gson gson;
    private SharedPreferences sharedPrefs;
    private Map<String, SynchronizationItem> synchronizationState;
    private Thread synchronizationThread;

    private DatabaseAspect<User> users;
    private DatabaseAspect<Artist> artists;
    private DatabaseAspect<ModelFile> files;
    private DatabaseAspect<Playlist> playlists;
    private DatabaseAspect<Track> tracks;

    private ArtistApi artistApi;
    private FileApi fileApi;
    private TrackApi trackApi;
    private PlaylistApi playlistApi;

    private Database(final Context context) {
        this.context = context;
        this.gson = new Gson();
        this.sharedPrefs = context.getSharedPreferences(Constants.MELOPHONY_APP_KEY, 0);
        this.synchronizationState = new LinkedHashMap<String, SynchronizationItem>();

        this.users = new DatabaseAspect<User>(getPersistedData(Constants.USER_KEY, new TypeToken<TreeMap<Integer, User>>(){}));
        this.artists = new DatabaseAspect<Artist>(getPersistedData(Constants.ARTISTS_KEY, new TypeToken<TreeMap<Integer, Artist>>(){}));
        this.files = new DatabaseAspect<ModelFile>(getPersistedData(Constants.FILES_KEY, new TypeToken<TreeMap<Integer, ModelFile>>(){}));
        this.playlists = new DatabaseAspect<Playlist>(getPersistedData(Constants.PLAYLISTS_KEY, new TypeToken<TreeMap<Integer, Playlist>>(){}));
        this.tracks = new DatabaseAspect<Track>(getPersistedData(Constants.TRACKS_KEY, new TypeToken<TreeMap<Integer, Track>>(){}));

        this.artistApi = new ArtistApi();
        this.fileApi = new FileApi();
        this.trackApi = new TrackApi();
        this.playlistApi = new PlaylistApi();
    }

    private <T> TreeMap<Integer, T> getPersistedData(final String key, final TypeToken<TreeMap<Integer, T>> typeToken) {
        final String jsonData = getPersistedData(key);
        if (jsonData == null) {
            return null;
        }
        try {
            return gson.fromJson(jsonData, typeToken.getType());
        } catch(Exception e) {
            Log.w(TAG, "Unable to deserialize data: " + jsonData);
            return null;
        }
    }

    public String getPersistedData(final String key) {
        return sharedPrefs.getString(key, null);
    }

    private <T extends IModel> void persistData(final String key, final DatabaseAspect<T> aspect) {
        persistData(key, gson.toJson(aspect.getMap()));
    }

    private void persistData(final String key, final String value) {
        final Editor editor = sharedPrefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public File getFile(final String directoryName, final String fileName) {
        final File directory = new File(context.getFilesDir(), directoryName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return new File(directory, fileName);
    }


    public DatabaseAspect<User> getUserAspect() {
        return this.users;
    }

    public DatabaseAspect<Artist> getArtistAspect() {
        return this.artists;
    }

    public DatabaseAspect<ModelFile> getFileAspect() {
        return this.files;
    }

    public DatabaseAspect<Playlist> getPlaylistAspect() {
        return this.playlists;
    }

    public DatabaseAspect<Track> getTrackAspect() {
        return this.tracks;
    }


    public void saveUser(final User user, final String token) {
        users.clear();
        users.insert(user);
        persistData(Constants.TOKEN_KEY, token);
        persistData(Constants.USER_KEY, users);
    }

    public void synchronize(final ISynchronizationListener listener) {
        boolean result = true;
        Log.i(TAG, "Start synchronization");

        result &= synchronizeAspect(Constants.ARTISTS_KEY, artists, new IListSynchronizer<Artist>() {
            public List<Artist> getRemoteList() throws ApiException {
                return artistApi.artistList();
            }
        });
        result &= synchronizeAspect(Constants.FILES_KEY, files, new IListSynchronizer<ModelFile>() {
            public List<ModelFile> getRemoteList() throws ApiException {
                return fileApi.fileList();
            }
        });
        result &= synchronizeAspect(Constants.TRACKS_KEY, tracks, new IListSynchronizer<Track>() {
            public List<Track> getRemoteList() throws ApiException {
                return trackApi.trackList();
            }
        });
        result &= synchronizeAspect(Constants.PLAYLISTS_KEY, playlists, new IListSynchronizer<Playlist>() {
            public List<Playlist> getRemoteList() throws ApiException {
                return playlistApi.playlistList();
            }
        });
        Log.i(TAG, "Synchronization result: " + result);

        for (final Playlist playlist : playlists.getAll()) {
            synchronizationState.put(getKey(Constants.PLAYLISTS_KEY, playlist), new SynchronizationItem("'" + playlist.getName() + "' image", false));
        }
        for (final Artist artist : artists.getAll()) {
            synchronizationState.put(getKey(Constants.ARTISTS_KEY, artist), new SynchronizationItem("'" + artist.getName() + "' image", false));
        }
        for (final ModelFile file : files.getAll()) {
            synchronizationState.put(getKey(Constants.FILES_KEY, file), new SynchronizationItem("'" + file.getFileId() + "' track", false));
        }
        startFileSynchronization(listener);
    }

    private String getKey(final String type, final IModel object) {
        return type + "_" + object.getId();
    }

    private SynchronizationItem markSynchronized(final String type, final IModel object, final boolean result) {
        final String key = getKey(type, object);
        final SynchronizationItem modifiedItem = synchronizationState.get(key).markSynchronized(result);
        synchronizationState.put(key, modifiedItem);
        return modifiedItem;
    }

    private void startFileSynchronization(final ISynchronizationListener listener) {
        synchronizationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "File synchronization");
                try {
                    synchronizeFiles(playlists, listener, Constants.PLAYLISTS_KEY, new IDownloader<Playlist>() {
                        public File getTargetFile(final Playlist playlist) {
                            if (playlist.getImageName() != null) {
                                return getFile(Constants.PLAYLIST_IMAGES_DIR, playlist.getImageName());
                            }
                            return null;
                        }
                        public File download(final Playlist playlist) throws ApiException {
                            return playlistApi.playlistImage(playlist.getId());
                        }
                    });
                    synchronizeFiles(artists, listener, Constants.ARTISTS_KEY, new IDownloader<Artist>() {
                        public File getTargetFile(final Artist artist) {
                            if (artist.getImageName() != null) {
                                return getFile(Constants.ARTIST_IMAGES_DIR, artist.getImageName());
                            }
                            return null;
                        }
                        public File download(final Artist artist) throws ApiException {
                            return artistApi.artistImage(artist.getId());
                        }
                    });
                    synchronizeFiles(files, listener, Constants.FILES_KEY, new IDownloader<ModelFile>() {
                        public File getTargetFile(final ModelFile file) {
                            if (file.getFileId() != null) {
                                return getFile(Constants.TRACKS_DIR, file.getFileId());
                            }
                            return null;
                        }
                        public File download(final ModelFile file) throws ApiException {
                            return fileApi.fileDownload(file.getId(), true);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Unauthorized to download files, aborting...");
                }
            }
        });
        synchronizationThread.start();
    }

    private <T extends IModel> void synchronizeFiles(final DatabaseAspect<T> aspect, final ISynchronizationListener listener,
        final String type, final IDownloader<T> downloader) throws Exception {
        int nbRetries = 0;
        for (final T object : aspect.getAll()) {
            boolean result = false;
            try {
                final File targetFile = downloader.getTargetFile(object);
                if (targetFile != null && !targetFile.exists()) {
                    final File downloadedFile = downloader.download(object);
                    if (downloadedFile != null) {
                        downloadedFile.renameTo(targetFile);
                    }
                }
                result = true;
            } catch (ApiException e) {
                Log.e(TAG, "Unable to request image: ", e);
                nbRetries++;
                if (nbRetries > 5) {
                    Log.e(TAG, e.getMessage());
                    throw new Exception(e.getMessage());
                }
                if (e.getCode() == 403) {
                    throw new Exception("Not authorized");
                }
            }
            listener.onItemSynchronized(markSynchronized(type, object, result));
        }
    }

    private <T extends IModel> boolean synchronizeAspect(final String key, final DatabaseAspect<T> aspect, final IListSynchronizer<T> listSynchronizer) {
        boolean result = true;
        Collection<T> objectList = aspect.getAll();
        try {
            aspect.bulkInsert(listSynchronizer.getRemoteList());
            persistData(key, aspect);
            Log.i(TAG, "Synchronization for '" + key + "' is successful.");
            result = true;
        } catch (ApiException e) {
            Log.e(TAG, "Unable to request objects: ", e);
            result = false;
        }
        synchronizationState.put(key, new SynchronizationItem(key, true).markSynchronized(result));
        return result;
    }

    public Map<String, SynchronizationItem> getSynchronizationState() {
        return synchronizationState;
    }

    public static class SynchronizationItem {
        private String name;
        private boolean isSynchronized;
        private boolean isSuccessfullySynchronized;

        public SynchronizationItem(final String name, final boolean isSynchronized) {
            this.name = name;
            this.isSynchronized = isSynchronized;
            this.isSuccessfullySynchronized = false;
        }

        public String getName() {
            return name;
        }

        public boolean isSynchronized() {
            return isSynchronized;
        }

        public boolean isSuccessfullySynchronized() {
            return isSuccessfullySynchronized;
        }

        public SynchronizationItem markSynchronized(final boolean result) {
            this.isSynchronized = true;
            this.isSuccessfullySynchronized = result;
            return this;
        }
    }
}
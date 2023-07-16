package com.benlulud.melophony.database;

import java.util.Collection;

import com.google.gson.Gson;

import com.benlulud.melophony.database.Database.SynchronizationItem;
import com.benlulud.melophony.server.ServerUtils;
import com.benlulud.melophony.server.sockets.DefaultSocket;
import com.benlulud.melophony.server.sockets.SocketCallbacks;
import com.benlulud.melophony.server.sockets.SocketHandler;

public class Synchronizer {

    private DefaultSocket socket;
    private boolean synchroCompleted;

    public Synchronizer() {
        this.socket = null;
        this.synchroCompleted = false;
    }

    public void synchronize(final Database db, final SocketHandler socketHandler) {
        socketHandler.setSocketCallbacks(new SocketCallbacks() {
            public void onOpen(final DefaultSocket openedSocket) {
                socket = openedSocket;
                socket.sendMessage(new Gson().toJson(new Modification(synchroCompleted, db.getSynchronizationState().values(), null)));
            }
        });
        db.synchronize(new Database.ISynchronizationListener() {

            public void onItemSynchronized(final SynchronizationItem item) {
                if (socket != null) {
                    socket.sendMessage(new Gson().toJson(new Modification(synchroCompleted, db.getSynchronizationState().values(), item)));
                }
            }
            public void onSynchronizationCompleted() {
                synchroCompleted = true;
                if (socket != null) {
                    socket.sendMessage(new Gson().toJson(new Modification(synchroCompleted, db.getSynchronizationState().values(), null)));
                }
            }
        });
    }

    public static class Modification {
        private boolean synchronizationCompleted;
        private Collection<SynchronizationItem> synchronizationState;
        private SynchronizationItem modifiedItem;

        public Modification(final boolean isCompleted, final Collection<SynchronizationItem> synchronizationState, final SynchronizationItem modifiedItem) {
            this.synchronizationCompleted = isCompleted;
            this.synchronizationState = synchronizationState;
            this.modifiedItem = modifiedItem;
        }
    }
}
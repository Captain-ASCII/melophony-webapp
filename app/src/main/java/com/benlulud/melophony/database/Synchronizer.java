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

    public Synchronizer() {
        this.socket = null;
    }

    public void synchronize(final Database db, final SocketHandler socketHandler) {
        socketHandler.setSocketCallbacks(new SocketCallbacks() {
            public void onOpen(final DefaultSocket openedSocket) {
                socket = openedSocket;
                socket.sendMessage(new Gson().toJson(new Modification(db.getSynchronizationState().values(), null)));
            }
        });
        db.synchronize(new Database.ISynchronizationListener() {
            public void onItemSynchronized(final SynchronizationItem item) {
                if (socket != null) {
                    socket.sendMessage(new Gson().toJson(new Modification(db.getSynchronizationState().values(), item)));
                }
            }
        });
    }

    public static class Modification {
        private Collection<SynchronizationItem> synchronizationState;
        private SynchronizationItem modifiedItem;

        public Modification(final Collection<SynchronizationItem> synchronizationState, final SynchronizationItem modifiedItem) {
            this.synchronizationState = synchronizationState;
            this.modifiedItem = modifiedItem;
        }
    }
}
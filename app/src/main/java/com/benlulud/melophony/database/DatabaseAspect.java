package com.benlulud.melophony.database;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import com.google.gson.reflect.TypeToken;

import com.benlulud.melophony.api.model.SynchronizationRequestInner;
import com.benlulud.melophony.api.model.SynchronizationRequestInner.ModificationTypeEnum;
import com.benlulud.melophony.api.model.SynchronizationRequestInner.ObjectTypeEnum;


public class DatabaseAspect<T extends IModel> {

    private static final String TAG = DatabaseAspect.class.getSimpleName();

    private Database database;
    private String type;
    private String aspectKey;
    private TreeMap<Integer, T> objects;
    private List<SynchronizationRequestInner> modifications;

    public DatabaseAspect(final Database database, final String type, final String aspectKey, final TypeToken typeToken) {
        this.database = database;
        this.type = type;
        this.aspectKey = aspectKey;
        final TreeMap<Integer, T> persistedObjects = database.getPersistedData(aspectKey, typeToken);
        this.objects = (persistedObjects == null) ? new TreeMap<Integer, T>() : persistedObjects;
        this.modifications = database.getPersistedModifications(aspectKey);
    }

    public T create(final T object) {
        try {
            final int localId = this.objects.size() + 1;
            object.setId(localId);
            this.objects.put(localId, object);
            modifications.add(modification(ModificationTypeEnum.CREATION, type, object));
            database.persistData(aspectKey, this);
            return object;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean bulkInsert(final Collection<T> objects) {
        for (T object : objects) {
            if (!insert(object)) {
                return false;
            }
        }
        return true;
    }

    public boolean insert(final T object) {
        try {
            this.objects.put(object.getId(), object);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public T get(final int id) {
        if (this.objects.containsKey(id)) {
            return this.objects.get(id);
        }
        return null;
    }

    public TreeMap<Integer, T> getMap() {
        return this.objects;
    }

    public Collection<T> getAll() {
        return this.objects.values();
    }

    public boolean update(final int id, final T object) {
        if (this.objects.containsKey(id)) {
            this.objects.put(id, object);
            modifications.add(modification(ModificationTypeEnum.UPDATE, type, object));
            database.persistData(aspectKey, this);
            return true;
        }
        return false;
    }

    public boolean delete(final int id) {
        if (this.objects.containsKey(id)) {
            final T removedObject = this.objects.remove(id);
            modifications.add(modification(ModificationTypeEnum.DELETION, type, removedObject));
            database.persistData(aspectKey, this);
            return true;
        }
        return false;
    }

    public void clear() {
        this.objects.clear();
        this.modifications.clear();
    }

    private SynchronizationRequestInner modification(final ModificationTypeEnum opType, final String objType, final T object) {
        return new SynchronizationRequestInner()
        .modificationType(opType)
        .objectType(ObjectTypeEnum.fromValue(objType))
        .modificationDateTime(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
        .modificationData(object);
    }

    List<SynchronizationRequestInner> getModifications() {
        return this.modifications;
    }

    void clearModifications() {
        this.modifications.clear();
    }
}
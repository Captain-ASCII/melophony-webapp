package com.benlulud.melophony.database;

import java.util.Collection;
import java.util.TreeMap;
import java.util.List;
import java.util.Set;

import android.util.Log;


public class DatabaseAspect<T extends IModel> {

    private static final String TAG = DatabaseAspect.class.getSimpleName();
    private TreeMap<Integer, T> objects;

    public DatabaseAspect(final TreeMap<Integer, T> persistedObjects) {
        this.objects = (persistedObjects == null) ? new TreeMap<Integer, T>() : persistedObjects;
    }

    public boolean create(final T object) {
        try {
            this.objects.put(this.objects.size() + 1, object);
            return true;
        } catch (Exception e) {
            return false;
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
            return true;
        }
        return false;
    }

    public boolean delete(final int id) {
        if (this.objects.containsKey(id)) {
            this.objects.remove(id);
            return true;
        }
        return false;
    }

    public void clear() {
        this.objects.clear();
    }
}
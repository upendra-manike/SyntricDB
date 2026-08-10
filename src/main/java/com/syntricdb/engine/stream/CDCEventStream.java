package com.syntricdb.engine.stream;

import com.syntricdb.engine.schema.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Change Data Capture (CDC) engine capturing real-time mutation events (INSERT, UPDATE, DELETE)
 * and broadcasting them to stream subscribers.
 */
public class CDCEventStream {
    private static final Logger log = LoggerFactory.getLogger(CDCEventStream.class);

    public enum EventType {
        INSERT,
        UPDATE,
        DELETE
    }

    public static class CDCEvent {
        private final String database;
        private final String table;
        private final EventType type;
        private final String key;
        private final Tuple tuple;
        private final long timestamp;

        public CDCEvent(String database, String table, EventType type, String key, Tuple tuple) {
            this.database = database;
            this.table = table;
            this.type = type;
            this.key = key;
            this.tuple = tuple;
            this.timestamp = System.currentTimeMillis();
        }

        public String getDatabase() { return database; }
        public String getTable() { return table; }
        public EventType getType() { return type; }
        public String getKey() { return key; }
        public Tuple getTuple() { return tuple; }
        public long getTimestamp() { return timestamp; }
    }

    public interface CDCListener {
        void onEvent(CDCEvent event);
    }

    private final List<CDCListener> listeners = new CopyOnWriteArrayList<>();

    public void registerListener(CDCListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(CDCListener listener) {
        listeners.remove(listener);
    }

    public void publishEvent(String database, String table, EventType type, String key, Tuple tuple) {
        CDCEvent event = new CDCEvent(database, table, type, key, tuple);
        log.debug("CDC Event published: {} on {}.{} key={}", type, database, table, key);

        for (CDCListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("Error dispatching CDC event to listener", e);
            }
        }
    }

    public int getListenerCount() {
        return listeners.size();
    }
}

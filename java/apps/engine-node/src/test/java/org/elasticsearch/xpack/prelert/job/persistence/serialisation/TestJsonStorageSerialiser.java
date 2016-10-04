
package org.elasticsearch.xpack.prelert.job.persistence.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * An implementation of StorageSerialiser to facilitate
 * unit testing of {@link StorageSerialisable#serialise(StorageSerialiser)} implementations.
 * <p>
 * It serialises to JSON which can be retrieved calling {@link #toJson()}.
 */
public class TestJsonStorageSerialiser implements StorageSerialiser {
    private static final String TIMESTAMP_NAME = "@timestamp";

    private final Deque<Object> objectStack;
    private String json;

    public TestJsonStorageSerialiser() {
        objectStack = new ArrayDeque<>();
    }

    @Override
    public StorageSerialiser startObject() throws IOException {
        objectStack.push(new ObjectHolder());
        return this;
    }

    @Override
    public StorageSerialiser startObject(String fieldName) throws IOException {
        objectStack.push(new ObjectHolder(fieldName));
        return this;
    }

    @Override
    public StorageSerialiser endObject() throws IOException {
        Object object = objectStack.pop();
        checkState(object instanceof ObjectHolder, "endObject() was called while object was not started");
        ObjectHolder objectHolder = (ObjectHolder) object;
        if (objectStack.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            objectMapper.writeValue(out, objectHolder.map);
            json = new String(out.toByteArray(), StandardCharsets.UTF_8);
        } else {
            Object parent = objectStack.peek();
            if (parent instanceof ObjectHolder) {
                checkState(objectHolder.name != null);
                ObjectHolder parentObjectHolder = (ObjectHolder) parent;
                parentObjectHolder.map.put(objectHolder.name, objectHolder.map);
            } else if (parent instanceof ListHolder) {
                checkState(objectHolder.name == null);
                ListHolder parentListHolder = (ListHolder) parent;
                parentListHolder.list.add(objectHolder.map);
            } else {
                throw new IllegalStateException();
            }
        }

        return this;
    }

    @Override
    public StorageSerialiser startList(String name) throws IOException {
        objectStack.push(new ListHolder(name));
        return this;
    }

    @Override
    public StorageSerialiser endList() throws IOException {
        Object object = objectStack.pop();
        checkState(object instanceof ListHolder, "endList() was called while list was not started");
        ListHolder listHolder = (ListHolder) object;
        peekObjectHolder().map.put(listHolder.name, listHolder.list);
        return this;
    }

    @Override
    public StorageSerialiser addTimestamp(Date value) throws IOException {
        peekObjectHolder().map.put(TIMESTAMP_NAME, value);
        return this;
    }

    private ObjectHolder peekObjectHolder() {
        Object next = objectStack.peek();
        if (next instanceof ObjectHolder) {
            return (ObjectHolder) next;
        } else {
            throw new IllegalStateException("Expected an ObjectHolder; found a " + next + " instead");
        }
    }

    @Override
    public StorageSerialiser add(String name, Object value) throws IOException {
        peekObjectHolder().map.put(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, String value) throws IOException {
        peekObjectHolder().map.put(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, int value) throws IOException {
        peekObjectHolder().map.put(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, long value) throws IOException {
        peekObjectHolder().map.put(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, double value) throws IOException {
        peekObjectHolder().map.put(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, boolean value) throws IOException {
        peekObjectHolder().map.put(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, String... values) throws IOException {
        peekObjectHolder().map.put(name, values);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, double... values) throws IOException {
        peekObjectHolder().map.put(name, values);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, Map<String, Object> map) throws IOException {
        peekObjectHolder().map.put(name, map);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, Collection<? extends StorageSerialisable> values)
            throws IOException {
        startList(name);
        for (StorageSerialisable value : values) {
            startObject();
            serialise(value);
            endObject();
        }
        endList();
        return this;
    }

    @Override
    public StorageSerialiser serialise(StorageSerialisable value) throws IOException {
        value.serialise(this);
        return this;
    }

    @Override
    public DotNotationReverser newDotNotationReverser() {
        return new JsonDotNotationReverser();
    }

    public String toJson() throws IOException {
        checkState(json != null,
                "JSON is not available; check that the objects have been ended correctly.");
        return json;
    }

    private static class ObjectHolder {
        final Map<String, Object> map;
        String name;

        ObjectHolder() {
            map = new HashMap<>();
        }

        ObjectHolder(String name) {
            this();
            this.name = name;
        }
    }

    private static class ListHolder {
        final List<Object> list;
        String name;

        ListHolder(String name) {
            list = new ArrayList<>();
            this.name = Objects.requireNonNull(name);
        }
    }

    /**
     * A reverser that simply appends .reversed in fields that were reversed to
     * enable testing that they were indeed reversed.
     */
    private static class JsonDotNotationReverser implements DotNotationReverser {
        private Map<String, Object> map = new HashMap<>();

        @Override
        public void add(String fieldName, String fieldValue) {
            map.put(fieldName + ".reversed", fieldValue);
        }

        @Override
        public Map<String, Object> getResultsMap() {
            return map;
        }

        @Override
        public Map<String, Object> getMappingsMap() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public StorageSerialiser addReverserResults(DotNotationReverser reverser) throws IOException {
        for (Map.Entry<String, Object> entry : reverser.getResultsMap().entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    private static void checkState(boolean isValidState) {
        if (!isValidState) {
            throw new IllegalStateException();
        }
    }

    private static void checkState(boolean isValidState, String msg) {
        if (!isValidState) {
            throw new IllegalStateException(msg);
        }
    }
}

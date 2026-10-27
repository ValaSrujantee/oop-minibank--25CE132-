package repository;

import model.annotation.Id;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Reusable in-memory repository; the default implementation uses a HashMap. */
public class Repository<T> {
    protected final Map<String, T> items;

    public Repository() { this(new HashMap<>()); }

    protected Repository(Map<String, T> storage) { this.items = storage; }

    public T save(T entity) {
        if (entity == null) throw new IllegalArgumentException("Entity is required.");
        String id = findId(entity);
        items.put(id, entity);
        return entity;
    }

    public T findById(String id) { return items.get(id); }
    public ArrayList<T> findAll() { return new ArrayList<>(items.values()); }
    public boolean delete(String id) { return items.remove(id) != null; }

    @SafeVarargs
    public final void saveAll(T... entities) {
        for (T entity : entities) save(entity);
    }

    public static <N extends Number> double average(N[] values) {
        if (values == null || values.length == 0) throw new IllegalArgumentException("At least one value is required.");
        double sum = 0;
        for (N value : values) {
            if (value == null) throw new IllegalArgumentException("Values cannot contain null.");
            sum += value.doubleValue();
        }
        return sum / values.length;
    }

    private String findId(T entity) {
        for (Class<?> type = entity.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Id.class)) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value == null || value.toString().isBlank()) throw new IllegalArgumentException("@Id value cannot be blank.");
                    return value.toString();
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("Could not read @Id field " + field.getName() + ".", ex);
                }
            }
        }
        throw new IllegalArgumentException("Entity type " + entity.getClass().getName() + " has no @Id field.");
    }
}

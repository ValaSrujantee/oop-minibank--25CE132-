package util;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class Validator {
    private Validator() { }
    public static boolean isPositive(long amount) { return amount > 0; }
    public static List<String> validate(Object object) {
        List<String> errors = new ArrayList<>();
        if (object == null) { errors.add("Object cannot be null."); return errors; }
        for (Field field : object.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                if (field.isAnnotationPresent(NotBlank.class) &&
                        (!(value instanceof String s) || s.isBlank())) errors.add(field.getName() + " must not be blank.");
                MinValue min = field.getAnnotation(MinValue.class);
                if (min != null && value instanceof Number n && n.longValue() < min.value())
                    errors.add(field.getName() + " must be at least " + min.value() + ".");
            } catch (ReflectiveOperationException ex) { errors.add("Could not validate " + field.getName()); }
        }
        return errors;
    }
    public static void logAudit(Object target) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Audit.class)) System.out.println("[AUDIT] " + target.getClass().getSimpleName() + "." + method.getName());
        }
    }
}

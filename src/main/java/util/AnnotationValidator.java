package util;

import model.annotation.MaxLength;
import model.annotation.Positive;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class AnnotationValidator {
    private AnnotationValidator() { }

    public static String[] validate(Object object) {
        if (object == null) return new String[] { "Object cannot be null." };
        List<String> errors = new ArrayList<>();
        for (Class<?> type = object.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    Positive positive = field.getAnnotation(Positive.class);
                    if (positive != null && value instanceof Number number && number.longValue() <= 0) {
                        errors.add(field.getName() + " " + positive.message());
                    }
                    MaxLength maxLength = field.getAnnotation(MaxLength.class);
                    if (maxLength != null && value instanceof String text && text.length() > maxLength.value()) {
                        errors.add(field.getName() + " must be at most " + maxLength.value() + " characters.");
                    }
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    errors.add("Could not validate " + field.getName() + ".");
                }
            }
        }
        return errors.toArray(String[]::new);
    }
}

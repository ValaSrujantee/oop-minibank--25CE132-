package util;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Validator {
    private Validator() { }
    private static final Pattern MOBILE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$", Pattern.CASE_INSENSITIVE);

    public static boolean isPositive(long amount) { return amount > 0; }
    public static boolean isValidMobile(String value) { return value != null && MOBILE.matcher(value.trim()).matches(); }
    public static boolean isValidEmail(String value) { return value != null && EMAIL.matcher(value.trim()).matches(); }
    public static boolean isValidPan(String value) { return value != null && PAN.matcher(value.trim()).matches(); }
    public static boolean isValidIfsc(String value) { return value != null && IFSC.matcher(value.trim()).matches(); }
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

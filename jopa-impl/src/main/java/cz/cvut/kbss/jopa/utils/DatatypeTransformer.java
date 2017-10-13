package cz.cvut.kbss.jopa.utils;

import cz.cvut.kbss.jopa.exception.UnsupportedTypeTransformation;
import cz.cvut.kbss.jopa.exceptions.OWLPersistenceException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility containing some basic mappings for conversion between types.
 */
public class DatatypeTransformer {

    private static final Map<Pair, Function<Object, ?>> TRANSFORMERS = initTransformers();

    private static Map<Pair, Function<Object, ?>> initTransformers() {
        final Map<Pair, Function<Object, ?>> map = new HashMap<>();
        map.put(new Pair(Short.class, Integer.class), value -> ((Short) value).intValue());
        map.put(new Pair(Short.class, Long.class), value -> ((Short) value).longValue());
        map.put(new Pair(Short.class, Float.class), value -> ((Short) value).floatValue());
        map.put(new Pair(Short.class, Double.class), value -> ((Short) value).doubleValue());
        map.put(new Pair(Integer.class, Short.class), value -> ((Integer) value).shortValue());
        map.put(new Pair(Integer.class, Long.class), value -> ((Integer) value).longValue());
        map.put(new Pair(Integer.class, Float.class), value -> ((Integer) value).doubleValue());
        map.put(new Pair(Integer.class, Double.class), value -> ((Integer) value).doubleValue());
        map.put(new Pair(Long.class, Integer.class), value -> ((Long) value).intValue());
        map.put(new Pair(Long.class, Short.class), value -> ((Long) value).shortValue());
        map.put(new Pair(Long.class, Float.class), value -> ((Long) value).floatValue());
        map.put(new Pair(Long.class, Double.class), value -> ((Long) value).doubleValue());
        map.put(new Pair(Float.class, Short.class), value -> ((Float) value).shortValue());
        map.put(new Pair(Float.class, Integer.class), value -> ((Float) value).intValue());
        map.put(new Pair(Float.class, Long.class), value -> ((Float) value).longValue());
        map.put(new Pair(Float.class, Double.class), value -> ((Float) value).doubleValue());
        map.put(new Pair(Double.class, Short.class), value -> ((Double) value).shortValue());
        map.put(new Pair(Double.class, Integer.class), value -> ((Double) value).intValue());
        map.put(new Pair(Double.class, Long.class), value -> ((Double) value).longValue());
        map.put(new Pair(Double.class, Float.class), value -> ((Double) value).floatValue());
        map.put(new Pair(URI.class, URL.class), value -> {
            try {
                return ((URI) value).toURL();
            } catch (MalformedURLException e) {
                throw new OWLPersistenceException("Unable to transform URI to URL.", e);
            }
        });
        return map;
    }

    /**
     * Maps the specified value to the target type (if possible).
     *
     * @param value      The value to convert
     * @param targetType The type to which the specified value should be converted
     * @return Value as the target type
     */
    public static <T> T transform(Object value, Class<T> targetType) {
        Objects.requireNonNull(value);
        if (targetType.equals(String.class)) {
            return (T) value.toString();
        }
        final Class<?> sourceType = value.getClass();
        if (targetType.isAssignableFrom(sourceType)) {
            return (T) value;
        }
        final Pair p = new Pair(sourceType, targetType);
        if (TRANSFORMERS.containsKey(p)) {
            return (T) TRANSFORMERS.get(p).apply(value);
        }
        final Optional<T> result = tryConversionUsingConstructor(value, targetType);
        return result.orElseThrow(() -> new UnsupportedTypeTransformation(
                String.format("Cannot transform value %s of type %s to target type %s.", value, value.getClass(),
                        targetType)));
    }

    private static <T> Optional<T> tryConversionUsingConstructor(Object value, Class<T> targetType) {
        try {
            final Constructor ctor = targetType.getDeclaredConstructor(value.getClass());
            return Optional.of((T) ctor.newInstance(value));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new OWLPersistenceException("Unable to transform value using target type constructor.", e);
        }
    }

    private static final class Pair {

        private final Class<?> sourceType;
        private final Class<?> targetType;

        private Pair(Class<?> sourceType, Class<?> targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pair)) return false;

            Pair pair = (Pair) o;

            return sourceType.equals(pair.sourceType) && targetType.equals(pair.targetType);
        }

        @Override
        public int hashCode() {
            int result = sourceType.hashCode();
            result = 31 * result + targetType.hashCode();
            return result;
        }
    }
}

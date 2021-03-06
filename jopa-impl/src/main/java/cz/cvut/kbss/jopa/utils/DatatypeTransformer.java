/**
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility containing some basic mappings for conversion between types.
 */
public class DatatypeTransformer {

    private static final Map<Pair, Function<Object, ?>> TRANSFORMERS = initTransformers();

    private DatatypeTransformer() {
        throw new AssertionError();
    }

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
        if (value == null) {
            return null;
        }
        if (targetType.equals(String.class)) {
            return targetType.cast(value.toString());
        }
        final Class<?> sourceType = value.getClass();
        if (targetType.isAssignableFrom(sourceType)) {
            return targetType.cast(value);
        }
        final Pair p = new Pair(sourceType, targetType);
        if (TRANSFORMERS.containsKey(p)) {
            return targetType.cast(TRANSFORMERS.get(p).apply(value));
        }
        final Optional<T> result = tryConversionUsingConstructor(value, targetType);
        return result.orElseThrow(() -> new UnsupportedTypeTransformation(
                String.format("Cannot transform value %s of type %s to target type %s.", value, value.getClass(),
                        targetType)));
    }

    private static <T> Optional<T> tryConversionUsingConstructor(Object value, Class<T> targetType) {
        try {
            final Constructor ctor = targetType.getDeclaredConstructor(value.getClass());
            return Optional.of(targetType.cast(ctor.newInstance(value)));
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

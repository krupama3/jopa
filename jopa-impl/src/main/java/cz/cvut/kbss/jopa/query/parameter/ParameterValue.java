/**
 * Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.query.parameter;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;

/**
 * Query parameter value holder.
 */
public interface ParameterValue {

    /**
     * Gets the value held by this wrapper.
     *
     * @return The parameter value
     */
    Object getValue();

    /**
     * Gets this parameter value as a string which can be inserted directly into a query.
     *
     * @return Value as query string
     */
    String getQueryString();

    /**
     * Returns a new variable parameter specification.
     * <p>
     * This is the default implementation, if a parameter is not set, a variable is used in the query to represent an
     * unbound parameter.
     *
     * @param name Parameter (variable) name
     * @return Parameter value object
     */
    static ParameterValue createVariableValue(String name) {
        return new NamedVariableParameterValue(name);
    }

    /**
     * Returns a new variable parameter specification.
     * <p>
     * This is the default implementation, if a parameter is not set, a variable is used in the query to represent an
     * unbound parameter.
     *
     * @param position Parameter (variable) position
     * @return Parameter value object
     */
    static ParameterValue createVariableValue(Integer position) {
        return new PositionalVariableParameterValue(position);
    }

    /**
     * Returns new String parameter value specification.
     * <p>
     * The language tag is optional.
     *
     * @param value    The value
     * @param language Language tag of the value, e.g. en, cz. Optional
     * @return Parameter value object
     */
    static ParameterValue create(String value, String language) {
        return new StringParameterValue(value, language);
    }

    /**
     * Returns new parameter value specification.
     *
     * @param value The value
     * @return Parameter value object
     */
    static ParameterValue create(Object value) {
        Objects.requireNonNull(value);
        if (value instanceof URI) {
            return new UriParameterValue((URI) value);
        } else if (value instanceof URL) {
            try {
                return new UriParameterValue(((URL) value).toURI());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to transform the specified URL to URI.", e);
            }
        } else if (value instanceof Boolean) {
            return new BooleanParameterValue((Boolean) value);
        } else if (value instanceof Short) {
            return new ShortParameterValue((Short) value);
        } else if (value instanceof Integer) {
            return new IntegerParameterValue((Integer) value);
        } else if (value instanceof Long) {
            return new LongParameterValue((Long) value);
        } else if (value instanceof Double) {
            return new DoubleParameterValue((Double) value);
        } else if (value instanceof Float) {
            return new FloatParameterValue((Float) value);
        } else if (value instanceof Date) {
            return new DateParameterValue((Date) value);
        } else {
            return new StringParameterValue(value.toString());
        }
    }

    /**
     * Returns new untyped parameter value specification.
     *
     * @param value The value
     * @return Parameter value object
     */
    static ParameterValue createUntyped(Object value) {
        return new UntypedParameterValue(Objects.requireNonNull(value));
    }
}

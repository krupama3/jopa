package cz.cvut.kbss.jopa.query;

import cz.cvut.kbss.jopa.model.query.Parameter;

import java.util.Set;

/**
 * Represents a caretaker of a query, enabling parameter setting and final assembly of the query.
 *
 * @author kidney
 */
public interface QueryHolder {

    /**
     * Gets the original query string.
     *
     * @return Gets the original unparsed query
     */
    String getQuery();

    /**
     * Gets a collection of parameters in the query.
     *
     * @return Parameter names
     */
    Set<Parameter<?>> getParameters();

    /**
     * Gets a parameter with the specified name.
     *
     * @param name Parameter name
     * @return Parameter object or {@code null}, if there is none with matching name
     */
    Parameter<?> getParameter(String name);

    /**
     * Gets a parameter with the specified position.
     *
     * @param position Parameter position
     * @return Parameter object or {@code null}, if there is none at matching position
     */
    Parameter<?> getParameter(int position);

    /**
     * Gets value bound to the specified parameter.
     *
     * @param parameter Parameter
     * @return parameter value
     * @throws IllegalArgumentException If there is no parameter with the specified name
     */
    Object getParameterValue(Parameter<?> parameter);

    /**
     * Sets value of the specified parameter in the query.
     * <p>
     * If a value was already specified for the parameter, it is overwritten by the new one.
     *
     * @param parameter Parameter object
     * @param value     Value to use
     * @throws IllegalArgumentException If there is no such parameter in the query
     */
    <T> void setParameter(Parameter<T> parameter, Object value);

    /**
     * Sets value of the specified parameter in the query.
     * <p>
     * If a value was already specified for the parameter, it is overwritten by the new one.
     *
     * @param parameter Parameter object
     * @param value     String value to use
     * @param language  Parameter language
     * @throws IllegalArgumentException If there is no such parameter in the query
     */
    <T> void setParameter(Parameter<T> parameter, String value, String language);

    /**
     * Clears any previously set value of the specified parameter.
     *
     * @param parameter Parameter object
     * @throws IllegalArgumentException If there is no such parameter in the query
     */
    void clearParameter(Parameter<?> parameter);

    /**
     * Clears any previously set parameter values in this query.
     */
    void clearParameters();

    /**
     * Assembles the query, using any parameter values specified, and returns it as a string.
     *
     * @return Assembled query
     */
    String assembleQuery();
}

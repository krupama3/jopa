/**
 * Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.model.annotations;

import java.lang.annotation.*;

/**
 * Marks an attribute mapped to an OWL object property.
 *
 * The Java type of such attributes is either another entity or a valid identifier type.
 *
 * Note that for use with RDF(S), attributes annotated with this annotation are expected to reference other RDF resources.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OWLObjectProperty {

    /**
     * IRI of the object property
     *
     * @return IRI of the object property
     */
    String iri();

    /**
     * (Optional) The operations that must be cascaded to the target of the association.
     * <p>
     * By default no operations are cascaded.
     *
     * @return Cascading setting for the annotated attribute
     */
    CascadeType[] cascade() default {};

    /**
     * (Optional) Whether the association should be lazily loaded or must be eagerly fetched.
     *
     * @return Whether this property is read only
     */
    FetchType fetch() default FetchType.LAZY;
}

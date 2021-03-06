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

import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NamespaceResolverTest {

    private final NamespaceResolver resolver = new NamespaceResolver();

    @Test
    public void resolveReturnsOriginalIriWhenItDoesNotContainPrefix() {
        assertEquals(DC.Elements.DESCRIPTION, resolver.resolveFullIri(DC.Elements.DESCRIPTION));
    }

    @Test
    public void resolveReturnsOriginalIriWhenPrefixIsNotRegistered() {
        final String iri = "dc:description";
        assertEquals(iri, resolver.resolveFullIri(iri));
    }

    @Test
    public void resolveReturnsPreregisteredIri() {
        assertEquals(RDFS.LABEL, resolver.resolveFullIri("rdfs:label"));
    }

    @Test
    public void resolveReturnsIriBasedOnRegisteredNamespace() {
        resolver.registerNamespace("dc", DC.Elements.NAMESPACE);
        final String iri = "dc:description";
        assertEquals(DC.Elements.NAMESPACE + "description", resolver.resolveFullIri(iri));
    }

    @Test
    public void resolveReturnsOriginalIriWhenPrefixIsInvalid() {
        final String invalidIri = "prefixWithoutLocalName";
        assertEquals(invalidIri, resolver.resolveFullIri(invalidIri));
    }
}
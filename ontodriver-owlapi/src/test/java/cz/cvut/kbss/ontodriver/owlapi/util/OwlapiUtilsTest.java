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
package cz.cvut.kbss.ontodriver.owlapi.util;

import cz.cvut.kbss.ontodriver.model.Assertion;
import cz.cvut.kbss.ontodriver.owlapi.environment.Generator;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OwlapiUtilsTest {

    private static final String LANG = "en";

    private OWLDataFactory dataFactory = new OWLDataFactoryImpl();

    @Test
    void doesLanguageMatchForAssertionReturnsTrueWhenLanguageTagMatches() {
        final OWLLiteral literal = dataFactory.getOWLLiteral("test", LANG);
        final Assertion assertion = Assertion.createPropertyAssertion(Generator.generateUri(), LANG, false);
        assertTrue(OwlapiUtils.doesLanguageMatch(literal, assertion));
    }

    @Test
    void doesLanguageMatchForAssertionReturnsFalseWhenLanguageTagDoesNotMatch() {
        final OWLLiteral literal = dataFactory.getOWLLiteral("test", LANG);
        final Assertion assertion = Assertion.createPropertyAssertion(Generator.generateUri(), "cs", false);
        assertFalse(OwlapiUtils.doesLanguageMatch(literal, assertion));
    }

    @Test
    void doesLanguageMatchForAssertionsReturnsTrueWhenLanguageIsNotSpecifiedOnAssertion() {
        final OWLLiteral literal = dataFactory.getOWLLiteral("test", LANG);
        final Assertion assertion = Assertion.createPropertyAssertion(Generator.generateUri(), false);
        assertTrue(OwlapiUtils.doesLanguageMatch(literal, assertion));
    }

    @Test
    void doesLanguageMatchForAssertionReturnsTrueWhenLiteralHasNoLanguageTag() {
        final OWLLiteral literal = dataFactory.getOWLLiteral("test");
        final Assertion assertion = Assertion.createPropertyAssertion(Generator.generateUri(), LANG, false);
        assertTrue(OwlapiUtils.doesLanguageMatch(literal, assertion));
    }

    @Test
    void doesLanguageMatchForAssertionReturnsTrueForNonStringLiteral() {
        final OWLLiteral literal = dataFactory.getOWLLiteral(117);
        final Assertion assertion = Assertion.createPropertyAssertion(Generator.generateUri(), LANG, false);
        assertTrue(OwlapiUtils.doesLanguageMatch(literal, assertion));
    }

    @Test
    void createOwlLiteralFromValueCreatesSimpleLiteralFromStringWithoutLanguageTag() {
        final OWLLiteral result = OwlapiUtils.createOWLLiteralFromValue("test", dataFactory, null);
        assertFalse(result.hasLang());
        assertEquals(OWL2Datatype.XSD_STRING.getDatatype(dataFactory), result.getDatatype());
    }

    @Test
    void createOwlLiteralFromValueCreatesLangStringFromStringWithLanguageTag() {
        final OWLLiteral result = OwlapiUtils.createOWLLiteralFromValue("test", dataFactory, LANG);
        assertTrue(result.hasLang());
        assertEquals(OWL2Datatype.RDF_LANG_STRING.getDatatype(dataFactory), result.getDatatype());
    }
}
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
package cz.cvut.kbss.jopa.model;

import cz.cvut.kbss.jopa.environment.OWLClassA;
import cz.cvut.kbss.jopa.environment.OWLClassF;
import cz.cvut.kbss.jopa.environment.OWLClassM;
import cz.cvut.kbss.jopa.environment.OWLClassQ;
import cz.cvut.kbss.jopa.environment.utils.Generators;
import cz.cvut.kbss.jopa.exceptions.AttributeModificationForbiddenException;
import cz.cvut.kbss.jopa.exceptions.InferredAttributeModifiedException;
import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.MappedSuperclass;
import cz.cvut.kbss.jopa.model.annotations.OWLAnnotationProperty;
import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.jopa.sessions.UnitOfWorkImpl;
import cz.cvut.kbss.jopa.sessions.UnitOfWorkTestBase;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BeanListenerAspectTest extends UnitOfWorkTestBase {

    private UnitOfWorkImpl sut;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.sut = spy(uow);
    }

    @Test
    void setterAspectIsCalledForFieldsInOWLClass() throws Exception {
        when(transactionMock.isActive()).thenReturn(true);
        sut.registerNewObject(entityA, descriptor);
        entityA.setStringAttribute("test");
        verify(sut).attributeChanged(entityA, OWLClassA.getStrAttField());
    }

    @Test
    void setterAspectIsCalledForFieldsInMappedSuperclass() throws Exception {
        when(transactionMock.isActive()).thenReturn(true);
        final OWLClassQ entityQ = new OWLClassQ();
        entityQ.setUri(Generators.createIndividualIdentifier());
        sut.registerNewObject(entityQ, descriptor);
        entityQ.setLabel("test");
        verify(sut).attributeChanged(entityQ, OWLClassQ.getLabelField());
    }

    @Test
    void getterAspectIsCalledForFieldsInMappedSuperclass() throws Exception {
        final OWLClassQ entityQ = new OWLClassQ();
        entityQ.setUri(Generators.createIndividualIdentifier());
        final FieldSpecification aSpec = metamodelMock.entity(OWLClassQ.class).getFieldSpecification("owlClassA");
        when(aSpec.getFetchType()).thenReturn(FetchType.LAZY);
        final OWLClassQ clone = (OWLClassQ) sut.registerExistingObject(entityQ, descriptor);
        clone.getOwlClassA();
        verify(sut).loadEntityField(clone, OWLClassQ.getOwlClassAField());
    }

    @Test
    void setterAspectHandlesInstanceOfMappedSuperclassOutsideOfPersistenceContext() {
        final WithMappedSuperclass instance = new WithMappedSuperclass();
        final String value = "test";
        // Just invoke setter and see if the aspect is able to handle it
        instance.setLabel(value);
    }

    @Test
    void getterAspectHandlesInstanceOfMappedSuperclassOutsideOfPersistenceContext() {
        final WithMappedSuperclass instance = new WithMappedSuperclass();
        // Just invoke getter and see if the aspect is able to handle it
        assertNull(instance.getLabel());
    }

    @MappedSuperclass
    public static class WithMappedSuperclass {
        @OWLAnnotationProperty(iri = RDFS.LABEL)
        private String label;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    @Test
    void setterAspectThrowsInferredAttributeModifiedExceptionWhenInferredAttributeIsModified() {
        when(transactionMock.isActive()).thenReturn(true);
        final OWLClassF entityF = new OWLClassF(Generators.createIndividualIdentifier());
        final OWLClassF clone = (OWLClassF) sut.registerExistingObject(entityF, descriptor);
        assertThrows(InferredAttributeModifiedException.class, () -> clone.setSecondStringAttribute("value"));
    }

    @Test
    void setterAspectThrowsAttributeModificationForbiddenWhenLexicalFormAttributeValueIsModified() {
        when(transactionMock.isActive()).thenReturn(true);
        final OWLClassM entityM = new OWLClassM();
        entityM.initializeTestValues(true);
        final OWLClassM clone = (OWLClassM) sut.registerExistingObject(entityM, descriptor);
        assertThrows(AttributeModificationForbiddenException.class, () -> clone.setLexicalForm("value"));
    }
}
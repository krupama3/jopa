package cz.cvut.kbss.jopa.test.integration.runners;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.model.descriptors.ObjectPropertyCollectionDescriptor;
import cz.cvut.kbss.jopa.test.OWLClassA;
import cz.cvut.kbss.jopa.test.OWLClassB;
import cz.cvut.kbss.jopa.test.OWLClassC;
import cz.cvut.kbss.jopa.test.OWLClassD;
import cz.cvut.kbss.jopa.test.environment.Generators;
import cz.cvut.kbss.jopa.test.environment.TestEnvironmentUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class UpdateOperationsMultiContextRunner extends BaseRunner {

	public UpdateOperationsMultiContextRunner(Logger logger) {
		super(logger);
	}

	public void updateDataPropertyInContext(EntityManager em) throws Exception {
		logger.config("Test: update data property value which is stored in a different context that the owner.");
		final Descriptor aDescriptor = new EntityDescriptor(CONTEXT_ONE);
		aDescriptor.addAttributeContext(OWLClassA.getStrAttField(), CONTEXT_TWO);
		em.getTransaction().begin();
		em.persist(entityA, aDescriptor);
		em.getTransaction().commit();

		em.getTransaction().begin();
		final OWLClassA a = em.find(OWLClassA.class, entityA.getUri(), aDescriptor);
		assertNotNull(a);
		final String newAttValue = "newStringAttributeValue";
		a.setStringAttribute(newAttValue);
		em.getTransaction().commit();

		final OWLClassA resA = em.find(OWLClassA.class, entityA.getUri(), aDescriptor);
		assertNotNull(resA);
		assertEquals(newAttValue, resA.getStringAttribute());
		assertEquals(entityA.getTypes(), resA.getTypes());
	}

	public void updateObjectPropertyToDifferentContext(EntityManager em) throws Exception {
		logger.config("Test: update object property with value from different context than the previous.");
		final Descriptor dDescriptor = new EntityDescriptor();
		final Descriptor aDescriptor = new EntityDescriptor(CONTEXT_ONE);
		dDescriptor.addAttributeDescriptor(OWLClassD.getOwlClassAField(), aDescriptor);
		em.getTransaction().begin();
		em.persist(entityD, dDescriptor);
		em.persist(entityA, aDescriptor);
		em.getTransaction().commit();

		final OWLClassD d = em.find(OWLClassD.class, entityD.getUri(), dDescriptor);
		assertNotNull(d);
		assertNotNull(d.getOwlClassA());
		em.getTransaction().begin();
		final OWLClassA newA = new OWLClassA();
		newA.setUri(URI.create("http://krizik.felk.cvut.cz/jopa/ontologies/newEntityA"));
		newA.setStringAttribute("newAStringAttribute");
		final Descriptor newADescriptor = new EntityDescriptor(CONTEXT_TWO);
		em.persist(newA, newADescriptor);
		dDescriptor.addAttributeDescriptor(OWLClassD.getOwlClassAField(), newADescriptor);
		d.setOwlClassA(newA);
		em.getTransaction().commit();

		final OWLClassD resD = em.find(OWLClassD.class, entityD.getUri(), dDescriptor);
		assertNotNull(resD);
		assertEquals(newA.getUri(), resD.getOwlClassA().getUri());
		assertEquals(newA.getStringAttribute(), resD.getOwlClassA().getStringAttribute());
		final OWLClassA resA = em.find(OWLClassA.class, entityA.getUri(), aDescriptor);
		assertNotNull(resA);
		assertEquals(entityA.getStringAttribute(), resA.getStringAttribute());
	}

	public void updateAddToPropertiesInContext(EntityManager em) throws Exception {
		logger.config("Test: add new property value, properties are stored in a different context.");
		entityB.setProperties(Generators.createProperties());
		final Descriptor bDescriptor = new EntityDescriptor(CONTEXT_ONE);
		bDescriptor.addAttributeContext(OWLClassB.getPropertiesField(), CONTEXT_TWO);
		em.getTransaction().begin();
		em.persist(entityB, bDescriptor);
		em.getTransaction().commit();

		em.getTransaction().begin();
		final OWLClassB b = em.find(OWLClassB.class, entityB.getUri(), bDescriptor);
		assertNotNull(b);
		final String newKey = "http://krizik.felk.cvut.cz/jopa/ontologies/properties/newPropertyKey";
		final String newValue = "http://krizik.felk.cvut.cz/jopa/ontologies/newPropertyValue";
		final String newPropertyValue = "http://krizik.felk.cvut.cz/jopa/ontologies/NewValueOfAnOldProperty";
        final String propertyToChange = b.getProperties().keySet().iterator().next();
		b.getProperties().put(newKey, Collections.singleton(newValue));
		b.getProperties().get(propertyToChange).add(newPropertyValue);
		em.getTransaction().commit();

		final OWLClassB res = em.find(OWLClassB.class, entityB.getUri(), bDescriptor);
		assertNotNull(res);
		assertEquals(entityB.getStringAttribute(), res.getStringAttribute());
		assertTrue(TestEnvironmentUtils.arePropertiesEqual(b.getProperties(), res.getProperties()));
	}

	public void updateAddToSimpleListInContext(EntityManager em) throws Exception {
		logger.config("Test: add new element into a simple list stored in different context than its owner.");
		entityC.setSimpleList(Generators.createSimpleList(15));
		final Descriptor cDescriptor = new EntityDescriptor(CONTEXT_ONE);
		final Descriptor lstDescriptor = new ObjectPropertyCollectionDescriptor(CONTEXT_TWO,
				OWLClassC.getSimpleListField());
		cDescriptor.addAttributeDescriptor(OWLClassC.getSimpleListField(), lstDescriptor);
		em.getTransaction().begin();
		em.persist(entityC, cDescriptor);
		for (OWLClassA a : entityC.getSimpleList()) {
			em.persist(a, lstDescriptor);
		}
		em.getTransaction().commit();

		em.getTransaction().begin();
		final OWLClassC c = em.find(OWLClassC.class, entityC.getUri(), cDescriptor);
		assertNotNull(c);
		assertEquals(entityC.getSimpleList().size(), c.getSimpleList().size());
		c.getSimpleList().add(entityA);
		em.persist(entityA, lstDescriptor);
		em.getTransaction().commit();

		final OWLClassC resC = em.find(OWLClassC.class, entityC.getUri(), cDescriptor);
		assertNotNull(resC);
		assertEquals(entityC.getSimpleList().size() + 1, resC.getSimpleList().size());
		boolean found = false;
		for (OWLClassA a : resC.getSimpleList()) {
			if (a.getUri().equals(entityA.getUri())) {
				assertEquals(entityA.getStringAttribute(), a.getStringAttribute());
				assertEquals(entityA.getTypes(), a.getTypes());
				found = true;
				break;
			}
		}
		assertTrue(found);
	}

	public void updateAddToReferencedListInContext(EntityManager em) throws Exception {
		logger.config("Test: add new element into a referenced list stored in different context than its owner.");
		entityC.setReferencedList(Generators.createReferencedList(10));
		final Descriptor cDescriptor = new EntityDescriptor(CONTEXT_ONE);
		final Descriptor lstDescriptor = new ObjectPropertyCollectionDescriptor(CONTEXT_TWO,
				OWLClassC.getRefListField());
		cDescriptor.addAttributeDescriptor(OWLClassC.getRefListField(), lstDescriptor);
		em.getTransaction().begin();
		em.persist(entityC, cDescriptor);
		for (OWLClassA a : entityC.getReferencedList()) {
			em.persist(a, lstDescriptor);
		}
		em.getTransaction().commit();

		em.getTransaction().begin();
		final OWLClassC c = em.find(OWLClassC.class, entityC.getUri(), cDescriptor);
		assertNotNull(c);
		assertEquals(entityC.getReferencedList().size(), c.getReferencedList().size());
		c.getReferencedList().add(entityA);
		em.persist(entityA, lstDescriptor);
		em.getTransaction().commit();

		final OWLClassC resC = em.find(OWLClassC.class, entityC.getUri(), cDescriptor);
		assertNotNull(resC);
		assertEquals(entityC.getReferencedList().size() + 1, resC.getReferencedList().size());
		boolean found = false;
		for (OWLClassA a : resC.getReferencedList()) {
			if (a.getUri().equals(entityA.getUri())) {
				assertEquals(entityA.getStringAttribute(), a.getStringAttribute());
				assertEquals(entityA.getTypes(), a.getTypes());
				found = true;
				break;
			}
		}
		assertTrue(found);
	}

	public void updateRemoveFromSimpleListInContext(EntityManager em) throws Exception {
		logger.config("Test: remove element from simple list stored in a different context than its owner.");
		entityC.setSimpleList(Generators.createSimpleList(15));
		final Descriptor cDescriptor = new EntityDescriptor(CONTEXT_ONE);
		final Descriptor lstDescriptor = new ObjectPropertyCollectionDescriptor(CONTEXT_TWO,
				OWLClassC.getSimpleListField());
		cDescriptor.addAttributeDescriptor(OWLClassC.getSimpleListField(), lstDescriptor);
		em.getTransaction().begin();
		em.persist(entityC, cDescriptor);
		for (OWLClassA a : entityC.getSimpleList()) {
			em.persist(a, lstDescriptor);
		}
		em.getTransaction().commit();

		em.getTransaction().begin();
		final OWLClassC c = em.find(OWLClassC.class, entityC.getUri(), cDescriptor);
		assertNotNull(c);
		assertEquals(entityC.getSimpleList().size(), c.getSimpleList().size());
		final OWLClassA a = c.getSimpleList().get(0);
		c.getSimpleList().remove(0);
		em.remove(a);
		em.getTransaction().commit();

		final OWLClassC resC = em.find(OWLClassC.class, entityC.getUri(), cDescriptor);
		assertNotNull(resC);
		assertEquals(entityC.getSimpleList().size() - 1, resC.getSimpleList().size());
		assertNull(em.find(OWLClassA.class, a.getUri(), lstDescriptor));
	}

	public void updateRemoveFromReferencedListInContext(EntityManager em) throws Exception {
		logger.config("Test: remove elements from referenced list stored in a different context than its owner.");
		entityC.setReferencedList(Generators.createReferencedList(10));
		final Descriptor cDescriptor = new EntityDescriptor(CONTEXT_ONE);
		final Descriptor lstDescriptor = new ObjectPropertyCollectionDescriptor(CONTEXT_TWO,
				OWLClassC.getRefListField());
		cDescriptor.addAttributeDescriptor(OWLClassC.getRefListField(), lstDescriptor);
		em.getTransaction().begin();
		em.persist(entityC, cDescriptor);
		for (OWLClassA a : entityC.getReferencedList()) {
			em.persist(a, lstDescriptor);
		}
		em.getTransaction().commit();

		em.getTransaction().begin();
		final OWLClassC c = em.find(OWLClassC.class, entityC.getUri(), cDescriptor);
		assertNotNull(c);
		assertEquals(entityC.getReferencedList().size(), c.getReferencedList().size());
		final List<OWLClassA> removed = new ArrayList<>();
		int i = 0;
		final Iterator<OWLClassA> it = c.getReferencedList().iterator();
		while (it.hasNext()) {
			i++;
			final OWLClassA a = it.next();
			if (i % 2 == 1) {
				continue;
			}
			removed.add(a);
			it.remove();
		}
		em.getTransaction().commit();

		final OWLClassC resC = em.find(OWLClassC.class, entityC.getUri(), cDescriptor);
		assertNotNull(resC);
		assertEquals(entityC.getReferencedList().size() - removed.size(), resC.getReferencedList()
				.size());
		for (OWLClassA a : removed) {
			final OWLClassA resA = em.find(OWLClassA.class, a.getUri(), lstDescriptor);
			assertNotNull(resA);
		}
	}
}
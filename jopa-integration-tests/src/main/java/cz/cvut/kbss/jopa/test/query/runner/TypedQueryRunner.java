/**
 * Copyright (C) 2016 Czech Technical University in Prague
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
package cz.cvut.kbss.jopa.test.query.runner;

import cz.cvut.kbss.jopa.exceptions.NoResultException;
import cz.cvut.kbss.jopa.exceptions.NoUniqueResultException;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.query.TypedQuery;
import cz.cvut.kbss.jopa.test.OWLClassA;
import cz.cvut.kbss.jopa.test.OWLClassB;
import cz.cvut.kbss.jopa.test.OWLClassD;
import cz.cvut.kbss.jopa.test.OWLClassE;
import cz.cvut.kbss.jopa.test.query.QueryTestEnvironment;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public abstract class TypedQueryRunner extends BaseQueryRunner {

    protected TypedQueryRunner(Logger logger) {
        super(logger);
    }

    @Test
    public void testFindAll() {
        logger.debug("Test: select all entities of a certain type.");
        final String query = "SELECT ?x WHERE { ?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassD> .}";
        final List<OWLClassD> ds = new ArrayList<>();
        final TypedQuery<OWLClassD> q = getEntityManager().createNativeQuery(query, OWLClassD.class);
        ds.addAll(QueryTestEnvironment.getDataByContext(null, OWLClassD.class));
        final List<OWLClassD> res = q.getResultList();
        assertNotNull(res);
        assertFalse(res.isEmpty());
        assertEquals(ds.size(), res.size());
        boolean found;
        for (OWLClassD d : ds) {
            found = false;
            for (OWLClassD dd : res) {
                if (d.getUri().equals(dd.getUri())) {
                    found = true;
                    assertNotNull(dd.getOwlClassA());
                    assertEquals(d.getOwlClassA().getUri(), dd.getOwlClassA().getUri());
                    break;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    public void testSelectByTypeAndDataPropertyValue() {
        logger.debug("Test: select entity by its type and data property value.");
        final OWLClassB b = QueryTestEnvironment.getData(OWLClassB.class).get(5);
        final String query =
                "SELECT ?x WHERE { " +
                        "?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassB> ; " +
                        "<http://krizik.felk.cvut.cz/ontologies/jopa/attributes#B-stringAttribute> ?bString . }";
        final TypedQuery<OWLClassB> q = getEntityManager().createNativeQuery(query, OWLClassB.class);
        q.setParameter("bString", b.getStringAttribute(), "en");
        final OWLClassB res = q.getSingleResult();
        assertNotNull(res);
        assertEquals(b.getUri(), res.getUri());
        assertEquals(b.getStringAttribute(), res.getStringAttribute());
    }

    @Test
    public void testSelectByObjectProperty() {
        logger.debug("Test: select entity by object property value.");
        final String query = "SELECT ?x WHERE { ?x <http://krizik.felk.cvut.cz/ontologies/jopa/attributes#hasA> ?y . }";
        final TypedQuery<OWLClassD> q = getEntityManager().createNativeQuery(query, OWLClassD.class);
        final List<OWLClassD> ds = new ArrayList<>();
        ds.addAll(QueryTestEnvironment.getDataByContext(null, OWLClassD.class));
        final int cnt = ds.size() / 2;
        assertTrue(cnt > 1);
        final List<OWLClassD> res = q.setMaxResults(cnt).getResultList();
        assertEquals(cnt, res.size());
    }

    @Test
    public void testSetMaxResults() {
        logger.debug("Test: set maximum number of results.");
        final String query = "SELECT ?x WHERE { ?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassE> . }";
        final TypedQuery<OWLClassE> q = getEntityManager().createNativeQuery(query, OWLClassE.class);
        final int max = 5;
        assertTrue(max < QueryTestEnvironment.getData(OWLClassE.class).size());
        assertEquals(Integer.MAX_VALUE, q.getMaxResults());
        q.setMaxResults(max);
        assertEquals(max, q.getMaxResults());
        final List<OWLClassE> res = q.getResultList();
        assertNotNull(res);
        assertFalse(res.isEmpty());
        assertEquals(max, res.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxResultsNegative() {
        logger.debug("Test: set maximum number of results. Negative argument.");
        final String query = "SELECT ?x WHERE { ?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassE> . }";
        final TypedQuery<OWLClassE> q = getEntityManager().createNativeQuery(query, OWLClassE.class);
        q.setMaxResults(-1);
    }

    @Test
    public void testSetMaxResultsZero() {
        logger.debug("Test: set maximum number of results. Zero argument.");
        final String query = "SELECT ?x WHERE { ?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassE> . }";
        final TypedQuery<OWLClassE> q = getEntityManager().createNativeQuery(query, OWLClassE.class);
        q.setMaxResults(0);
        final List<OWLClassE> res = q.getResultList();
        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    public void testGetSingleResult() {
        logger.debug("Test: get single result.");
        final OWLClassA a = QueryTestEnvironment.getData(OWLClassA.class).get(0);
        final String query =
                "SELECT ?x WHERE { ?x <http://krizik.felk.cvut.cz/ontologies/jopa/attributes#A-stringAttribute> ?aString .}";
        final TypedQuery<OWLClassA> q = getEntityManager().createNativeQuery(query, OWLClassA.class);
        q.setParameter("aString", a.getStringAttribute(), "en");
        final OWLClassA res = q.getSingleResult();
        assertNotNull(res);
        assertEquals(a.getUri(), res.getUri());
    }

    @Test(expected = NoUniqueResultException.class)
    public void testGetSingleResultMultiples() {
        logger.debug("Test: get single result. No unique result.");
        final String query = "SELECT ?x WHERE { ?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassE> . }";
        final TypedQuery<OWLClassE> q = getEntityManager().createNativeQuery(query, OWLClassE.class);
        q.getSingleResult();
    }

    @Test(expected = NoResultException.class)
    public void testGetSingleResultNoResult() {
        logger.debug("Test: get single result. No result.");
        final String query = "SELECT ?x WHERE { ?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassX> . }";
        final TypedQuery<OWLClassE> q = getEntityManager().createNativeQuery(query, OWLClassE.class);
        q.getSingleResult();
    }

    @Test(expected = NullPointerException.class)
    public void testCreateQueryNullQuery() {
        logger.debug("Test: create query. Null query passed.");
        getEntityManager().createNativeQuery(null, OWLClassA.class);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateQueryNullClass() {
        logger.debug("Test: create query. Null result class passed.");
        final String query = "SELECT ?x WHERE { ?x ?y ?z .}";
        getEntityManager().createNativeQuery(query, (Class<OWLClassA>) null);
    }

    @Test
    public void askQueryReturnsTrue() {
        logger.debug("Test: execute a ASK query which returns true.");
        final String query = "ASK { ?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassA> . }";
        final TypedQuery<Boolean> q = getEntityManager().createNativeQuery(query, Boolean.class);
        final Boolean res = q.getSingleResult();
        assertNotNull(res);
        assertTrue(res);
    }

    @Test
    public void askQueryReturnsFalse() {
        logger.debug("Test: execute a ASK query which returns false.");
        final String query = "ASK { ?x a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassX> . }";
        final TypedQuery<Boolean> q = getEntityManager().createNativeQuery(query, Boolean.class);
        final List<Boolean> res = q.getResultList();
        assertNotNull(res);
        assertEquals(1, res.size());
        assertFalse(res.get(0));
    }

    @Ignore
    @Test
    public void askQueryAgainstTransactionalOntologyContainsUncommittedChangesAsWell() throws Exception {
        logger.debug("Test: execute an ASK query which returns changes yet to be committed in transaction.");
        final OWLClassE e = new OWLClassE();
        getEntityManager().getTransaction().begin();
        try {
            getEntityManager().persist(e);
            final TypedQuery<Boolean> query = getEntityManager().createNativeQuery(
                    "ASK { ?individual a <http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassE> . }",
                    Boolean.class).setParameter("individual", e.getUri());
            final Boolean res = query.getSingleResult();
            assertTrue(res);
        } finally {
            getEntityManager().getTransaction().rollback();
        }
    }

    @Test
    public void askQueryWithPositionParameter() {
        logger.debug("Test: execute an ASK query which returns true, query contains positional parameter.");
        final String query = "ASK { ?x a $1 . }";
        final URI paramValue = URI.create(OWLClassA.class.getAnnotation(OWLClass.class).iri());
        final TypedQuery<Boolean> q = getEntityManager().createNativeQuery(query, Boolean.class)
                                                        .setParameter(1, paramValue);
        final Boolean res = q.getSingleResult();
        assertNotNull(res);
        assertTrue(res);
    }

    @Test
    public void testCreateTypedNamedNativeQuery() {
        final List<OWLClassA> expected = QueryTestEnvironment.getData(OWLClassA.class);
        final List<URI> uris = expected.stream().map(OWLClassA::getUri).collect(Collectors.toList());
        final List<OWLClassA> res = getEntityManager().createNamedQuery("OWLClassA.findAll", OWLClassA.class)
                                                      .getResultList();
        assertEquals(expected.size(), res.size());
        res.forEach(a -> assertTrue(uris.contains(a.getUri())));
    }
}

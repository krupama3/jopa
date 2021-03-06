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
package cz.cvut.kbss.jopa.oom;

import cz.cvut.kbss.jopa.exceptions.StorageAccessException;
import cz.cvut.kbss.jopa.model.MetamodelImpl;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.metamodel.Attribute;
import cz.cvut.kbss.jopa.model.metamodel.EntityType;
import cz.cvut.kbss.jopa.model.metamodel.EntityTypeImpl;
import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.jopa.oom.exceptions.EntityDeconstructionException;
import cz.cvut.kbss.jopa.oom.exceptions.EntityReconstructionException;
import cz.cvut.kbss.jopa.oom.exceptions.UnpersistedChangeException;
import cz.cvut.kbss.jopa.sessions.CacheManager;
import cz.cvut.kbss.jopa.sessions.LoadingParameters;
import cz.cvut.kbss.jopa.sessions.UnitOfWorkImpl;
import cz.cvut.kbss.jopa.utils.Configuration;
import cz.cvut.kbss.jopa.utils.EntityPropertiesUtils;
import cz.cvut.kbss.ontodriver.Connection;
import cz.cvut.kbss.ontodriver.descriptor.*;
import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import cz.cvut.kbss.ontodriver.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cz.cvut.kbss.jopa.exceptions.OWLEntityExistsException.individualAlreadyManaged;

public class ObjectOntologyMapperImpl implements ObjectOntologyMapper, EntityMappingHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectOntologyMapperImpl.class);

    private final UnitOfWorkImpl uow;
    private final CacheManager cache;
    private final Connection storageConnection;
    private final MetamodelImpl metamodel;

    private final AxiomDescriptorFactory descriptorFactory;
    private final EntityConstructor entityBuilder;
    private final EntityDeconstructor entityBreaker;
    private final InstanceRegistry instanceRegistry;
    private final PendingReferenceRegistry pendingReferences;

    private final EntityInstanceLoader defaultInstanceLoader;
    private final EntityInstanceLoader twoStepInstanceLoader;

    public ObjectOntologyMapperImpl(UnitOfWorkImpl uow, Connection connection) {
        this.uow = Objects.requireNonNull(uow);
        this.cache = uow.getLiveObjectCache();
        this.storageConnection = Objects.requireNonNull(connection);
        this.metamodel = uow.getMetamodel();
        this.descriptorFactory = new AxiomDescriptorFactory(uow.getConfiguration());
        this.instanceRegistry = new InstanceRegistry();
        this.pendingReferences = new PendingReferenceRegistry();
        this.entityBuilder = new EntityConstructor(this);
        this.entityBreaker = new EntityDeconstructor(this);

        this.defaultInstanceLoader = DefaultInstanceLoader.builder().connection(storageConnection).metamodel(metamodel)
                                                          .descriptorFactory(descriptorFactory)
                                                          .entityBuilder(entityBuilder).cache(cache).build();
        this.twoStepInstanceLoader = TwoStepInstanceLoader.builder().connection(storageConnection).metamodel(metamodel)
                                                          .descriptorFactory(descriptorFactory)
                                                          .entityBuilder(entityBuilder).cache(cache).build();
    }

    @Override
    public <T> boolean containsEntity(Class<T> cls, URI identifier, Descriptor descriptor) {
        assert cls != null;
        assert identifier != null;
        assert descriptor != null;

        final EntityType<T> et = getEntityType(cls);
        final NamedResource classUri = NamedResource.create(et.getIRI().toURI());
        final Axiom<NamedResource> ax = new AxiomImpl<>(NamedResource.create(identifier),
                Assertion.createClassAssertion(false), new Value<>(classUri));
        try {
            return storageConnection.contains(ax, descriptor.getContext());
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    @Override
    public <T> T loadEntity(LoadingParameters<T> loadingParameters) {
        assert loadingParameters != null;

        instanceRegistry.reset();
        return loadEntityInternal(loadingParameters);
    }

    private <T> T loadEntityInternal(LoadingParameters<T> loadingParameters) {
        final EntityTypeImpl<T> et = getEntityType(loadingParameters.getEntityType());
        final T result;
        if (et.hasSubtypes()) {
            result = twoStepInstanceLoader.loadEntity(loadingParameters);
        } else {
            result = defaultInstanceLoader.loadEntity(loadingParameters);
        }
        if (result != null) {
            cache.add(loadingParameters.getIdentifier(), result, loadingParameters.getDescriptor());
        }
        return result;
    }

    @Override
    public <T> T loadReference(LoadingParameters<T> loadingParameters) {
        assert loadingParameters != null;

        final EntityTypeImpl<T> et = getEntityType(loadingParameters.getEntityType());
        if (et.hasSubtypes()) {
            return twoStepInstanceLoader.loadReference(loadingParameters);
        } else {
            return defaultInstanceLoader.loadReference(loadingParameters);
        }
    }

    @Override
    public <T> EntityTypeImpl<T> getEntityType(Class<T> cls) {
        return metamodel.entity(cls);
    }

    @Override
    public <T> void loadFieldValue(T entity, Field field, Descriptor descriptor) {
        assert entity != null;
        assert field != null;
        assert descriptor != null;

        LOG.trace("Lazily loading value of field {} of entity {}.", field, entity);

        final EntityType<T> et = (EntityType<T>) getEntityType(entity.getClass());
        final URI primaryKey = EntityPropertiesUtils.getIdentifier(entity, et);

        final AxiomDescriptor axiomDescriptor = descriptorFactory.createForFieldLoading(primaryKey,
                field, descriptor, et);
        try {
            final Collection<Axiom<?>> axioms = storageConnection.find(axiomDescriptor);
            if (axioms.isEmpty()) {
                return;
            }
            entityBuilder.setFieldValue(entity, field, axioms, et, descriptor);
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new EntityReconstructionException(e);
        }
    }

    @Override
    public <T> void persistEntity(URI identifier, T entity, Descriptor descriptor) {
        assert entity != null;
        assert descriptor != null;

        @SuppressWarnings("unchecked") final EntityType<T> et = (EntityType<T>) getEntityType(entity.getClass());
        try {
            if (identifier == null) {
                identifier = generateIdentifier(et);
                assert identifier != null;
                EntityPropertiesUtils.setIdentifier(identifier, entity, et);
            }
            entityBreaker.setReferenceSavingResolver(new ReferenceSavingResolver(this));
            final AxiomValueGatherer axiomBuilder = entityBreaker.mapEntityToAxioms(identifier, entity, et, descriptor);
            axiomBuilder.persist(storageConnection);
            persistPendingReferences(entity, axiomBuilder.getSubjectIdentifier());
        } catch (IllegalArgumentException e) {
            throw new EntityDeconstructionException("Unable to deconstruct entity " + entity, e);
        }
    }

    @Override
    public URI generateIdentifier(EntityType<?> et) {
        try {
            return storageConnection.generateIdentifier(et.getIRI().toURI());
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    private <T> void persistPendingReferences(T instance, NamedResource identifier) {
        try {
            final Set<PendingAssertion> pas = pendingReferences.removeAndGetPendingAssertionsWith(instance);
            for (PendingAssertion pa : pas) {
                final AxiomValueDescriptor desc = new AxiomValueDescriptor(pa.getOwner());
                desc.addAssertionValue(pa.getAssertion(), new Value<>(identifier));
                desc.setAssertionContext(pa.getAssertion(), pa.getContext());
                storageConnection.persist(desc);
            }
            final Set<PendingReferenceRegistry.PendingListReference> pLists =
                    pendingReferences.removeAndGetPendingListReferencesWith(instance);
            final EntityType<?> et = getEntityType(instance.getClass());
            for (PendingReferenceRegistry.PendingListReference list : pLists) {
                final ListValueDescriptor desc = list.getDescriptor();
                ListPropertyStrategy.addItemsToDescriptor(desc, list.getValues(), et);
                if (desc instanceof SimpleListValueDescriptor) {
                    // TODO This can be an update or a persist
                    storageConnection.lists().updateSimpleList((SimpleListValueDescriptor) desc);
                } else {
                    storageConnection.lists().updateReferencedList((ReferencedListValueDescriptor) desc);
                }
            }
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    @Override
    public <T> T getEntityFromCacheOrOntology(Class<T> cls, URI identifier, Descriptor descriptor) {
        final T orig = uow.getManagedOriginal(cls, identifier, descriptor);
        if (orig != null) {
            return orig;
        }
        if (cache.contains(cls, identifier, descriptor)) {
            return cache.get(cls, identifier, descriptor);
        } else if (instanceRegistry.containsInstance(identifier, descriptor.getContext())) {
            final Object existing = instanceRegistry.getInstance(identifier, descriptor.getContext());
            if (!cls.isAssignableFrom(existing.getClass())) {
                throw individualAlreadyManaged(identifier);
            }
            // This prevents endless cycles in bidirectional relationships
            return cls.cast(existing);
        } else {
            return loadEntityInternal(new LoadingParameters<>(cls, identifier, descriptor));
        }
    }

    @Override
    public <T> T getOriginalInstance(T clone) {
        assert clone != null;
        return (T) uow.getOriginal(clone);
    }

    boolean isManaged(Object instance) {
        return uow.isObjectManaged(instance);
    }

    <T> void registerInstance(URI primaryKey, T instance, URI context) {
        instanceRegistry.registerInstance(primaryKey, instance, context);
    }

    @Override
    public void checkForUnpersistedChanges() {
        if (pendingReferences.hasPendingResources()) {
            throw new UnpersistedChangeException(
                    "The following instances were neither persisted nor marked as cascade for persist: "
                            + pendingReferences.getPendingResources());
        }
    }

    void registerPendingAssertion(NamedResource owner, Assertion assertion, Object object, URI context) {
        pendingReferences.addPendingAssertion(owner, assertion, object, context);
    }

    void registerPendingListReference(Object item, ListValueDescriptor listDescriptor, List<?> values) {
        pendingReferences.addPendingListReference(item, listDescriptor, values);
    }

    @Override
    public <T> void removeEntity(URI identifier, Class<T> cls, Descriptor descriptor) {
        final EntityType<T> et = getEntityType(cls);
        final AxiomDescriptor axiomDescriptor = descriptorFactory.createForEntityLoading(
                new LoadingParameters<>(cls, identifier, descriptor, true), et);
        try {
            storageConnection.remove(axiomDescriptor);
            pendingReferences.removePendingReferences(axiomDescriptor.getSubject());
        } catch (OntoDriverException e) {
            throw new StorageAccessException("Exception caught when removing entity.", e);
        }
    }

    @Override
    public <T> void updateFieldValue(T entity, Field field, Descriptor entityDescriptor) {
        @SuppressWarnings("unchecked") final EntityType<T> et = (EntityType<T>) getEntityType(entity.getClass());
        final URI pkUri = EntityPropertiesUtils.getIdentifier(entity, et);

        entityBreaker.setReferenceSavingResolver(new ReferenceSavingResolver(this));
        // It is OK to do it like this, because if necessary, the mapping will re-register a pending assertion
        removePendingAssertions(et, field, pkUri);
        final AxiomValueGatherer axiomBuilder = entityBreaker
                .mapFieldToAxioms(pkUri, entity, field, et, entityDescriptor);
        axiomBuilder.update(storageConnection);
    }

    private <T> void removePendingAssertions(EntityType<T> et, Field field, URI identifier) {
        final FieldSpecification<? super T, ?> fs = et.getFieldSpecification(field.getName());
        if (fs instanceof Attribute) {
            final Attribute<?, ?> att = (Attribute<?, ?>) fs;
            // We care only about object property assertions, others are never pending
            final Assertion assertion = Assertion.createObjectPropertyAssertion(att.getIRI().toURI(), att.isInferred());
            pendingReferences.removePendingReferences(NamedResource.create(identifier), assertion);
        }
    }

    @Override
    public Collection<Axiom<NamedResource>> loadSimpleList(SimpleListDescriptor listDescriptor) {
        try {
            return storageConnection.lists().loadSimpleList(listDescriptor);
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    @Override
    public Collection<Axiom<NamedResource>> loadReferencedList(ReferencedListDescriptor listDescriptor) {
        try {
            return storageConnection.lists().loadReferencedList(listDescriptor);
        } catch (OntoDriverException e) {
            throw new StorageAccessException(e);
        }
    }

    @Override
    public Configuration getConfiguration() {
        return uow.getConfiguration();
    }
}

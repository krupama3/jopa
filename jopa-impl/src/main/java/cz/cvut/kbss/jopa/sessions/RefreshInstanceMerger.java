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
package cz.cvut.kbss.jopa.sessions;

import cz.cvut.kbss.jopa.adapters.IndirectCollection;
import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.jopa.sessions.merge.DefaultValueMerger;
import cz.cvut.kbss.jopa.sessions.merge.ValueMerger;
import cz.cvut.kbss.jopa.utils.CollectionFactory;
import cz.cvut.kbss.jopa.utils.EntityPropertiesUtils;

/**
 * Merges changes during instance refresh.
 * <p>
 * This means overwriting any changes made to the entity.
 */
class RefreshInstanceMerger {

    private final ValueMerger merger;

    private final CollectionFactory collectionFactory;

    RefreshInstanceMerger(CollectionFactory collectionFactory) {
        this.collectionFactory = collectionFactory;
        this.merger = new DefaultValueMerger();
    }

    /**
     * Merges changes in the opposite direction, i.e. changes made on the clone are overwritten by the original values.
     *
     * @param changeSet Changes done
     */
    void mergeChanges(ObjectChangeSet changeSet) {
        final Object source = changeSet.getChangedObject();
        final Object target = changeSet.getCloneObject();
        for (ChangeRecord change : changeSet.getChanges()) {
            final FieldSpecification<?, ?> att = change.getAttribute();
            final Object sourceValue = EntityPropertiesUtils.getAttributeValue(att, source);
            if (sourceValue instanceof IndirectCollection) {
                final IndirectCollection col = (IndirectCollection) sourceValue;
                final IndirectCollection<?> ic = collectionFactory
                        .createIndirectCollection(col.getReferencedCollection(), target, att.getJavaField());
                merger.mergeValue(att, target, null, ic, null);
            } else {
                merger.mergeValue(att, target, null, sourceValue, null);
            }
        }
    }
}

package cz.cvut.kbss.jopa.owlapi.metamodel;

import cz.cvut.kbss.jopa.model.IRI;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.metamodel.Attribute;
import cz.cvut.kbss.jopa.owlapi.BasicTypeImpl;
import cz.cvut.kbss.jopa.owlapi.MetamodelImpl;

import java.lang.reflect.Field;

/**
 * @author ledvima1
 */
class DataPropertyAttributes extends PropertyAttributes {

    @Override
    void resolve(Field field, MetamodelImpl metamodel, Class<?> fieldValueCls) {
        super.resolve(field, metamodel, fieldValueCls);
        final OWLDataProperty odp = field.getAnnotation(OWLDataProperty.class);
        assert odp != null;

        this.persistentAttributeType = Attribute.PersistentAttributeType.DATA;
        this.iri = IRI.create(odp.iri());
        this.fetchType = odp.fetch();
        this.type = BasicTypeImpl.get(fieldValueCls);
    }
}

package com.vesanieminen.froniusvisualizer.components.appnav;

import org.vaadin.firitin.fields.ElementCollectionField;

public class LocalizedElementCollectionField<T> extends ElementCollectionField<T> {

    public LocalizedElementCollectionField(Class<T> clazz, Class<?> editorClass) {
        super(clazz, editorClass);
    }

    @Override
    protected String getHeaderForField(String fieldName) {
        return getTranslation(fieldName);
    }

}

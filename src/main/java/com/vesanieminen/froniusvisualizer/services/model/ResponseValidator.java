package com.vesanieminen.froniusvisualizer.services.model;

import com.vesanieminen.froniusvisualizer.util.Utils;

public interface ResponseValidator {

    Object[] getObjects();

    default boolean isValid() {
        return Utils.notNull(getObjects());
    }

}

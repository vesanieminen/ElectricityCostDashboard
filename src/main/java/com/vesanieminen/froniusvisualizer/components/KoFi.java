package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

@Tag("ko-fi")
@JsModule("./src/kofi.ts")
public class KoFi extends Component {

    private static final PropertyDescriptor<String, String> TEXT = PropertyDescriptors.propertyWithDefault("text", "");
    private static final PropertyDescriptor<String, String> URL = PropertyDescriptors.propertyWithDefault("url", "");

    public KoFi(String text, String url) {
        set(TEXT, text);
        set(URL, url);
    }

}

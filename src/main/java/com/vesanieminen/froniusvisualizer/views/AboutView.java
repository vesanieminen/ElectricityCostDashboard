package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;

@Tag("about-view")
@JsModule("src/about-view.ts")
@Route(value = "about", layout = MainLayout.class)
public class AboutView extends Component {

    private static final PropertyDescriptor<String, String> LANGUAGE = PropertyDescriptors.propertyWithDefault("language", "en");

    public AboutView() {
        set(LANGUAGE, getLocale().getLanguage());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
    }

}

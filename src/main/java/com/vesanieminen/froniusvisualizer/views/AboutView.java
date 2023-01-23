package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

@Tag("about-view")
@JsModule("src/about-view.ts")
@Route(value = "tietoja", layout = MainLayout.class)
@RouteAlias(value = "about", layout = MainLayout.class)
public class AboutView extends Component {

    private static final PropertyDescriptor<String, String> LANGUAGE = PropertyDescriptors.propertyWithDefault("language", "en");

    public AboutView() {
        set(LANGUAGE, getLocale().getLanguage());
    }

}

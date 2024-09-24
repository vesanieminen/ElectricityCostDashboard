package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import static com.vesanieminen.froniusvisualizer.util.Utils.setZoomLevel;

@Tag("help-view")
@JsModule("./src/help-view.ts")
@Route(value = "videot", layout = MainLayout.class)
@RouteAlias(value = "ohjeet", layout = MainLayout.class)
@RouteAlias(value = "instructions", layout = MainLayout.class)
public class HelpView extends Component {

    private static final PropertyDescriptor<String, String> LANGUAGE = PropertyDescriptors.propertyWithDefault("language", "en");

    public HelpView() {
        set(LANGUAGE, getLocale().getLanguage());
        setZoomLevel(this);
    }

}

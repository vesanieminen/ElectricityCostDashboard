package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import java.util.Arrays;

@Tag("chart-template")
@JsModule("src/chart-template.ts")
public class ChartTemplate extends Component {

    public ChartTemplate() {
        getElement().setPropertyList("values", Arrays.asList(50d, 100d, 150d));
    }

}

package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.HashMap;
import java.util.stream.Stream;

@Route("aa-calculator")
public class AxisAndAlliesCalculator extends Div {

    private final H3 totalCost;
    private final Div fieldContainer;
    private final String totalCostPrefix;
    private final HashMap<IntegerField, UnitCost> map;

    enum UnitCost {
        INFANTRY(3, "Infantry"),
        ARTILLERY(4, "Artillery"),
        TANK(6, "Tank"),
        ANTIAIRCRAFT_ARTILLERY(5, "AA Gun"),
        INDUSTRIAL_COMPLEX(15, "IC"),
        FIGHTER(10, "Fighter"),
        BOMBER(12, "Bomber"),
        SUBMARINE(6, "Submarine"),
        TRANSPORT(7, "Transport"),
        DESTROYER(8, "Destroyer"),
        CRUISER(12, "Cruiser"),
        AIRCRAFT_CARRIER(14, "Carrier"),
        BATTLESHIP(20, "Battleship");

        public int cost;
        public String name;

        UnitCost(int cost, String name) {
            this.cost = cost;
            this.name = name;
        }
    }

    public AxisAndAlliesCalculator() {
        final var header = new Div();
        header.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Margin.SMALL, LumoUtility.Gap.MEDIUM, LumoUtility.AlignItems.CENTER);
        totalCostPrefix = "Total cost: ";
        totalCost = new H3(totalCostPrefix);
        totalCost.addClassNames(LumoUtility.Margin.Top.SMALL);
        final var clear = new Button("Clear", e -> clear());
        header.add(clear, totalCost);
        add(header);

        fieldContainer = new Div();
        fieldContainer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Margin.SMALL);
        add(fieldContainer);

        map = new HashMap<>();
        Stream.of(UnitCost.values()).forEach(this::addField);
    }

    private void addField(UnitCost item) {
        final var integerField = createIntegerField(item);
        map.put(integerField, item);
    }

    private IntegerField createIntegerField(UnitCost unitCost) {
        var integerField = new IntegerField(unitCost.name + " " + unitCost.cost);
        integerField.setMin(0);
        integerField.setWidth("110px");
        integerField.addClassNames(LumoUtility.Margin.XSMALL);
        integerField.setValue(0);
        integerField.setStepButtonsVisible(true);
        integerField.addValueChangeListener(e -> calculate());
        fieldContainer.add(integerField);
        return integerField;
    }

    private void calculate() {
        final var sum = map.keySet().stream().map(this::getFieldValue).reduce(0, Integer::sum);
        totalCost.setText(totalCostPrefix + sum);
    }

    private int getFieldValue(IntegerField component) {
        return map.get(component).cost * component.getValue();
    }

    private void clear() {
        map.keySet().forEach(field -> field.setValue(0));
        totalCost.setText(totalCostPrefix);
    }

}

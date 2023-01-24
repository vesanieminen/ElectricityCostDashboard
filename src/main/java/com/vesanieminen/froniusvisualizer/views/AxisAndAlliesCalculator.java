package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.HashMap;

@Route("aa-calculator")
public class AxisAndAlliesCalculator extends Div {

    private final H3 totalCost;
    private final Div fieldContainer;
    private final String totalCostPrefix;
    private final HashMap<IntegerField, UnitCost> map;

    enum UnitCost {
        INFANTRY(3),
        ARTILLERY(4),
        TANK(6),
        ANTIAIRCRAFT_ARTILLERY(5),
        INDUSTRIAL_COMPLEX(15),
        FIGHTER(10),
        BOMBER(12),
        SUBMARINE(6),
        TRANSPORT(7),
        DESTROYER(8),
        CRUISER(12),
        AIRCRAFT_CARRIER(14),
        BATTLESHIP(20);

        public int cost;

        UnitCost(int cost) {
            this.cost = cost;
        }
    }

    private final IntegerField infantryField;
    private final IntegerField artilleryField;
    private final IntegerField tankField;
    private final IntegerField aaField;
    private final IntegerField icField;
    private final IntegerField fighterField;
    private final IntegerField bomberField;
    private final IntegerField submarineField;
    private final IntegerField transportField;
    private final IntegerField destroyerField;
    private final IntegerField cruiserField;
    private final IntegerField aircraftCarrierField;
    private final IntegerField battleshipField;

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

        infantryField = createIntegerField("Infantry - 3");
        artilleryField = createIntegerField("Artillery - 4");
        tankField = createIntegerField("Tank - 6");
        aaField = createIntegerField("AA Gun - 5");
        icField = createIntegerField("IC - 15");
        fighterField = createIntegerField("Fighter - 10");
        bomberField = createIntegerField("Bomber - 12");
        submarineField = createIntegerField("Submarine - 6");
        transportField = createIntegerField("Transport - 7");
        destroyerField = createIntegerField("Destroyer - 8");
        cruiserField = createIntegerField("Cruiser - 12");
        aircraftCarrierField = createIntegerField("Carrier - 14");
        battleshipField = createIntegerField("Battleship 20");

        map = new HashMap<>();
        map.put(infantryField, UnitCost.INFANTRY);
        map.put(artilleryField, UnitCost.ARTILLERY);
        map.put(tankField, UnitCost.TANK);
        map.put(aaField, UnitCost.ANTIAIRCRAFT_ARTILLERY);
        map.put(icField, UnitCost.INDUSTRIAL_COMPLEX);
        map.put(fighterField, UnitCost.FIGHTER);
        map.put(bomberField, UnitCost.BOMBER);
        map.put(submarineField, UnitCost.SUBMARINE);
        map.put(transportField, UnitCost.TRANSPORT);
        map.put(destroyerField, UnitCost.DESTROYER);
        map.put(cruiserField, UnitCost.CRUISER);
        map.put(aircraftCarrierField, UnitCost.AIRCRAFT_CARRIER);
        map.put(battleshipField, UnitCost.BATTLESHIP);
    }

    private IntegerField createIntegerField(String name) {
        var infantryField = new IntegerField(name);
        infantryField.setMin(0);
        infantryField.setWidth("110px");
        infantryField.addClassNames(LumoUtility.Margin.XSMALL);
        infantryField.setValue(0);
        infantryField.setStepButtonsVisible(true);
        infantryField.addValueChangeListener(e -> calculate());
        fieldContainer.add(infantryField);
        return infantryField;
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

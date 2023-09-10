package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("admin")
public class AdminView extends Div {

    public AdminView() {
        final var updateFingrid = new Button(getTranslation("Update Fingrid data"));
        //updateFingrid.addClickListener(e -> {
        //    FingridService.updateWindEstimateData();
        //    try {
        //        TimeUnit.MILLISECONDS.sleep(500);
        //        FingridService.updateProductionEstimateData();
        //        TimeUnit.MILLISECONDS.sleep(500);
        //        FingridService.updateConsumptionEstimateData();
        //        TimeUnit.MILLISECONDS.sleep(500);
        //        FingridService.updateRealtimeData();
        //    } catch (InterruptedException ex) {
        //        throw new RuntimeException(ex);
        //    }
        //});
        //add(updateFingrid);
    }

}

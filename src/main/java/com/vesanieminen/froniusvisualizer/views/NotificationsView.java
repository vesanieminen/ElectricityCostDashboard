package com.vesanieminen.froniusvisualizer.views;

import com.vesanieminen.froniusvisualizer.services.model.PriceNotification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.services.NotificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.vaadin.firitin.components.select.VSelect;
import org.vaadin.firitin.components.textfield.VIntegerField;
import org.vaadin.firitin.components.textfield.VNumberField;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.fields.ElementCollectionField;
import org.vaadin.firitin.util.WebStorage;

@Route(layout = MainLayout.class)
public class NotificationsView extends VerticalLayout {

    private String uid;
    private final NotificationService service;

    public static class PriceNotificationEditor {

        Checkbox enabled = new Checkbox();
        NumberField price = new VNumberField().withWidth("5em");
        VSelect<Boolean> up = new VSelect<Boolean>()
                .withItems(Boolean.TRUE, Boolean.FALSE)
                .withItemLabelGenerator(b -> b ? "↗" : "↘︎")
                .withWidth("5em");
        // VIntegerField prewarn = new VIntegerField().withWidth("5em");
        VIntegerField timeout = new VIntegerField().withWidth("5em");
        VTextField extraMsg = new VTextField();

    }

    private List<PriceNotification> notifications;

    public NotificationsView(NotificationService service) {
        this.service = service;
        add(new H1("Notifications"));

        WebStorage.getItem("uid", uid -> {
            if (uid == null) {
                uid = UUID.randomUUID().toString();
                WebStorage.setItem("uid", uid);
                notifications = new ArrayList<>();
            } else {
                notifications = service.listNotifications(uid);
            }
            this.uid = uid;
            bindData();
        });

        add(new Paragraph("""
Tällä näytöllä voit konfiguraoida itsellesi notifikaatiot hintojen heilahteluista.
Voit asentaa maksimissaan 4 sääntöä.

Huom. Applen iOS laitteilla sovellus tulee "asentaa kotinäytölle", 
jotta notificaatiot toimivat.    			
    			"""));

    }

    private void save() {
        service.saveNotifications(uid, notifications);
    }

    private void bindData() {
        ElementCollectionField<PriceNotification> elementCollectionField = new ElementCollectionField<>(PriceNotification.class, PriceNotificationEditor.class);

        elementCollectionField.setValue(notifications);

        add(elementCollectionField);

        add(new Button("Save", e -> save()));
        
    }

}

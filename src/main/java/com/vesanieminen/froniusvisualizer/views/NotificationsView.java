package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.UI;
import com.vesanieminen.froniusvisualizer.services.model.PriceNotification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vesanieminen.froniusvisualizer.services.NotificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nl.martijndwars.webpush.Subscription;
import org.vaadin.firitin.components.button.DeleteButton;
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
//        VIntegerField timeout = new VIntegerField().withWidth("5em");
        VTextField extraMsg = new VTextField();
    }

    private List<PriceNotification> notifications;
    
    Button requestNotificationsBtn;

    public NotificationsView(NotificationService service) {
        this.service = service;
        add(new H1("Notifications"));
        add(new Paragraph("""
Tällä näytöllä voit konfiguraoida itsellesi notifikaatiot hintojen heilahteluista.

Huom. Applen iOS laitteilla sovellus tulee "asentaa kotinäytölle", 
jotta notificaatiot toimivat.    			
    			"""));

        WebStorage.getItem("uid", uid -> {
            if (uid == null) {
                this.uid = UUID.randomUUID().toString();
                requestToAllowNotifications();
                addSubscribeListener(e -> {
                    WebStorage.setItem("uid", this.uid);
                    service.subscribe(this.uid, e.getSubscription());
                    notifications = new ArrayList<>();
                    bindData();
                    remove(requestNotificationsBtn);
                });
            } else {
                this.uid = uid;
                notifications = service.listNotifications(uid);
                bindData();
            }
        });

    }

    private void save() {
        service.saveNotifications(uid, notifications);
        UI.getCurrent().navigate("");
    }

    private void bindData() {
        ElementCollectionField<PriceNotification> elementCollectionField = new ElementCollectionField<>(PriceNotification.class, PriceNotificationEditor.class);

        elementCollectionField.setValue(notifications);

        add(elementCollectionField);

        add(new Button("Save", e -> save()));
        
        add(new DeleteButton("Clear and stop notifications", () -> {
            unsubscribe();
        }));
    }
    
    
    public void requestToAllowNotifications() {
        requestNotificationsBtn = new Button("Subscribe...");
        add(requestNotificationsBtn);
        // Notifications need to be requested from direct user interaction
        requestNotificationsBtn.getElement().executeJs("""
    const keyfromserver = $0;
    const padding = '='.repeat((4 - (keyfromserver.length % 4)) % 4);
    const base64 = (keyfromserver + padding).replace(/\\-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const serverKey = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) {
        serverKey[i] = rawData.charCodeAt(i);
    }

    this.addEventListener("click", () => {
        Notification.requestPermission().then( notificationPermission => {
            if (notificationPermission === 'granted') {
                navigator.serviceWorker.getRegistration().then(registration => {
                    registration.pushManager.subscribe({
                        userVisibleOnly: true,
                        applicationServerKey: serverKey,
                    }).then(subscription => {
                        this.dispatchEvent(new CustomEvent('web-push-subscribed', {
                            bubbles: true,
                            composed: true,
                            // Serialize keys uint8array -> base64
                            detail: JSON.parse(JSON.stringify(subscription))
                        }));
                    }).catch( (er) => {
                        console.error(er);
                    });
                });
            } else {
                window.alert("No fun then!");
            }
        });
    });
                           """, service.getPublicKey());

    }
    
    private void unsubscribe() {
        WebStorage.clear();
        service.unsubscribe(uid);

        getElement().executeJs("""
navigator.serviceWorker.ready.then((reg) => {
reg.pushManager.getSubscription().then((subscription) => {
  subscription
    .unsubscribe()
    .then((successful) => {
      // You've successfully unsubscribed
    })
    .catch((e) => {
        console.log(e);
    });
});
});                               
                               
        """).then(r -> UI.getCurrent().getPage().reload());
    }
    
        // Events

    public static class WebPushSubscriptionEvent extends ComponentEvent<NotificationsView> {
        private final Subscription subscription;

        public WebPushSubscriptionEvent(NotificationsView source,
                                        boolean fromClient,
                                        Subscription subscription) {
            super(source, fromClient);
            this.subscription = subscription;
        }

        public Subscription getSubscription() {
            return subscription;
        }
    }

    @DomEvent("web-push-subscribed")
    public static class SubscribeEvent extends WebPushSubscriptionEvent {
        public SubscribeEvent(
                NotificationsView source,
                boolean fromClient,
                @EventData("event.detail.endpoint") String endpoint,
                @EventData("event.detail.keys.auth") String auth,
                @EventData("event.detail.keys.p256dh") String p256dh) {
            super(source, fromClient, new Subscription(endpoint, new Subscription.Keys(p256dh, auth)));
        }
    }

    public Registration addSubscribeListener(ComponentEventListener<SubscribeEvent> listener) {
        return addListener(SubscribeEvent.class, listener);
    }

}

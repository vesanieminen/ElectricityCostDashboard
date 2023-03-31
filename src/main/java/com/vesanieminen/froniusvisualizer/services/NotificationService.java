package com.vesanieminen.froniusvisualizer.services;

import com.vesanieminen.froniusvisualizer.services.model.PriceNotification;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    
    @Value("${vapid.public.key}")
    private String publicKey;
    @Value("${vapid.private.key}")
    private String privateKey;
    @Value("${vapid.subject}")
    private String subject;

    private PushService pushService;
    
    

    // TODO persist to PostgreSQL with jdbc template before releasing publicly
    List<PriceNotification> notifications = new ArrayList<>();

    public List<PriceNotification> listNotifications(String userId) {
        return new ArrayList<>(notifications.stream().filter(pn -> pn.getUid().equals(userId)).collect(Collectors.toList()));
    }

    public void saveNotifications(String userId, List<PriceNotification> notifications) {
        log.info("Updating notifications for " + userId);
        
        for (PriceNotification notification : notifications) {
            notification.setUid(userId);
        }
        clearAll(userId);
        this.notifications.addAll(notifications);
    }

    private void clearAll(String userId) {
        Iterator<PriceNotification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            PriceNotification pn = iterator.next();
            if (pn.getUid().equals(userId)) {
                iterator.remove();
            }
        }
    }

    @Scheduled(cron = "0 * * * *")
    public void sendNotifications() {
        log.info("Starting to send hourly notifications...");

        Instant now = Instant.now();
        Double priceNow = 30.0; // TODO
        Double previousPrice = 1.0; // TODO
        boolean up = priceNow > previousPrice;
        Double lowerBound = up ? previousPrice : priceNow;
        Double upperBound = up ? priceNow : previousPrice;
        
        // TODO add support for pre-warning

        notifications.stream()
                .filter(PriceNotification::isEnabled)
                .filter(pn -> pn.isUp() == up)
                .filter(pn -> pn.getPrice() >= lowerBound && pn.getPrice() <= upperBound )
                .forEach(pn -> {
                    // TODO send all notifications
                    
                });
        log.info("All notifications sent...");
    }

}

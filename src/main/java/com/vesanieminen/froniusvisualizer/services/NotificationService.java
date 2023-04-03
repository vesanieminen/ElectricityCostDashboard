package com.vesanieminen.froniusvisualizer.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.services.model.PriceNotification;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    Map<String,Subscription> uidToSubscription = new HashMap<>(); 
    
    File uidsubfile = new File("uidsub.json");
    File notificationsfile = new File("notifications.json");
    
    @PostConstruct
    private void init() throws GeneralSecurityException {
        try {
            notifications = mapper.readValue(notificationsfile, new TypeReference<>() {
            });
            uidToSubscription = mapper.readValue(uidsubfile, new TypeReference<HashMap<String, Subscription>>() {
            });
        } catch( Exception e) {
            log.info("couln't read old data", e);
        }
        
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(publicKey, privateKey, subject);
    }
    
    @PreDestroy
    private void serialize() {
        try {
            mapper.writeValue(uidsubfile, uidToSubscription);
            mapper.writeValue(notificationsfile, notifications);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NotificationService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NotificationService.class.getName()).log(Level.SEVERE, null, ex);
        }
    
    }


    public List<PriceNotification> listNotifications(String userId) {
        return new ArrayList<>(notifications.stream().filter(pn -> pn.getUid().equals(userId)).collect(Collectors.toList()));
    }

    public void saveNotifications(String userId, List<PriceNotification> notifications) {
        log.info("Updating notifications for " + userId);
        Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);
        
        for (PriceNotification notification : notifications) {
            notification.setUid(userId);
            notification.setLastTriggered(now);
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

    @Scheduled(cron = "0 0 * * * *")
    public void sendNotifications() {
        log.info("Starting to send hourly notifications...");

        Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant nowMinus1Hour = now.minus(1, ChronoUnit.HOURS);

        List<NordpoolPrice> latest7DaysList = NordpoolSpotService.getLatest7DaysList();
        NordpoolPrice prev = latest7DaysList.stream().filter(p->p.timeInstant().equals(nowMinus1Hour)).findFirst().orElse(null);
        NordpoolPrice current = latest7DaysList.stream().filter(p->p.timeInstant().equals(now)).findFirst().orElse(null);

        if(prev == null || current == null) {
            log.info("Couldn't find previous or current price, skipping");
            return;
        }

        final Double priceNow = current.price()*getVat(current.timeInstant());
        final Double previousPrice = prev.price()*getVat(prev.timeInstant());
        final boolean up = priceNow > previousPrice;
        final Double lowerBound = up ? previousPrice : priceNow;
        final Double upperBound = up ? priceNow : previousPrice;
        Set<PriceNotification> uidsToClean = new HashSet<>();
        
        notifications.stream()
               .filter(PriceNotification::isEnabled)
                .filter(pn -> pn.getLastTriggered().plus(pn.getTimeout(),ChronoUnit.HOURS).isBefore(now))
                .filter(pn -> pn.isUp() == up)
                .filter(pn -> pn.getPrice() >= lowerBound && pn.getPrice() <= upperBound )
                .forEach(pn -> {
                    String body =
                            "Price now %s c/kWh. %s".formatted(priceNow, pn.getExtraMsg());
                    String title = up ? "Prices going up!": "Prices going down!";
                    Message message = new Message(title, body);
                    Subscription s = uidToSubscription.get(pn.getUid());
                    if(s != null) {
                       sendNotification(s, message);
                       pn.setLastTriggered(now);
                    } else {
                        uidsToClean.add(pn);
                    }
                });
        notifications.removeAll(uidsToClean);
        log.info("All notifications  sent...");
    }

    private double getVat(Instant timeInstant) {
        return timeInstant.isBefore(Instant.parse("2023-05-01T03:00:00Z")) ? 1.10 : 1.24;
    }

    void sendNotification(Subscription subscription, Message msg) {
        log.info("sendNotification: " + subscription.endpoint);
        try {
            HttpResponse response = pushService.send(new Notification(subscription, mapper.writeValueAsString(msg)));
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 201) {
                System.out.println("Server error, status code:" + statusCode);
                InputStream content = response.getEntity().getContent();
                List<String> strings = IOUtils.readLines(content, "UTF-8");
                System.out.println(strings);
            }
        } catch (GeneralSecurityException | IOException | JoseException | ExecutionException
                 | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String uid, Subscription subscription) {
        uidToSubscription.put(uid, subscription);
    }

    public void unsubscribe(String uid) {
        uidToSubscription.remove(uid);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public record Message(String title, String body) {
        
    }

    ObjectMapper mapper = new ObjectMapper();
    
}

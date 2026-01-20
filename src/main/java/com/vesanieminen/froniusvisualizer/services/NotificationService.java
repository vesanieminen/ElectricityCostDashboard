package com.vesanieminen.froniusvisualizer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.services.model.NotificationSubscription;
import com.vesanieminen.froniusvisualizer.services.model.PriceNotification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class NotificationService {
    
    @Value("${vapid.public.key}")
    private String publicKey;
    @Value("${vapid.private.key}")
    private String privateKey;
    @Value("${vapid.subject}")
    private String subject;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    NotificationRepository repository;

    private PushService pushService;

    File uidsubfile = new File("uidsub.json");
    File notificationsfile = new File("notifications.json");
    
    @PostConstruct
    private void init() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        if(!privateKey.isEmpty()) {
            pushService = new PushService(publicKey, privateKey, subject);
        } else {
            log.warn("No VAPID keys found, notifications disabled");
        }
    }

    public List<PriceNotification> listNotifications(UUID userId) {
        return new ArrayList<>(repository.findBySubscriptionId(userId));
    }

    public void saveNotifications(UUID userId, List<PriceNotification> notifications) {
        log.info("Updating notifications for " + userId);
        Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);

        for (PriceNotification notification : notifications) {
            notification.setSubscriptionId(userId);
        }
        repository.saveAll(notifications);
        repository.findBySubscriptionId(userId).forEach(n -> {
            if (!notifications.contains(n)) {
                repository.delete(n);
            }
        });
    }

    private void clearAll(UUID userId) {
        repository.deleteAllBySubscriptionId(userId);
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
        final double priceNow = current.price()*getVat(current.timeInstant());
        final double previousPrice = prev.price()*getVat(prev.timeInstant());
        final boolean up = priceNow > previousPrice;

        List<NordpoolPrice> futurePrices = latest7DaysList.subList(latest7DaysList.indexOf(current), latest7DaysList.size());
        Iterator<NordpoolPrice> priceIterator = futurePrices.iterator();
        NordpoolPrice nextPeakLow = current;
        while (priceIterator.hasNext()) {
            NordpoolPrice price = priceIterator.next();
            if(up) {
                if(price.price() > current.price()) {
                    nextPeakLow = price;
                } else {
                    break;
                }
            } else {
                if(price.price() < current.price()) {
                    nextPeakLow = price;
                } else {
                    break;
                }
            }
        }
        final String peakLowMsg;
        if(nextPeakLow != current) {
            LocalTime peakTime = LocalTime.ofInstant(nextPeakLow.timeInstant(), ZoneId.of("Europe/Helsinki"));
            peakLowMsg = (" Next " + (up ? "peak" : "low") + ": %.2f c/kWh at %s.").formatted(nextPeakLow.price()*getVat(nextPeakLow.timeInstant()), peakTime);
        } else {
            peakLowMsg = "";
        }

        final double lowerBound = up ? previousPrice : priceNow;
        final double upperBound = up ? priceNow : previousPrice;
        Set<PriceNotification> orphaned = new HashSet<>();

        // TODO enabled, bounds & direction to query
        Iterable<PriceNotification> all = repository.findAll();
        all.forEach(pn -> {
            if(pn.isEnabled() && pn.isUp() == up && pn.getPrice() >= lowerBound && pn.getPrice() <= upperBound) {
                String body =
                        "Price now %.2f c/kWh. %s".formatted(priceNow, pn.getExtraMsg())
                                + peakLowMsg;
                String title = up ? "Prices going up!": "Prices going down!";
                Message message = new Message(title, body);
                Subscription s = getSubscription(pn.getSubscriptionId());
                sendNotification(s, message);
            }
        });

        log.info("All notifications  sent...");
    }

    private Subscription getSubscription(UUID uid) {
        NotificationSubscription ns = subscriptionRepository.findById(uid).get();
        return new Subscription(ns.getEndpoint(), new Subscription.Keys(ns.getP256dh(), ns.getAuth()));
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

    public UUID subscribe(Subscription subscription) {
        NotificationSubscription saved = subscriptionRepository.save(new NotificationSubscription(subscription.endpoint, subscription.keys.p256dh, subscription.keys.auth));
        return saved.getId();
    }

    public void unsubscribe(UUID uid) {
        repository.deleteAllBySubscriptionId(uid);
        subscriptionRepository.deleteById(uid);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public record Message(String title, String body) {
        
    }

    ObjectMapper mapper = new ObjectMapper();
    
}

package com.vesanieminen.froniusvisualizer.services.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 *
 * @author mstahv
 */
@Getter
@Setter
@Table("subscription")
public class NotificationSubscription {
    @Id
    UUID id = UUID.randomUUID();
    @Version
    int version;
    String endpoint;
    String p256dh;
    String auth;

    public NotificationSubscription(String endpoint, String p256dh, String auth) {
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
    }
}

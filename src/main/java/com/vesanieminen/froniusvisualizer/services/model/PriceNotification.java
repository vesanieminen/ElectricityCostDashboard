package com.vesanieminen.froniusvisualizer.services.model;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 *
 * @author mstahv
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table("notification")
public class PriceNotification {
    @Id
    @EqualsAndHashCode.Include
    UUID id = UUID.randomUUID();
    @Version
    int version;
    UUID subscriptionId;
    boolean enabled;
    double price = 20;
    boolean up = true;
    String extraMsg = "";

}

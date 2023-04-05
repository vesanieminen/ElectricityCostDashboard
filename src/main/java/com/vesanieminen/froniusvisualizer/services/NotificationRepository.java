package com.vesanieminen.froniusvisualizer.services;

import com.vesanieminen.froniusvisualizer.services.model.PriceNotification;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends CrudRepository<PriceNotification, UUID> {

    List<PriceNotification> findBySubscriptionId(UUID userId);

    void deleteAllBySubscriptionId(UUID userId);
}

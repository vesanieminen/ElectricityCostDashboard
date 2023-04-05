package com.vesanieminen.froniusvisualizer.services;

import com.vesanieminen.froniusvisualizer.services.model.NotificationSubscription;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface SubscriptionRepository extends CrudRepository<NotificationSubscription, UUID> {
}

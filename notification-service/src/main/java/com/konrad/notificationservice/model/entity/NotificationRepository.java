package com.konrad.notificationservice.model.entity;

import com.konrad.notificationservice.model.entity.Notification;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class NotificationRepository {

    private final Map<String, Notification> store = new ConcurrentHashMap<>();

    public Notification save(Notification n) {
        if (n.getId() == null) n.setId("notif-" + UUID.randomUUID().toString().substring(0, 8));
        store.put(n.getId(), n);
        return n;
    }

    public List<Notification> findAll() { return new ArrayList<>(store.values()); }

    public List<Notification> findByDestinatario(String email) {
        return store.values().stream()
            .filter(n -> n.getDestinatario().equalsIgnoreCase(email))
            .sorted(Comparator.comparing(Notification::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
}

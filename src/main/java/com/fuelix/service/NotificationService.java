package com.fuelix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuelix.model.Notification;
import com.fuelix.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public Notification createPrivateNotification(Long userId, String title, String message, Map<String, Object> data) {
        String dataJson = null;
        try {
            if (data != null) {
                dataJson = objectMapper.writeValueAsString(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Notification notification = new Notification(title, message, "PRIVATE", userId, dataJson);
        Notification saved = notificationRepository.save(notification);
        System.out.println("Private notification created for user: " + userId);

        return saved;
    }

    @Transactional
    public Notification createPublicNotification(String title, String message, Map<String, Object> data) {
        String dataJson = null;
        try {
            if (data != null) {
                dataJson = objectMapper.writeValueAsString(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Notification notification = new Notification(title, message, "PUBLIC", null, dataJson);
        Notification saved = notificationRepository.save(notification);
        System.out.println("Public notification created: " + title);

        return saved;
    }

    @Transactional
    public void notifyFuelLogCreated(Long userId, String vehicleName, double litres, String fuelGrade, double totalCost) {
        String title = "Fuel Log Added";
        String message = String.format("You added %.1f L of %s to %s. Total cost: LKR %.2f",
                litres, fuelGrade, vehicleName, totalCost);

        Map<String, Object> data = new HashMap<>();
        data.put("type", "FUEL_LOG");
        data.put("litres", litres);
        data.put("fuelGrade", fuelGrade);
        data.put("vehicleName", vehicleName);
        data.put("totalCost", totalCost);

        createPrivateNotification(userId, title, message, data);
    }

    @Transactional
    public void notifyBulkQuotaUpdate(List<QuotaChange> changes, String updatedBy) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        String title = "Weekly Quota Updates";

        // Build detailed message with all changes
        StringBuilder messageBuilder = new StringBuilder("Weekly fuel quotas have been updated:\n\n");
        for (QuotaChange change : changes) {
            messageBuilder.append(String.format("• %s: %.0fL → %.0fL\n",
                    change.getVehicleType(), change.getOldQuota(), change.getNewQuota()));
        }
        messageBuilder.append(String.format("\nChanges effective from %s.", getNextMonday().format(formatter)));

        Map<String, Object> data = new HashMap<>();
        data.put("type", "QUOTA_UPDATE");
        data.put("changes", changes);
        data.put("updatedBy", updatedBy);
        data.put("effectiveDate", getNextMonday().format(formatter));
        data.put("changeCount", changes.size());

        createPublicNotification(title, messageBuilder.toString(), data);
    }

    @Transactional
    public void notifyBulkFuelPriceUpdate(List<FuelPriceChange> changes, String updatedBy) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        String title = "Fuel Price Updates";

        // Build detailed message with all changes
        StringBuilder messageBuilder = new StringBuilder("Fuel prices have been updated:\n\n");
        for (FuelPriceChange change : changes) {
            messageBuilder.append(String.format("• %s: LKR %.2f → LKR %.2f\n",
                    change.getFuelGrade(), change.getOldPrice(), change.getNewPrice()));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("type", "PRICE_UPDATE");
        data.put("changes", changes);
        data.put("updatedBy", updatedBy);
        data.put("updatedAt", LocalDateTime.now().format(formatter));
        data.put("changeCount", changes.size());

        createPublicNotification(title, messageBuilder.toString(), data);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.getUserNotifications(userId);
    }

    private LocalDateTime getNextMonday() {
        LocalDateTime now = LocalDateTime.now();
        int daysUntilMonday = (8 - now.getDayOfWeek().getValue()) % 7;
        if (daysUntilMonday == 0) daysUntilMonday = 7;
        return now.plusDays(daysUntilMonday).withHour(0).withMinute(0).withSecond(0);
    }

    // Inner class for quota change
    public static class QuotaChange {
        private String vehicleType;
        private Double oldQuota;
        private Double newQuota;

        public QuotaChange() {}

        public QuotaChange(String vehicleType, Double oldQuota, Double newQuota) {
            this.vehicleType = vehicleType;
            this.oldQuota = oldQuota;
            this.newQuota = newQuota;
        }

        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
        public Double getOldQuota() { return oldQuota; }
        public void setOldQuota(Double oldQuota) { this.oldQuota = oldQuota; }
        public Double getNewQuota() { return newQuota; }
        public void setNewQuota(Double newQuota) { this.newQuota = newQuota; }
    }

    // Inner class for fuel price change
    public static class FuelPriceChange {
        private String fuelGrade;
        private Double oldPrice;
        private Double newPrice;

        public FuelPriceChange() {}

        public FuelPriceChange(String fuelGrade, Double oldPrice, Double newPrice) {
            this.fuelGrade = fuelGrade;
            this.oldPrice = oldPrice;
            this.newPrice = newPrice;
        }

        public String getFuelGrade() { return fuelGrade; }
        public void setFuelGrade(String fuelGrade) { this.fuelGrade = fuelGrade; }
        public Double getOldPrice() { return oldPrice; }
        public void setOldPrice(Double oldPrice) { this.oldPrice = oldPrice; }
        public Double getNewPrice() { return newPrice; }
        public void setNewPrice(Double newPrice) { this.newPrice = newPrice; }
    }
}
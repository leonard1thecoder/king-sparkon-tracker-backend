package com.king_sparkon_tracker.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InventoryTransactionQrCodeTest {

    @Test
    void websitePaymentCreatesQrCodeForPurchaseUrl() {
        InventoryTransaction transaction = transaction();

        transaction.markWebsitePaymentPending(
                "customer@example.com",
                "customer@example.com",
                "plink_123",
                "https://pay.stripe.com/link/123");

        assertThat(transaction.getPaymentQrCodeUrl()).contains("qrserver");
        assertThat(transaction.getPaymentQrCodeUrl()).contains("pay.stripe.com%2Flink%2F123");
    }

    @Test
    void offlinePaymentDoesNotCreatePurchaseQrCode() {
        InventoryTransaction transaction = transaction();

        transaction.markOfflinePayment(TransactionPaymentType.CASH, null);

        assertThat(transaction.getPaymentQrCodeUrl()).isNull();
    }

    private InventoryTransaction transaction() {
        TrackerUser owner = new TrackerUser("owner", "owner@example.com", "encoded", new Privilege(PrivilegeRole.Owner));
        Business business = new Business("Owner Store", owner);
        owner.setBusiness(business);
        TrackerUser worker = new TrackerUser("worker", "worker@example.com", "encoded", new Privilege(PrivilegeRole.Worker));
        worker.setBusiness(business);
        return new InventoryTransaction(TransactionType.SELL, worker, owner, business);
    }
}

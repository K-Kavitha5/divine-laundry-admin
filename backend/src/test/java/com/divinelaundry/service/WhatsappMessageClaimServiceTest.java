package com.divinelaundry.service;

import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsappMessageClaimServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");
    private static final String KEY = "INVOICE_IMAGE:INV-1";

    @Test
    void firstRequestSuccessfullyClaims() {
        WhatsappMessageRepository repository = repositoryReturning(1);
        WhatsappMessage message = message();
        when(repository.findByDeduplicationKey(KEY)).thenReturn(Optional.of(message));

        Optional<WhatsappMessage> result = claimService(repository).claim(KEY);

        assertThat(result).containsSame(message);
        verify(repository).claimForDelivery(KEY, NOW, NOW.minus(Duration.ofMinutes(15)));
    }

        @Test
        void pendingTimeoutMustBePositive() {
        WhatsappMessageRepository repository = mock(WhatsappMessageRepository.class);

        assertThatThrownBy(() -> new WhatsappMessageClaimService(repository, Clock.systemUTC(), "PT0S"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WHATSAPP_PENDING_TIMEOUT");
        assertThatThrownBy(() -> new WhatsappMessageClaimService(repository, Clock.systemUTC(), "not-a-duration"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WHATSAPP_PENDING_TIMEOUT");
        }

    @Test
    void activePendingCannotBeClaimed() {
        assertNoClaimWhenUpdateAffectsZeroRows();
    }

    @Test
    void sentCannotBeClaimed() {
        assertNoClaimWhenUpdateAffectsZeroRows();
    }

    @Test
    void deliveredCannotBeClaimed() {
        assertNoClaimWhenUpdateAffectsZeroRows();
    }

    @Test
    void failedMessageCanBeReclaimedForManualRetry() {
        assertThat(claimService(repositoryReturning(1)).claim(KEY)).isPresent();
    }

    @Test
    void stalePendingCanBeReclaimed() {
        assertThat(claimService(repositoryReturning(1)).claim(KEY)).isPresent();
    }

    @Test
    void freshPendingCannotBeReclaimed() {
        assertNoClaimWhenUpdateAffectsZeroRows();
    }

    @Test
    void concurrentClaimAttemptsHaveExactlyOneSuccessfulClaimant() throws Exception {
        WhatsappMessageRepository repository = mock(WhatsappMessageRepository.class);
        AtomicBoolean claimed = new AtomicBoolean();
        AtomicInteger successfulUpdates = new AtomicInteger();
        when(repository.claimForDelivery(eq(KEY), any(), any())).thenAnswer(invocation -> {
            if (!claimed.compareAndSet(false, true)) return 0;
            successfulUpdates.incrementAndGet();
            return 1;
        });
        WhatsappMessage message = message();
        when(repository.findByDeduplicationKey(KEY)).thenReturn(Optional.of(message));
        WhatsappMessageClaimService service = claimService(repository);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> awaitAndClaim(service, start));
            var second = pool.submit(() -> awaitAndClaim(service, start));
            start.countDown();
            assertThat(first.get()).isNotEqualTo(second.get());
            assertThat(successfulUpdates).hasValue(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private void assertNoClaimWhenUpdateAffectsZeroRows() {
        WhatsappMessageRepository repository = repositoryReturning(0);
        assertThat(claimService(repository).claim(KEY)).isEmpty();
        verify(repository, never()).findByDeduplicationKey(any());
    }

    private static Optional<WhatsappMessage> awaitAndClaim(
            WhatsappMessageClaimService service, CountDownLatch start) {
        try {
            start.await();
            return service.claim(KEY);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    private static WhatsappMessageClaimService claimService(WhatsappMessageRepository repository) {
        return new WhatsappMessageClaimService(repository,
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(15));
    }

    private static WhatsappMessageRepository repositoryReturning(int rows) {
        WhatsappMessageRepository repository = mock(WhatsappMessageRepository.class);
        when(repository.claimForDelivery(any(), any(), any())).thenReturn(rows);
        if (rows == 1) {
            when(repository.findByDeduplicationKey(KEY)).thenReturn(Optional.of(message()));
        }
        return repository;
    }

    private static WhatsappMessage message() {
        return new WhatsappMessage(KEY, null, "9876543210", "template");
    }
}
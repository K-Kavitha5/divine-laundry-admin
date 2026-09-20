package com.divinelaundry.service;

import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class WhatsappMessageClaimService {
    private final WhatsappMessageRepository messages;
    private final Clock clock;
    private final Duration pendingTimeout;

    public WhatsappMessageClaimService(
            WhatsappMessageRepository messages,
            Clock clock,
            @Value("${app.whatsapp.pending-timeout:PT15M}") Duration pendingTimeout) {
        this.messages = messages;
        this.clock = clock;
        this.pendingTimeout = pendingTimeout;
    }

    @Transactional
    public Optional<WhatsappMessage> claim(String deduplicationKey) {
        Instant claimedAt = clock.instant();
        Instant staleBefore = claimedAt.minus(pendingTimeout);
        if (messages.claimForDelivery(deduplicationKey, claimedAt, staleBefore) != 1) {
            return Optional.empty();
        }
        return messages.findByDeduplicationKey(deduplicationKey);
    }
}
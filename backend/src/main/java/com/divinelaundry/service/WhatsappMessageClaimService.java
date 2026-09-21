package com.divinelaundry.service;

import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    @Autowired
    public WhatsappMessageClaimService(
            WhatsappMessageRepository messages,
            Clock clock,
            @Value("${app.whatsapp.pending-timeout:PT15M}") String pendingTimeout) {
        this.messages = messages;
        this.clock = clock;
        this.pendingTimeout = parsePendingTimeout(pendingTimeout);
    }

    WhatsappMessageClaimService(WhatsappMessageRepository messages, Clock clock, Duration pendingTimeout) {
        this.messages = messages;
        this.clock = clock;
        this.pendingTimeout = parsePendingTimeout(pendingTimeout == null ? null : pendingTimeout.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<WhatsappMessage> claim(String deduplicationKey) {
        Instant claimedAt = clock.instant();
        Instant staleBefore = claimedAt.minus(pendingTimeout);
        if (messages.claimForDelivery(deduplicationKey, claimedAt, staleBefore) != 1) {
            return Optional.empty();
        }
        return messages.findByDeduplicationKey(deduplicationKey);
    }

    static Duration parsePendingTimeout(String value) {
        try {
            Duration parsed = Duration.parse(value == null ? "" : value.trim());
            if (parsed.isZero() || parsed.isNegative()) throw new IllegalArgumentException();
            return parsed;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("WHATSAPP_PENDING_TIMEOUT must be a positive ISO-8601 duration", error);
        }
    }
}
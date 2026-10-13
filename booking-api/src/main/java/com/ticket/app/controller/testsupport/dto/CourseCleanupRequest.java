package com.ticket.app.controller.testsupport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CourseCleanupRequest(
        @NotBlank
        @Pattern(regexp = "[a-z0-9]{8,32}")
        String runId,
        @NotNull List<@Valid @Positive Long> userIds,
        @NotNull List<@Valid @Positive Long> orderIds,
        @NotNull List<@Valid @Positive Long> ticketIds,
        @NotNull List<@Valid @Positive Long> couponIds
) {}

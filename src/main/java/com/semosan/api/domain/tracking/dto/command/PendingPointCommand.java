package com.semosan.api.domain.tracking.dto.command;

import java.time.LocalDateTime;

/** Consumer 메모리 버퍼에 누적되는 점 단위 — service/repository 계층이 공통으로 참조. */
public record PendingPointCommand(
        double lat,
        double lng,
        Double altitude,
        LocalDateTime recordedAt
) {
}

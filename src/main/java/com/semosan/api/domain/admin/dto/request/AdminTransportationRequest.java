package com.semosan.api.domain.admin.dto.request;

import com.semosan.api.domain.mountain.enums.TransportationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminTransportationRequest(
        @NotNull TransportationType type,
        @NotBlank @Size(max = 50) String direction,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}

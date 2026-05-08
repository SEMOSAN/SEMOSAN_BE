package com.semosan.api.domain.notification.controller.docs;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.domain.notification.dto.NotificationTestRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Notification Test", description = "알림 테스트 API (local 전용)")
public interface NotificationTestControllerDocs {

    @Operation(summary = "테스트 알림 발송", description = "지정한 유저에게 테스트 알림을 발송합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 발송 성공")
    })
    ResponseEntity<ApiResponse<Void>> send(
            @Valid @RequestBody NotificationTestRequest request
    );
}

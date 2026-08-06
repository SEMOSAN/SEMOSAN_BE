package com.semosan.api.domain.hiking.controller;

import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.response.PageResponse;
import com.semosan.api.common.status.SuccessStatus;
import com.semosan.api.domain.hiking.dto.request.CreateCourseDifficultyFeedbackRequest;
import com.semosan.api.domain.hiking.dto.response.CourseDifficultyFeedbackResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingMountainRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordResponse;
import com.semosan.api.domain.hiking.dto.response.GetUserHikingRecordSummaryResponse;
import com.semosan.api.domain.hiking.dto.response.HikingRecordDetailResponse;
import com.semosan.api.domain.hiking.enums.DifficultyFeedbackType;
import com.semosan.api.domain.hiking.service.HikingRecordService;
import com.semosan.api.domain.mountain.enums.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HikingRecordControllerTest {

    @Mock
    private HikingRecordService hikingRecordService;

    @InjectMocks
    private HikingRecordController hikingRecordController;

    @Test
    void getUserHikingRecordsReturnsPagedSuccessResponse() {
        PageRequest pageable = PageRequest.of(0, 10);
        GetUserHikingRecordResponse item = hikingRecordResponse();
        when(hikingRecordService.getUserHikingRecords(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        ResponseEntity<ApiResponse<PageResponse<GetUserHikingRecordResponse>>> response =
                hikingRecordController.getUserHikingRecords(1L, pageable);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.GET_HIKING_RECORD_LIST_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData().content()).containsExactly(item);
    }

    @Test
    void getMountainRecordsAndSummaryAndDetailReturnSuccessResponses() {
        PageRequest pageable = PageRequest.of(0, 10);
        GetUserHikingMountainRecordResponse mountainRecord =
                new GetUserHikingMountainRecordResponse(10L, "관악산", List.of("a.jpg", "b.jpg"), 2L,
                        LocalDate.of(2026, 8, 6));
        GetUserHikingRecordResponse record = hikingRecordResponse();
        GetUserHikingRecordSummaryResponse summary = new GetUserHikingRecordSummaryResponse(3L, 2L, 1500.0);
        HikingRecordDetailResponse detail = new HikingRecordDetailResponse(
                1L, null, null, 1000.0, 3600, 650.0, 100.0, 80.0,
                400, 18.0, LocalDateTime.now(), LocalDateTime.now(), null, null, List.of()
        );
        when(hikingRecordService.getUserHikingMountainRecords(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(mountainRecord), pageable, 1));
        when(hikingRecordService.getUserHikingRecordsByMountainId(1L, 10L, pageable))
                .thenReturn(new PageImpl<>(List.of(record), pageable, 1));
        when(hikingRecordService.getUserHikingRecordSummary(1L)).thenReturn(summary);
        when(hikingRecordService.getHikingRecordDetail(1L, 100L)).thenReturn(detail);

        assertThat(hikingRecordController.getUserHikingMountainRecords(1L, pageable).getBody().getData().content())
                .containsExactly(mountainRecord);
        assertThat(hikingRecordController.getUserHikingRecordsByMountainId(1L, 10L, pageable).getBody().getData().content())
                .containsExactly(record);
        assertThat(hikingRecordController.getUserHikingRecordSummary(1L).getBody().getData()).isSameAs(summary);
        assertThat(hikingRecordController.getHikingRecordDetail(1L, 100L).getBody().getData()).isSameAs(detail);
    }

    @Test
    void createCourseDifficultyFeedbackReturnsSuccessResponse() {
        CreateCourseDifficultyFeedbackRequest request =
                new CreateCourseDifficultyFeedbackRequest(DifficultyFeedbackType.SIMILAR);
        CourseDifficultyFeedbackResponse feedback = new CourseDifficultyFeedbackResponse(
                1L, 100L, 10L, "관악산", 20L, "정상 코스", Difficulty.NORMAL, DifficultyFeedbackType.SIMILAR
        );
        when(hikingRecordService.createCourseDifficultyFeedback(1L, 100L, request)).thenReturn(feedback);

        ResponseEntity<ApiResponse<CourseDifficultyFeedbackResponse>> response =
                hikingRecordController.createCourseDifficultyFeedback(1L, 100L, request);

        assertThat(response.getStatusCode())
                .isEqualTo(SuccessStatus.CREATE_COURSE_DIFFICULTY_FEEDBACK_SUCCESS.getHttpStatus());
        assertThat(response.getBody().getData()).isSameAs(feedback);
        verify(hikingRecordService).createCourseDifficultyFeedback(1L, 100L, request);
    }

    private GetUserHikingRecordResponse hikingRecordResponse() {
        return new GetUserHikingRecordResponse(
                100L, 200L, 10L, "관악산", 20L, "정상 코스",
                List.of("report.jpg", "clive.jpg"), 1500.0, 3600, LocalDate.of(2026, 8, 6)
        );
    }
}

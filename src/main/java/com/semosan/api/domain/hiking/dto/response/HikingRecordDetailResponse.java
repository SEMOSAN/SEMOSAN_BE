package com.semosan.api.domain.hiking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.semosan.api.domain.hiking.entity.HikingRecord;
import com.semosan.api.domain.tracking.entity.TrackingPhoto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 등산 기록 단건 상세 응답.
 *  - track / altitudes: PostGIS / jsonb 를 그대로 통과 (응답에 raw JSON 으로 직렬화). 점이 0~1개면 null.
 *  - course: 자유 기록인 경우 null.
 *  - photos: 트래킹 중 업로드된 마일스톤 사진들 (시간/마일스톤 순).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HikingRecordDetailResponse(
        Long hikingRecordId,
        MountainSummary mountain,
        CourseSummary course,
        Double distanceMeters,
        Integer durationSeconds,
        Double maxAltitudeMeters,
        Double ascentMeters,
        Double descentMeters,
        Integer calories,
        Double temperature,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        @JsonRawValue String track,
        @JsonRawValue String altitudes,
        List<PhotoMarker> photos
) {
    public record MountainSummary(Long id, String name) {
        public static MountainSummary of(Long id, String name) {
            return new MountainSummary(id, name);
        }
    }

    public record CourseSummary(Long id, String name, String startName, String endName) {
        public static CourseSummary of(Long id, String name, String startName, String endName) {
            return new CourseSummary(id, name, startName, endName);
        }
    }

    public record PhotoMarker(
            Integer milestoneIndex,
            Double lat,
            Double lng,
            String imageUrl,
            LocalDateTime capturedAt
    ) {
        public static PhotoMarker from(TrackingPhoto photo) {
            return new PhotoMarker(
                    photo.getMilestoneIndex(),
                    photo.getLat(),
                    photo.getLng(),
                    photo.getImageUrl(),
                    photo.getCapturedAt()
            );
        }
    }

    public static HikingRecordDetailResponse of(
            HikingRecord record,
            String track,
            String altitudes,
            List<TrackingPhoto> photos
    ) {
        return new HikingRecordDetailResponse(
                record.getId(),
                MountainSummary.of(record.getMountain().getId(), record.getMountain().getName()),
                record.getCourse() == null
                        ? null
                        : CourseSummary.of(
                                record.getCourse().getId(),
                                record.getCourse().getName(),
                                record.getCourse().getStartName(),
                                record.getCourse().getEndName()
                        ),
                record.getDistance(),
                record.getDuration(),
                record.getMaxAltitude(),
                record.getAscent(),
                record.getDescent(),
                record.getCalories(),
                record.getTemperature(),
                record.getStartedAt(),
                record.getEndedAt(),
                track,
                altitudes,
                photos.stream().map(PhotoMarker::from).toList()
        );
    }
}

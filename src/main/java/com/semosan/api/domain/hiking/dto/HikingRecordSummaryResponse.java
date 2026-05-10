package com.semosan.api.domain.hiking.dto;

import com.semosan.api.domain.hiking.entity.HikingRecord;

public record HikingRecordSummaryResponse(
        Long id,
        String mountainName,
        String courseName,
        Integer duration,
        Double altitude,
        Integer calories,
        String cliveImageUrl,
        String photoReportImageUrl
) {
    public static HikingRecordSummaryResponse from(HikingRecord record) {
        return new HikingRecordSummaryResponse(
                record.getId(),
                record.getMountain().getName(),
                record.getCourse().getName(),
                record.getDuration(),
                record.getAltitude(),
                record.getCalories(),
                record.getCliveImageUrl(),
                record.getPhotoReportImageUrl()
        );
    }
}

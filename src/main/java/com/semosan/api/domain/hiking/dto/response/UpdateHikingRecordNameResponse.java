package com.semosan.api.domain.hiking.dto.response;

import com.semosan.api.domain.hiking.entity.HikingRecord;

public record UpdateHikingRecordNameResponse(
        Long hikingRecordId,
        String recordName
) {

    public static UpdateHikingRecordNameResponse from(HikingRecord record) {
        return new UpdateHikingRecordNameResponse(record.getId(), record.getName());
    }
}

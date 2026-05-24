package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 트래킹 세션의 사진 촬영 마일스톤 거리 리스트를 계산한다.
 *  - 코스 따라가기: course.distance(km) → m 변환 후 1/4, 2/4, 3/4, 4/4 (총 4컷)
 *  - 자유 기록: 500m 간격, 정책상 최대 6컷 (500/1000/1500/2000/2500/3000m)
 *
 * 모든 반환 단위는 미터(m). distanceTotal(Haversine) 과 단위 일치.
 */
@Component
public class TrackingMilestoneCalculator {

    private static final int COURSE_MILESTONE_COUNT = 4;
    private static final double FREE_RECORDING_INTERVAL_METERS = 500.0;
    private static final int FREE_RECORDING_MAX_COUNT = 6;
    private static final double KM_TO_M = 1000.0;

    public List<Double> calculate(TrackingSession session) {
        if (Boolean.TRUE.equals(session.getIsFreeRecording()) || session.getCourse() == null) {
            return freeRecordingMilestones();
        }
        return courseMilestones(session.getCourse());
    }

    private List<Double> courseMilestones(Course course) {
        double totalMeters = (course.getDistance() == null ? 0.0 : course.getDistance()) * KM_TO_M;
        List<Double> result = new ArrayList<>(COURSE_MILESTONE_COUNT);
        for (int i = 1; i <= COURSE_MILESTONE_COUNT; i++) {
            result.add(totalMeters * i / COURSE_MILESTONE_COUNT);
        }
        return result;
    }

    private List<Double> freeRecordingMilestones() {
        List<Double> result = new ArrayList<>(FREE_RECORDING_MAX_COUNT);
        for (int i = 1; i <= FREE_RECORDING_MAX_COUNT; i++) {
            result.add(FREE_RECORDING_INTERVAL_METERS * i);
        }
        return result;
    }
}

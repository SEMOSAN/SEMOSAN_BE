package com.semosan.api.domain.tracking.service;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.tracking.entity.TrackingSession;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 트래킹 세션의 사진 촬영 마일스톤 거리 리스트를 계산한다.
 *  - 코스 따라가기: course.distance(미터) 의 1/4, 2/4, 3/4, 4/4 지점 (총 4컷)
 *  - 자유 기록: 500m 간격, 정책상 최대 6컷 (500/1000/1500/2000/2500/3000m)
 *
 * 단위 정책: 입력/출력 모두 미터(m). distanceTotal(Haversine 누적, m) 과 비교될 값이라 단위 일치 필수.
 * 과거: course.distance 를 km 로 가정하고 × 1000 했으나, DB/시드/응답이 모두 m 단위라 마일스톤이 1000배로 박혀 OPEN 이 영영 안 오는 버그가 있었음.
 */
@Component
public class TrackingMilestoneCalculator {

    private static final int COURSE_MILESTONE_COUNT = 4;
    private static final double FREE_RECORDING_INTERVAL_METERS = 500.0;
    private static final int FREE_RECORDING_MAX_COUNT = 6;

    public List<Double> calculate(TrackingSession session) {
        if (Boolean.TRUE.equals(session.getIsFreeRecording()) || session.getCourse() == null) {
            return freeRecordingMilestones();
        }
        return courseMilestones(session.getCourse());
    }

    private List<Double> courseMilestones(Course course) {
        double totalMeters = course.getDistance() == null ? 0.0 : course.getDistance();
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

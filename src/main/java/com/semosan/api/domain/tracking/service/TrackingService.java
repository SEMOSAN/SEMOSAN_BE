package com.semosan.api.domain.tracking.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.tracking.dto.response.LiveActivityCourseResponse;
import com.semosan.api.domain.tracking.dto.response.NearbyMountainResponse;
import com.semosan.api.domain.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingService {

    private final MountainRepository mountainRepository;
    private final CourseRepository courseRepository;
    private final UserReader userReader;

    /**
     * 사용자의 현재 좌표 기준 가장 가까운 산 1개와 그 산의 코스 목록을 반환한다.
     * 거리 임계값 없이 항상 가장 가까운 산을 반환한다.
     * 산 데이터가 비어있거나 location 이 null 인 산만 존재할 경우 MOUNTAIN_NOT_FOUND 로 응답.
     */
    public NearbyMountainResponse getNearbyMountain(Long userId, Double lat, Double lng) {
        userReader.findActiveUserById(userId);
        Mountain mountain = mountainRepository.findNearestByLatLng(lat, lng)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND));
        return NearbyMountainResponse.of(
                mountain,
                courseRepository.findByMountainId(mountain.getId())
        );
    }

    public LiveActivityCourseResponse getLiveActivityCourse(Long userId, Long courseId) {
        userReader.findActiveUserById(userId);
        return courseRepository.findById(courseId)
                .map(LiveActivityCourseResponse::from)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
    }
}

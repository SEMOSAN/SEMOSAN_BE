package com.semosan.api.domain.tracking.dto.response;

import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.enums.Difficulty;

import java.util.List;

public record NearbyMountainResponse(
        MountainInfo mountain,
        List<CourseInfo> courses
) {

    public static NearbyMountainResponse of(Mountain mountain, List<Course> courses) {
        return new NearbyMountainResponse(
                MountainInfo.from(mountain),
                courses.stream().map(CourseInfo::from).toList()
        );
    }

    public record MountainInfo(
            Long mountainId,
            String name,
            String address,
            Double altitude,
            Double latitude,
            Double longitude,
            List<String> imageUrls
    ) {
        public static MountainInfo from(Mountain mountain) {
            return new MountainInfo(
                    mountain.getId(),
                    mountain.getName(),
                    mountain.getAddress(),
                    mountain.getAltitude(),
                    mountain.getLatitude(),
                    mountain.getLongitude(),
                    mountain.getImageUrls()
            );
        }
    }

    /**
     * 시안의 코스 카드에 표시되는 정보 중 현재 도메인 모델로 채울 수 있는 것만 포함.
     * TODO: ascent / descent / maxAltitude 컬럼이 Course 에 추가되면 함께 노출.
     */
    public record CourseInfo(
            Long courseId,
            String name,
            Difficulty difficulty,
            Double distance,
            Integer duration
    ) {
        public static CourseInfo from(Course course) {
            return new CourseInfo(
                    course.getId(),
                    course.getName(),
                    course.getDifficulty(),
                    course.getDistance(),
                    course.getDuration()
            );
        }
    }
}

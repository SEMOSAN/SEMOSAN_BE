package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.common.util.GeoDistance;
import com.semosan.api.domain.admin.dto.request.AdminCourseCreateRequest;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자가 지도에서 찍은 좌표로 코스를 만들고 지운다.
 *
 * 코스/산은 원래 시드 마이그레이션으로만 들어가는 데이터였다. 테스트 코스를 넣으려고
 * 운영 DB 에 직접 SQL 을 쓰면 이력이 남지 않아 나중에 복구가 안 되므로(V6 사례),
 * 어드민에서 만들 수 있게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCourseService {

    /**
     * geography(LineString, 4326) 컬럼에 맞춰야 한다. SRID 를 빠뜨리면 0 으로 저장돼
     * 이후 PostGIS 공간 쿼리와 거리 계산이 전부 어긋난다.
     */
    private static final int SRID_WGS84 = 4326;

    private final CourseRepository courseRepository;
    private final MountainRepository mountainRepository;

    @Transactional
    public Long create(AdminCourseCreateRequest request) {
        Mountain mountain = mountainRepository.findById(request.mountainId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND));

        List<AdminCourseCreateRequest.PointRequest> points = request.points();
        validateSummitIndex(request.summitPointIndex(), points.size());

        LineString polyline = toLineString(points);
        double distanceMeters = GeoDistance.pathLengthMeters(polyline.getCoordinates());

        Course course = courseRepository.save(Course.create(
                mountain,
                request.name().trim(),
                request.difficulty(),
                distanceMeters,
                request.duration(),
                polyline,
                trimToNull(request.startName()),
                trimToNull(request.endName())
        ));

        // 고도가 없으므로 summitEle 는 null 이다. 정상 좌표만 있으면
        // CourseSummitDistanceCalculator 가 폴리라인 최근접 점으로 스냅해 거리를 구한다.
        Integer summitIndex = request.summitPointIndex();
        if (summitIndex != null) {
            AdminCourseCreateRequest.PointRequest summit = points.get(summitIndex);
            course.updateSummit(summit.lat(), summit.lng(), null);
        }

        return course.getId();
    }

    /**
     * courses 를 참조하는 FK 5개(reviews, hiking_records, tracking_sessions,
     * course_difficulty_feedbacks, course_likes)가 모두 ON DELETE 절 없이 선언돼 있어
     * 참조가 있으면 DB 가 삭제를 막는다. 사용자 기록을 함께 지우는 강제 삭제는 만들지 않고
     * 409 로 명확히 거부한다.
     */
    @Transactional
    public void delete(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
        try {
            courseRepository.delete(course);
            courseRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ErrorStatus.ADMIN_COURSE_IN_USE);
        }
    }

    private static void validateSummitIndex(Integer summitIndex, int pointCount) {
        if (summitIndex == null) {
            return;
        }
        if (summitIndex < 0 || summitIndex >= pointCount) {
            throw new GeneralException(ErrorStatus.ADMIN_COURSE_SUMMIT_INDEX_INVALID);
        }
    }

    /** JTS Coordinate 는 x=경도, y=위도 순서다. 시드의 WKT(LINESTRING(lng lat, ...)) 와 같은 순서. */
    private static LineString toLineString(List<AdminCourseCreateRequest.PointRequest> points) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
        Coordinate[] coordinates = points.stream()
                .map(p -> new Coordinate(p.lng(), p.lat()))
                .toArray(Coordinate[]::new);
        return geometryFactory.createLineString(coordinates);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

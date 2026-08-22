package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminCourseSummitRequest;
import com.semosan.api.domain.admin.dto.request.AdminMountainUpdateRequest;
import com.semosan.api.domain.admin.dto.request.AdminMountainVisibilityRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantSectionRequest;
import com.semosan.api.domain.admin.dto.request.AdminTransportationRequest;
import com.semosan.api.domain.admin.dto.response.AdminCourseWaypointsResponse;
import com.semosan.api.domain.admin.dto.response.AdminMountainListResponse;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.entity.Restaurant;
import com.semosan.api.domain.mountain.entity.RestaurantSection;
import com.semosan.api.domain.mountain.entity.Transportation;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.mountain.repository.RestaurantRepository;
import com.semosan.api.domain.mountain.repository.RestaurantSectionRepository;
import com.semosan.api.domain.mountain.repository.AmenityRepository;
import com.semosan.api.domain.mountain.repository.TransportationRepository;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.*;
import com.semosan.api.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMountainService {

    private final MountainRepository mountainRepository;
    private final CourseRepository courseRepository;
    private final RestaurantSectionRepository restaurantSectionRepository;
    private final RestaurantRepository restaurantRepository;
    private final TransportationRepository transportationRepository;
    private final AmenityRepository amenityRepository;
    private final ReviewService reviewService;

    @Transactional(readOnly = true)
    public Page<AdminMountainListResponse> getMountains(String keyword, String visibility, Pageable pageable) {
        Page<Mountain> mountains;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String trimmedKeyword = hasKeyword ? keyword.trim() : null;

        if ("PUBLIC".equals(visibility)) {
            mountains = hasKeyword
                    ? mountainRepository.searchByKeywordAndVisibility(trimmedKeyword, true, pageable)
                    : mountainRepository.findByIsPublicTrue(pageable);
        } else if ("PRIVATE".equals(visibility)) {
            mountains = hasKeyword
                    ? mountainRepository.searchByKeywordAndVisibility(trimmedKeyword, false, pageable)
                    : mountainRepository.findByIsPublicFalse(pageable);
        } else {
            mountains = hasKeyword
                    ? mountainRepository.searchByKeywordAll(trimmedKeyword, pageable)
                    : mountainRepository.findAll(pageable);
        }

        Map<Long, Long> courseCountMap = getCourseCountMap(mountains.getContent());

        return mountains.map(mountain -> AdminMountainListResponse.of(
                mountain,
                courseCountMap.getOrDefault(mountain.getId(), 0L)
        ));
    }

    private Map<Long, Long> getCourseCountMap(List<Mountain> mountains) {
        if (mountains.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> mountainIds = mountains.stream()
                .map(Mountain::getId)
                .toList();
        return courseRepository.countByMountainIds(mountainIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional(readOnly = true)
    public MountainDetailResponse getMountainDetail(Long mountainId) {
        Mountain mountain = findMountainById(mountainId);
        return new MountainDetailResponse(
                MountainInfo.from(mountain),
                courseRepository.findByMountainId(mountainId).stream().map(CourseInfo::from).toList(),
                TransportationGroup.from(transportationRepository.findByMountainId(mountainId)),
                MountainDetailResponse.groupAmenities(amenityRepository.findByMountainId(mountainId)),
                restaurantSectionRepository.findByMountainIdWithRestaurants(mountainId).stream().map(RestaurantSectionInfo::from).toList(),
                reviewService.getReviewsByMountainId(mountainId).stream().map(ReviewInfo::from).toList()
        );
    }

    @Transactional
    public void updateMountain(Long mountainId, AdminMountainUpdateRequest request) {
        Mountain mountain = findMountainById(mountainId);
        mountain.updateInfo(request.name(), request.address(), request.altitude(),
                request.difficulty(), request.duration());
        mountain.updateImageUrls(request.imageUrls());
    }

    @Transactional(readOnly = true)
    public List<AdminCourseWaypointsResponse> getWaypoints(Long mountainId) {
        findMountainById(mountainId);
        return courseRepository.findByMountainId(mountainId).stream()
                .map(AdminCourseWaypointsResponse::from)
                .toList();
    }

    @Transactional
    public void updateSummit(Long courseId, AdminCourseSummitRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.COURSE_NOT_FOUND));
        course.updateSummit(request.latitude(), request.longitude(), request.altitude());
    }

    @Transactional
    public void updateVisibility(Long mountainId, AdminMountainVisibilityRequest request) {
        Mountain mountain = findMountainById(mountainId);
        mountain.updateVisibility(request.isPublic());
    }

    @Transactional
    public Long createRestaurantSection(Long mountainId, AdminRestaurantSectionRequest request) {
        Mountain mountain = findMountainById(mountainId);
        RestaurantSection section = RestaurantSection.create(mountain, request.title());
        return restaurantSectionRepository.save(section).getId();
    }

    @Transactional
    public void updateRestaurantSection(Long sectionId, AdminRestaurantSectionRequest request) {
        RestaurantSection section = restaurantSectionRepository.findById(sectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.RESTAURANT_SECTION_NOT_FOUND));
        section.updateTitle(request.title());
    }

    @Transactional
    public void deleteRestaurantSection(Long sectionId) {
        RestaurantSection section = restaurantSectionRepository.findById(sectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.RESTAURANT_SECTION_NOT_FOUND));
        restaurantSectionRepository.delete(section);
    }

    @Transactional
    public Long createRestaurant(Long sectionId, AdminRestaurantRequest request) {
        RestaurantSection section = restaurantSectionRepository.findById(sectionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.RESTAURANT_SECTION_NOT_FOUND));
        Restaurant restaurant = Restaurant.create(section, request.name(), request.category(),
                request.menu(), request.description(), request.imageUrl(),
                request.mapUrl(), request.blogUrl());
        return restaurantRepository.save(restaurant).getId();
    }

    @Transactional
    public void updateRestaurant(Long restaurantId, AdminRestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.RESTAURANT_NOT_FOUND));
        restaurant.update(request.name(), request.category(), request.menu(),
                request.description(), request.imageUrl(), request.mapUrl(), request.blogUrl());
    }

    @Transactional
    public void deleteRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.RESTAURANT_NOT_FOUND));
        restaurantRepository.delete(restaurant);
    }

    @Transactional
    public Long createTransportation(Long mountainId, AdminTransportationRequest request) {
        Mountain mountain = findMountainById(mountainId);
        Transportation transportation = Transportation.create(mountain, request.type(),
                request.direction(), request.name(), request.description());
        return transportationRepository.save(transportation).getId();
    }

    @Transactional
    public void updateTransportation(Long transportationId, AdminTransportationRequest request) {
        Transportation transportation = transportationRepository.findById(transportationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TRANSPORTATION_NOT_FOUND));
        transportation.update(request.type(), request.direction(), request.name(), request.description());
    }

    @Transactional
    public void deleteTransportation(Long transportationId) {
        Transportation transportation = transportationRepository.findById(transportationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TRANSPORTATION_NOT_FOUND));
        transportationRepository.delete(transportation);
    }

    private Mountain findMountainById(Long mountainId) {
        return mountainRepository.findById(mountainId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND));
    }
}

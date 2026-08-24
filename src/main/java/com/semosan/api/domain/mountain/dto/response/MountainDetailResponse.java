package com.semosan.api.domain.mountain.dto.response;

import com.semosan.api.domain.mountain.entity.*;
import com.semosan.api.domain.mountain.enums.AmenityType;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.enums.TransportationType;
import com.semosan.api.domain.review.entity.Review;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record MountainDetailResponse(
        MountainInfo mountain,
        List<CourseInfo> courses,
        TransportationGroup transportations,
        Map<String, List<AmenityType>> amenities,
        List<RestaurantSectionInfo> restaurantSections,
        List<ReviewInfo> reviews
) {

    public record MountainInfo(
            Long mountainId,
            String name,
            String address,
            Double altitude,
            Difficulty difficulty,
            Integer duration,
            List<String> imageUrls,
            Double latitude,
            Double longitude
    ) {
        public static MountainInfo from(Mountain mountain) {
            return new MountainInfo(
                    mountain.getId(),
                    mountain.getName(),
                    mountain.getAddress(),
                    mountain.getAltitude(),
                    mountain.getDifficulty(),
                    mountain.getDuration(),
                    mountain.getImageUrls(),
                    mountain.getLatitude(),
                    mountain.getLongitude()
            );
        }
    }

    @Schema(name = "MountainDetailCourseInfo")
    public record CourseInfo(
            Long courseId,
            String name,
            Difficulty difficulty,
            Double distance,
            Integer duration,
            String startName,
            String endName
    ) {
        public static CourseInfo from(Course course) {
            return new CourseInfo(
                    course.getId(),
                    course.getName(),
                    course.getDifficulty(),
                    course.getDistance(),
                    course.getDuration(),
                    course.getStartName(),
                    course.getEndName()
            );
        }
    }

    public record TransportationGroup(
            Map<String, List<TransportationItem>> publicTransport,
            Map<String, List<TransportationItem>> parking
    ) {
        public static TransportationGroup from(List<Transportation> transportations) {
            Map<String, List<TransportationItem>> publicTransport = transportations.stream()
                    .filter(t -> t.getType() == TransportationType.SUBWAY || t.getType() == TransportationType.BUS)
                    .collect(Collectors.groupingBy(
                            Transportation::getDirection,
                            Collectors.mapping(TransportationItem::from, Collectors.toList())
                    ));
            Map<String, List<TransportationItem>> parking = transportations.stream()
                    .filter(t -> t.getType() == TransportationType.PARKING)
                    .collect(Collectors.groupingBy(
                            Transportation::getDirection,
                            Collectors.mapping(TransportationItem::from, Collectors.toList())
                    ));
            return new TransportationGroup(publicTransport, parking);
        }
    }

    public record TransportationItem(
            Long transportationId,
            TransportationType type,
            String name,
            String description
    ) {
        public static TransportationItem from(Transportation transportation) {
            return new TransportationItem(
                    transportation.getId(),
                    transportation.getType(),
                    transportation.getName(),
                    transportation.getDescription()
            );
        }
    }

    public static Map<String, List<AmenityType>> groupAmenities(List<Amenity> amenities) {
        return amenities.stream()
                .collect(Collectors.groupingBy(
                        Amenity::getDirection,
                        Collectors.mapping(Amenity::getType, Collectors.toList())
                ));
    }

    public record RestaurantSectionInfo(
            Long sectionId,
            String title,
            List<RestaurantInfo> restaurants
    ) {
        public static RestaurantSectionInfo from(RestaurantSection section) {
            return new RestaurantSectionInfo(
                    section.getId(),
                    section.getTitle(),
                    section.getRestaurants().stream()
                            .map(RestaurantInfo::from)
                            .toList()
            );
        }
    }

    public record RestaurantInfo(
            Long restaurantId,
            String name,
            String category,
            String imageUrl,
            String mapUrl
    ) {
        public static RestaurantInfo from(Restaurant restaurant) {
            return new RestaurantInfo(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getCategory(),
                    restaurant.getImageUrl(),
                    restaurant.getMapUrl()
            );
        }
    }

    public record ReviewInfo(
            Long reviewId,
            String imageUrl,
            String authorName,
            String content,
            Difficulty difficulty,
            String courseName
    ) {
        public static ReviewInfo from(Review review) {
            return new ReviewInfo(
                    review.getId(),
                    review.getImageUrl(),
                    review.getUser().getName(),
                    review.getContent(),
                    review.getDifficulty(),
                    review.getCourse() != null ? review.getCourse().getName() : null
            );
        }
    }
}

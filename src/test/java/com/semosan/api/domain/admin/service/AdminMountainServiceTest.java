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
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.entity.Course;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.entity.Restaurant;
import com.semosan.api.domain.mountain.entity.RestaurantSection;
import com.semosan.api.domain.mountain.entity.Transportation;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.enums.TransportationType;
import com.semosan.api.domain.mountain.repository.AmenityRepository;
import com.semosan.api.domain.mountain.repository.CourseRepository;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.mountain.repository.RestaurantRepository;
import com.semosan.api.domain.mountain.repository.RestaurantSectionRepository;
import com.semosan.api.domain.mountain.repository.TransportationRepository;
import com.semosan.api.domain.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMountainServiceTest {

    @Mock
    private MountainRepository mountainRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RestaurantSectionRepository restaurantSectionRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private TransportationRepository transportationRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private AdminMountainService adminMountainService;

    @Test
    void getMountainsUsesPublicSearchWhenKeywordAndPublicVisibilityProvided() {
        Mountain mountain = mountain(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainRepository.searchByKeywordAndVisibility("관악", true, pageable))
                .thenReturn(new PageImpl<>(List.of(mountain), pageable, 1));
        when(courseRepository.countByMountainIds(List.of(1L)))
                .thenReturn(Collections.singletonList(new Object[]{1L, 4L}));

        Page<AdminMountainListResponse> result = adminMountainService.getMountains("  관악  ", "PUBLIC", pageable);

        AdminMountainListResponse response = result.getContent().getFirst();
        assertThat(response.mountainId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("관악산");
        assertThat(response.courseCount()).isEqualTo(4L);
    }

    @Test
    void getMountainsUsesPrivateListWhenPrivateVisibilityWithoutKeyword() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainRepository.findByIsPublicFalse(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AdminMountainListResponse> result = adminMountainService.getMountains(null, "PRIVATE", pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void getMountainsUsesPublicListWhenPublicVisibilityWithoutKeyword() {
        Mountain mountain = mountain(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainRepository.findByIsPublicTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(mountain), pageable, 1));
        when(courseRepository.countByMountainIds(List.of(1L)))
                .thenReturn(Collections.singletonList(new Object[]{1L, 2L}));

        Page<AdminMountainListResponse> result = adminMountainService.getMountains(null, "PUBLIC", pageable);

        assertThat(result.getContent().getFirst().courseCount()).isEqualTo(2L);
    }

    @Test
    void getMountainsUsesPrivateSearchWhenKeywordAndPrivateVisibilityProvided() {
        Mountain mountain = mountain(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainRepository.searchByKeywordAndVisibility("관악", false, pageable))
                .thenReturn(new PageImpl<>(List.of(mountain), pageable, 1));
        when(courseRepository.countByMountainIds(List.of(1L)))
                .thenReturn(Collections.singletonList(new Object[]{1L, 1L}));

        Page<AdminMountainListResponse> result = adminMountainService.getMountains(" 관악 ", "PRIVATE", pageable);

        assertThat(result.getContent().getFirst().courseCount()).isEqualTo(1L);
    }

    @Test
    void getMountainsUsesFindAllWhenKeywordAndVisibilityAreMissing() {
        Mountain mountain = mountain(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(mountain), pageable, 1));
        when(courseRepository.countByMountainIds(List.of(1L))).thenReturn(List.of());

        Page<AdminMountainListResponse> result = adminMountainService.getMountains(" ", null, pageable);

        assertThat(result.getContent().getFirst().courseCount()).isZero();
    }

    @Test
    void getMountainsUsesAllSearchWhenVisibilityIsUnknown() {
        Mountain mountain = mountain(2L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(mountainRepository.searchByKeywordAll("북한", pageable))
                .thenReturn(new PageImpl<>(List.of(mountain), pageable, 1));
        when(courseRepository.countByMountainIds(List.of(2L)))
                .thenReturn(Collections.singletonList(new Object[]{2L, 0L}));

        Page<AdminMountainListResponse> result = adminMountainService.getMountains("북한", "ALL", pageable);

        assertThat(result.getContent().getFirst().courseCount()).isZero();
    }

    @Test
    void updateMountainChangesInfoAndImages() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        AdminMountainUpdateRequest request = new AdminMountainUpdateRequest(
                "북한산", "서울 강북구", 836.5, Difficulty.HARD, 240, List.of("a.jpg", "b.jpg"));

        adminMountainService.updateMountain(1L, request);

        assertThat(mountain.getName()).isEqualTo("북한산");
        assertThat(mountain.getAddress()).isEqualTo("서울 강북구");
        assertThat(mountain.getAltitude()).isEqualTo(836.5);
        assertThat(mountain.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(mountain.getDuration()).isEqualTo(240);
        assertThat(mountain.getImageUrls()).containsExactly("a.jpg", "b.jpg");
    }

    @Test
    void getWaypointsReturnsWaypointsGroupedByCourse() {
        Mountain mountain = mountain(1L);
        Course course = course(5L, "정상 코스",
                List.of(new Course.CourseWaypoint(37.5, 127.0, 632.2, "연주대", "PEAK")));
        course.updateSummit(37.5, 127.0, 632.2);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.findByMountainId(1L)).thenReturn(List.of(course));

        List<AdminCourseWaypointsResponse> result = adminMountainService.getWaypoints(1L);

        assertThat(result).hasSize(1);
        AdminCourseWaypointsResponse response = result.getFirst();
        assertThat(response.courseId()).isEqualTo(5L);
        assertThat(response.courseName()).isEqualTo("정상 코스");
        assertThat(response.summitLat()).isEqualTo(37.5);
        assertThat(response.summitLng()).isEqualTo(127.0);
        assertThat(response.summitEle()).isEqualTo(632.2);
        assertThat(response.waypoints()).containsExactly(
                new AdminCourseWaypointsResponse.WaypointInfo(37.5, 127.0, 632.2, "연주대", "PEAK"));
    }

    @Test
    void getWaypointsReturnsEmptyListWhenCourseWaypointsAreNull() {
        Mountain mountain = mountain(1L);
        Course course = course(5L, "정상 코스", null);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.findByMountainId(1L)).thenReturn(List.of(course));

        List<AdminCourseWaypointsResponse> result = adminMountainService.getWaypoints(1L);

        assertThat(result.getFirst().waypoints()).isEmpty();
    }

    @Test
    void updateSummitUpdatesCourseSummitCoordinates() {
        Course course = course(5L, "정상 코스", null);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));

        adminMountainService.updateSummit(5L, new AdminCourseSummitRequest(37.5, 127.0, 836.5));

        assertThat(course.getSummitLat()).isEqualTo(37.5);
        assertThat(course.getSummitLng()).isEqualTo(127.0);
        assertThat(course.getSummitEle()).isEqualTo(836.5);
    }

    @Test
    void updateSummitOverwritesSummitEleWithNullWhenAltitudeMissing() {
        Course course = course(5L, "정상 코스", null);
        course.updateSummit(37.0, 126.0, 632.2);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));

        adminMountainService.updateSummit(5L, new AdminCourseSummitRequest(37.5, 127.0, null));

        assertThat(course.getSummitLat()).isEqualTo(37.5);
        assertThat(course.getSummitLng()).isEqualTo(127.0);
        assertThat(course.getSummitEle()).isNull();
    }

    @Test
    void updateVisibilityChangesPublicFlag() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));

        adminMountainService.updateVisibility(1L, new AdminMountainVisibilityRequest(false));

        assertThat(mountain.isPublic()).isFalse();
    }

    @Test
    void getMountainDetailReturnsEmptyDetailSectionsWhenRelatedDataIsMissing() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(courseRepository.findByMountainId(1L)).thenReturn(List.of());
        when(transportationRepository.findByMountainId(1L)).thenReturn(List.of());
        when(amenityRepository.findByMountainId(1L)).thenReturn(List.of());
        when(restaurantSectionRepository.findByMountainIdWithRestaurants(1L)).thenReturn(List.of());
        when(reviewService.getReviewsByMountainId(1L)).thenReturn(List.of());

        MountainDetailResponse response = adminMountainService.getMountainDetail(1L);

        assertThat(response.mountain().mountainId()).isEqualTo(1L);
        assertThat(response.courses()).isEmpty();
        assertThat(response.transportations().publicTransport()).isEmpty();
        assertThat(response.transportations().parking()).isEmpty();
        assertThat(response.amenities()).isEmpty();
        assertThat(response.restaurantSections()).isEmpty();
        assertThat(response.reviews()).isEmpty();
    }

    @Test
    void createRestaurantSectionSavesSectionForMountain() {
        Mountain mountain = mountain(1L);
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(restaurantSectionRepository.save(any(RestaurantSection.class))).thenAnswer(invocation -> {
            RestaurantSection section = invocation.getArgument(0);
            ReflectionTestUtils.setField(section, "id", 10L);
            return section;
        });

        Long sectionId = adminMountainService.createRestaurantSection(1L, new AdminRestaurantSectionRequest("맛집"));

        assertThat(sectionId).isEqualTo(10L);
        ArgumentCaptor<RestaurantSection> captor = ArgumentCaptor.forClass(RestaurantSection.class);
        verify(restaurantSectionRepository).save(captor.capture());
        assertThat(captor.getValue().getMountain()).isSameAs(mountain);
        assertThat(captor.getValue().getTitle()).isEqualTo("맛집");
    }

    @Test
    void updateRestaurantSectionChangesTitle() {
        RestaurantSection section = RestaurantSection.create(mountain(1L), "기존");
        when(restaurantSectionRepository.findById(10L)).thenReturn(Optional.of(section));

        adminMountainService.updateRestaurantSection(10L, new AdminRestaurantSectionRequest("변경"));

        assertThat(section.getTitle()).isEqualTo("변경");
    }

    @Test
    void deleteRestaurantSectionDeletesFoundSection() {
        RestaurantSection section = RestaurantSection.create(mountain(1L), "맛집");
        when(restaurantSectionRepository.findById(10L)).thenReturn(Optional.of(section));

        adminMountainService.deleteRestaurantSection(10L);

        verify(restaurantSectionRepository).delete(section);
    }

    @Test
    void createRestaurantSavesRestaurantForSection() {
        RestaurantSection section = RestaurantSection.create(mountain(1L), "맛집");
        AdminRestaurantRequest request = restaurantRequest("식당");
        when(restaurantSectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant restaurant = invocation.getArgument(0);
            ReflectionTestUtils.setField(restaurant, "id", 20L);
            return restaurant;
        });

        Long restaurantId = adminMountainService.createRestaurant(10L, request);

        assertThat(restaurantId).isEqualTo(20L);
        ArgumentCaptor<Restaurant> captor = ArgumentCaptor.forClass(Restaurant.class);
        verify(restaurantRepository).save(captor.capture());
        assertThat(captor.getValue().getSection()).isSameAs(section);
        assertThat(captor.getValue().getName()).isEqualTo("식당");
    }

    @Test
    void updateRestaurantChangesFields() {
        Restaurant restaurant = Restaurant.create(RestaurantSection.create(mountain(1L), "맛집"),
                "기존", "한식", "메뉴", "설명", "image", "map", "blog");
        when(restaurantRepository.findById(20L)).thenReturn(Optional.of(restaurant));

        adminMountainService.updateRestaurant(20L, restaurantRequest("변경"));

        assertThat(restaurant.getName()).isEqualTo("변경");
        assertThat(restaurant.getCategory()).isEqualTo("양식");
        assertThat(restaurant.getMenu()).isEqualTo("파스타");
        assertThat(restaurant.getDescription()).isEqualTo("설명 변경");
        assertThat(restaurant.getImageUrl()).isEqualTo("new-image");
        assertThat(restaurant.getMapUrl()).isEqualTo("new-map");
        assertThat(restaurant.getBlogUrl()).isEqualTo("new-blog");
    }

    @Test
    void deleteRestaurantDeletesFoundRestaurant() {
        Restaurant restaurant = Restaurant.create(RestaurantSection.create(mountain(1L), "맛집"),
                "식당", "한식", "메뉴", "설명", "image", "map", "blog");
        when(restaurantRepository.findById(20L)).thenReturn(Optional.of(restaurant));

        adminMountainService.deleteRestaurant(20L);

        verify(restaurantRepository).delete(restaurant);
    }

    @Test
    void createTransportationSavesTransportationForMountain() {
        Mountain mountain = mountain(1L);
        AdminTransportationRequest request = transportationRequest("정류장");
        when(mountainRepository.findById(1L)).thenReturn(Optional.of(mountain));
        when(transportationRepository.save(any(Transportation.class))).thenAnswer(invocation -> {
            Transportation transportation = invocation.getArgument(0);
            ReflectionTestUtils.setField(transportation, "id", 30L);
            return transportation;
        });

        Long transportationId = adminMountainService.createTransportation(1L, request);

        assertThat(transportationId).isEqualTo(30L);
        ArgumentCaptor<Transportation> captor = ArgumentCaptor.forClass(Transportation.class);
        verify(transportationRepository).save(captor.capture());
        assertThat(captor.getValue().getMountain()).isSameAs(mountain);
        assertThat(captor.getValue().getName()).isEqualTo("정류장");
    }

    @Test
    void updateTransportationChangesFields() {
        Transportation transportation = Transportation.create(mountain(1L),
                TransportationType.BUS, "상행", "기존", "설명");
        when(transportationRepository.findById(30L)).thenReturn(Optional.of(transportation));

        adminMountainService.updateTransportation(30L, transportationRequest("변경"));

        assertThat(transportation.getType()).isEqualTo(TransportationType.SUBWAY);
        assertThat(transportation.getDirection()).isEqualTo("하행");
        assertThat(transportation.getName()).isEqualTo("변경");
        assertThat(transportation.getDescription()).isEqualTo("설명 변경");
    }

    @Test
    void deleteTransportationDeletesFoundTransportation() {
        Transportation transportation = Transportation.create(mountain(1L),
                TransportationType.BUS, "상행", "정류장", "설명");
        when(transportationRepository.findById(30L)).thenReturn(Optional.of(transportation));

        adminMountainService.deleteTransportation(30L);

        verify(transportationRepository).delete(transportation);
    }

    @Test
    void updateMountainThrowsWhenMountainMissing() {
        when(mountainRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.updateMountain(1L,
                new AdminMountainUpdateRequest("산", "주소", 100.0, Difficulty.EASY, 60, List.of())))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void getWaypointsThrowsWhenMountainMissing() {
        when(mountainRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.getWaypoints(1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void updateSummitThrowsWhenCourseMissing() {
        when(courseRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.updateSummit(5L,
                new AdminCourseSummitRequest(37.5, 127.0, null)))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.COURSE_NOT_FOUND);
    }

    @Test
    void getMountainDetailThrowsWhenMountainMissing() {
        when(mountainRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.getMountainDetail(1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void updateRestaurantSectionThrowsWhenSectionMissing() {
        when(restaurantSectionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.updateRestaurantSection(10L,
                new AdminRestaurantSectionRequest("맛집")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.RESTAURANT_SECTION_NOT_FOUND);
    }

    @Test
    void deleteRestaurantSectionThrowsWhenSectionMissing() {
        when(restaurantSectionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.deleteRestaurantSection(10L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.RESTAURANT_SECTION_NOT_FOUND);
    }

    @Test
    void createRestaurantThrowsWhenSectionMissing() {
        when(restaurantSectionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.createRestaurant(10L, restaurantRequest("식당")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.RESTAURANT_SECTION_NOT_FOUND);
    }

    @Test
    void updateRestaurantThrowsWhenRestaurantMissing() {
        when(restaurantRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.updateRestaurant(20L, restaurantRequest("식당")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.RESTAURANT_NOT_FOUND);
    }

    @Test
    void deleteRestaurantThrowsWhenRestaurantMissing() {
        when(restaurantRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.deleteRestaurant(20L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.RESTAURANT_NOT_FOUND);
    }

    @Test
    void createTransportationThrowsWhenMountainMissing() {
        when(mountainRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.createTransportation(1L, transportationRequest("정류장")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.MOUNTAIN_NOT_FOUND);
    }

    @Test
    void updateTransportationThrowsWhenTransportationMissing() {
        when(transportationRepository.findById(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.updateTransportation(30L, transportationRequest("정류장")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRANSPORTATION_NOT_FOUND);
    }

    @Test
    void deleteTransportationThrowsWhenTransportationMissing() {
        when(transportationRepository.findById(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMountainService.deleteTransportation(30L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.TRANSPORTATION_NOT_FOUND);
    }

    private Mountain mountain(Long id) {
        Mountain mountain = newInstance(Mountain.class);
        ReflectionTestUtils.setField(mountain, "id", id);
        ReflectionTestUtils.setField(mountain, "name", "관악산");
        ReflectionTestUtils.setField(mountain, "address", "서울 관악구");
        ReflectionTestUtils.setField(mountain, "altitude", 632.2);
        ReflectionTestUtils.setField(mountain, "difficulty", Difficulty.NORMAL);
        ReflectionTestUtils.setField(mountain, "duration", 120);
        ReflectionTestUtils.setField(mountain, "imageUrls", List.of("image.jpg"));
        ReflectionTestUtils.setField(mountain, "isPublic", true);
        return mountain;
    }

    private Course course(Long id, String name, List<Course.CourseWaypoint> waypoints) {
        Course course = newInstance(Course.class);
        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "name", name);
        ReflectionTestUtils.setField(course, "waypoints", waypoints);
        return course;
    }

    private AdminRestaurantRequest restaurantRequest(String name) {
        return new AdminRestaurantRequest(name, "양식", "파스타", "설명 변경",
                "new-image", "new-map", "new-blog");
    }

    private AdminTransportationRequest transportationRequest(String name) {
        return new AdminTransportationRequest(TransportationType.SUBWAY, "하행", name, "설명 변경");
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

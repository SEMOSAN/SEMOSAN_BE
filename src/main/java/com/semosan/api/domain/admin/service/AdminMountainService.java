package com.semosan.api.domain.admin.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.admin.dto.request.AdminMountainUpdateRequest;
import com.semosan.api.domain.admin.dto.request.AdminMountainVisibilityRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantRequest;
import com.semosan.api.domain.admin.dto.request.AdminRestaurantSectionRequest;
import com.semosan.api.domain.mountain.entity.Mountain;
import com.semosan.api.domain.mountain.entity.Restaurant;
import com.semosan.api.domain.mountain.entity.RestaurantSection;
import com.semosan.api.domain.mountain.repository.MountainRepository;
import com.semosan.api.domain.mountain.repository.RestaurantRepository;
import com.semosan.api.domain.mountain.repository.RestaurantSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMountainService {

    private final MountainRepository mountainRepository;
    private final RestaurantSectionRepository restaurantSectionRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void updateMountain(Long mountainId, AdminMountainUpdateRequest request) {
        Mountain mountain = findMountainById(mountainId);
        mountain.updateInfo(request.name(), request.address(), request.altitude(),
                request.difficulty(), request.duration());
        mountain.updateImageUrls(request.imageUrls());
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

    private Mountain findMountainById(Long mountainId) {
        return mountainRepository.findById(mountainId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOUNTAIN_NOT_FOUND));
    }
}

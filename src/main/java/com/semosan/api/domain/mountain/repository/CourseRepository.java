package com.semosan.api.domain.mountain.repository;

import com.semosan.api.domain.mountain.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByMountainId(Long mountainId);
}

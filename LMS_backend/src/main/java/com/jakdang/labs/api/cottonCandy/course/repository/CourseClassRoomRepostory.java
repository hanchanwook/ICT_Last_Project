package com.jakdang.labs.api.cottonCandy.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.jakdang.labs.entity.ClassroomEntity;

import feign.Param;

public interface CourseClassRoomRepostory extends JpaRepository<ClassroomEntity, String> {
    // 강의실 과정 조회
    @Query("SELECT c.classId, c.classNumber, c.classCapacity, co.courseId, co.courseDays, co.startTime, " +
            "co.endTime, co.courseStartDay, co.courseEndDay " +
            "FROM ClassroomEntity c " +
            "LEFT JOIN com.jakdang.labs.entity.CourseEntity co ON c.classId = co.classId " +
            "WHERE c.educationId = :educationId")
    List<Object[]> findCourseWithClassRoom(@Param("educationId") String educationId);
}

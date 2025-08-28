package com.jakdang.labs.api.cottonCandy.course.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jakdang.labs.api.cottonCandy.course.dto.RequestCourseDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.ResponseCourseDTO;
import com.jakdang.labs.api.cottonCandy.course.dto.ResponseStudentsListDTO;
import com.jakdang.labs.api.cottonCandy.course.repository.CourseMemberRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.CourseRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.SubGroupRepository;
import com.jakdang.labs.api.cottonCandy.course.repository.SubjectRepository;
import com.jakdang.labs.api.youngjae.repository.MemberRepository;
import com.jakdang.labs.api.chanwook.repository.ClassroomRepository;
import com.jakdang.labs.entity.CourseEntity;
import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.entity.SubGroupEntity;
import com.jakdang.labs.entity.ClassroomEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final SubGroupRepository subGroupRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final ClassroomRepository classroomRepository;
    private final MemberRepository memberRepository;
    
    // 과정 리스트 조회
    public List<ResponseCourseDTO> getCourseList(String educationId) {
        List<Object[]> results = courseRepository.findCourseWithSubGroup(educationId);
        Map<String, ResponseCourseDTO> courseMap = new HashMap<>();
        
        for (Object[] result : results) {
            String courseId = (String) result[0];
            String memberName = (String) result[1];
            String courseCode = (String) result[2];
            String courseName = (String) result[3];
            int maxCapacity = (int) result[4];
            int minCapacity = (int) result[5];
            String classNumber = String.valueOf(result[6]);
            String classId = String.valueOf(result[7]);
            // LocalDate 변환 안전 처리
            Object courseStartDayObj = result[8];
            LocalDate courseStartDay;
            
            int studentCount = memberRepository.countByCourseIdAndMemberRole(courseId, "ROLE_STUDENT");
            if (courseStartDayObj instanceof LocalDate) {
                courseStartDay = (LocalDate) courseStartDayObj;
            } else if (courseStartDayObj instanceof String) {
                courseStartDay = LocalDate.parse((String) courseStartDayObj);
            } else {
                courseStartDay = null;
            }
            Object courseEndDayObj = result[9];
            LocalDate courseEndDay;
            if (courseEndDayObj instanceof LocalDate) {
                courseEndDay = (LocalDate) courseEndDayObj;
            } else if (courseEndDayObj instanceof String) {
                courseEndDay = LocalDate.parse((String) courseEndDayObj);
            } else {
                courseEndDay = null;
            }
            String courseDays = (String) result[10];
            String startTime = String.valueOf(result[11]);
            String endTime = String.valueOf(result[12]);
            
            // createdAt 안전 처리
            Object createdAtObj = result[13];
            LocalDate createdAt;
            if (createdAtObj instanceof Instant) {
                createdAt = ((Instant) createdAtObj).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            } else if (createdAtObj instanceof LocalDate) {
                createdAt = (LocalDate) createdAtObj;
            } else if (createdAtObj instanceof String) {
                createdAt = LocalDate.parse((String) createdAtObj);
            } else {
                createdAt = null;
            }
            
            String subjectId = (String) result[14];
            String subjectName = (String) result[15];
            String subjectInfo = (String) result[16];
            Integer subjectTime = (Integer) result[17];

            // courseActive가 0인 데이터만 추가
            // courseActive는 쿼리에서 이미 0으로 필터링되어 있지만, 혹시 모를 데이터 오류 대비
            // courseActive 인덱스는 쿼리 SELECT 순서에 따라 조정 필요 (예: result[?])
            // 만약 courseActive가 result[?]에 있다면 아래처럼 체크
            // int courseActive = (int) result[?];
            // if (courseActive != 0) continue;
            
            if (!courseMap.containsKey(courseId)) {
                ResponseCourseDTO courseDTO = ResponseCourseDTO.builder()
                    .courseId(courseId)
                    .memberName(memberName)
                    .courseCode(courseCode)
                    .courseName(courseName)
                    .maxCapacity(maxCapacity)
                    .minCapacity(minCapacity)
                    .classNumber(classNumber)
                    .classId(classId)
                    .courseStartDay(courseStartDay)
                    .courseEndDay(courseEndDay)
                    .courseDays(courseDays)
                    .startTime(startTime)
                    .endTime(endTime)
                    .createdAt(createdAt)
                    .subjects(new ArrayList<>())
                    .studentCount(studentCount)
                    .build();
                courseMap.put(courseId, courseDTO); 
            }

            if (subjectName != null && subjectInfo != null) {
                ResponseCourseDTO.SubjectList subject = ResponseCourseDTO.SubjectList.builder()
                    .subjectId(subjectId)
                    .subjectName(subjectName)
                    .subjectInfo(subjectInfo)
                    .subjectTime(subjectTime)
                    .build();
                courseMap.get(courseId).getSubjects().add(subject);
            }
        }
        return new ArrayList<>(courseMap.values());
    }
    // 25애 랜덤 6자리 코드 생성

    // 과정 등록
    @Transactional
    public ResponseCourseDTO createCourse(RequestCourseDTO dto, String educationId) {
        CourseEntity course = new CourseEntity();

        String year = String.valueOf(LocalDate.now().getYear()).substring(2); // 년도 뒤 2자리
        int randomNum = new Random().nextInt(999999) + 1; // 1~999999
        String randomStr = String.format("%06d", randomNum); // 6자리로 포맷(앞에 0채움)

        course.setCourseName(dto.getCourseName());
        course.setEducationId(educationId); 
        course.setCourseCode(year + randomStr);
        course.setMemberId(dto.getMemberId());
        course.setClassId(dto.getClassId());
        course.setMaxCapacity(dto.getMaxCapacity());
        course.setMinCapacity(dto.getMinCapacity());
        course.setCourseStartDay(LocalDate.parse(dto.getCourseStartDay()));
        course.setCourseEndDay(LocalDate.parse(dto.getCourseEndDay()));
        // courseDays를 List<String>에서 String으로 변환
        String courseDaysStr = String.join(",", dto.getCourseDays());
        course.setCourseDays(courseDaysStr);
        course.setStartTime(dto.getStartTime());
        course.setEndTime(dto.getEndTime());

        courseRepository.save(course);

        // 과목 정보 처리
        if(dto.getSubjects() != null && !dto.getSubjects().isEmpty()){
            for(RequestCourseDTO.SubjectInfo subjectInfo : dto.getSubjects()){
                if(subjectInfo.getSubjectId() != null && !subjectInfo.getSubjectId().trim().isEmpty()){
                    try {                        
                        boolean exists = subjectRepository.existsById(subjectInfo.getSubjectId().trim());
                        if(!exists){
                            continue;
                        }
                        
                        SubGroupEntity subGroupEntity = SubGroupEntity.builder()
                            .subjectId(subjectInfo.getSubjectId().trim())
                            .courseId(course.getCourseId())
                            .subjectTime(subjectInfo.getSubjectTime())
                            .build();
                        
                        subGroupRepository.save(subGroupEntity);
                    } catch (Exception e) {
                        throw e;
                    }
                }
            }
        }
        List<Object[]> subDetailInfoList = subGroupRepository.findSubGroupInfoByCourseId(course.getCourseId());
        List<ResponseCourseDTO.SubjectList> subjects = subDetailInfoList.stream()
            .map(info -> ResponseCourseDTO.SubjectList.builder()
                .subjectId((String) info[0])
                .subjectName((String) info[1])
                .subjectInfo((String) info[2])
                .subjectTime((Integer) info[3])
                .build())
            .collect(Collectors.toList());
        return ResponseCourseDTO.builder()
            .courseId(course.getCourseId())
            .courseName(course.getCourseName())
            .educationId(course.getEducationId())
            .courseCode(course.getCourseCode())
            .maxCapacity(course.getMaxCapacity())
            .minCapacity(course.getMinCapacity())
            .classId(course.getClassId()) // 필요시 채우기
            .courseStartDay(course.getCourseStartDay())
            .courseEndDay(course.getCourseEndDay())
            .courseDays(course.getCourseDays())
            .startTime(course.getStartTime())
            .endTime(course.getEndTime())
            .subjects(subjects)
            .build();
    }

    // 과정 Active 수정
    public Boolean updateCourse(String courseId) {
        CourseEntity existingCourse = courseRepository.findById(courseId)
        .orElseThrow(() -> {
            return new RuntimeException("과정을 찾을 수 없습니다. ID: " + courseId);
        });
        existingCourse.setCourseActive(1);
        courseRepository.save(existingCourse);
        return true;
    }

   
    // 선택한 요일에 대해, 시작~끝 날짜 중 모든 날짜에서 공통적으로 가능한 시간대만 반환 (특정 강의실만 조회 가능)
    public List<String> getCommonAvailableTimesByEducationId(String educationId, String classId, LocalDate startDate, LocalDate endDate, List<String> daysOfWeek) {
        // 1. 선택한 요일에 해당하는 날짜 리스트 추출 (한글 요일로 비교)
        List<LocalDate> targetDates = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayKor = null;
            switch (date.getDayOfWeek().getValue()) {
                case 1: dayKor = "월"; break;
                case 2: dayKor = "화"; break;
                case 3: dayKor = "수"; break;
                case 4: dayKor = "목"; break;
                case 5: dayKor = "금"; break;
            }
            if (daysOfWeek.contains(dayKor)) {
                targetDates.add(date);
            }
        }
        // 2. 각 날짜별로 예약이 없는 시간대 리스트 추출 (특정 강의실만)
        List<Set<String>> availablePerDay = new ArrayList<>();
        for (LocalDate date : targetDates) {
            Set<String> availableSlots = new HashSet<>();
            List<ClassroomEntity> classrooms;
            if (classId != null && !classId.isEmpty()) {
                classrooms = classroomRepository.findByClassIdAndClassActive(classId, 0);
            } else {
                classrooms = classroomRepository.findByEducationIdAndClassActive(educationId, 0);
            }
            for (ClassroomEntity classroom : classrooms) {
                String cid = classroom.getClassId();
                String[] times = {"09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00","19:00","20:00"};
                for (int i = 0; i < times.length; i++) {
                    String slotStart = times[i];
                    int hour = Integer.parseInt(times[i].split(":")[0]) + 1;
                    String slotEnd = String.format("%02d:00", hour);
                    boolean isReserved = false;
                    List<CourseEntity> reservedCourses = courseRepository.findReservedCoursesByClassIdAndPeriod(cid, date, date);
                    String dayKor = null;
                    switch (date.getDayOfWeek().getValue()) {
                        case 1: dayKor = "월"; break;
                        case 2: dayKor = "화"; break;
                        case 3: dayKor = "수"; break;
                        case 4: dayKor = "목"; break;
                        case 5: dayKor = "금"; break;
                    }
                    for (CourseEntity course : reservedCourses) {
                        List<String> reservedDays = List.of(course.getCourseDays().split(","));
                        if (reservedDays.contains(dayKor)) {
                            if (!(slotEnd.compareTo(course.getStartTime()) <= 0 || slotStart.compareTo(course.getEndTime()) >= 0)) {
                                isReserved = true;
                                break;
                            }
                        }
                    }
                    if (!isReserved) {
                        availableSlots.add(slotStart);
                    }
                }
            }
            availablePerDay.add(availableSlots);
        }
        // 3. 모든 날짜의 교집합 구하기
        if (availablePerDay.isEmpty()) return new ArrayList<>();
        Set<String> common = new HashSet<>(availablePerDay.get(0));
        for (Set<String> slots : availablePerDay) {
            common.retainAll(slots);
        }
        List<String> result = new ArrayList<>(common);
        result.sort(String::compareTo);
        return result;
    }

    // 해당 기관 학생 조회
    public List<MemberEntity> getStudentsByEducationId(String educationId) {
        return courseMemberRepository.findByEducationIdAndMemberRole(educationId, "ROLE_STUDENT");
    }

    // 해당 기관 강사 조회
    public List<MemberEntity> getTeachersByEducationId(String educationId) {
        return courseMemberRepository.findByEducationIdAndMemberRole(educationId, "ROLE_INSTRUCTOR");
    }

    // 해당 강의 수강 학생 리스트
    public List<ResponseStudentsListDTO> getStudentsByCourseId(String courseId) {
        List<MemberEntity> allMembers = courseMemberRepository.findByCourseId(courseId);
        return allMembers.stream()
            .filter(member -> "ROLE_STUDENT".equals(member.getMemberRole())) // 학생만 필터링
            .map(member -> ResponseStudentsListDTO.builder()
                .memberId(member.getMemberId())
                .memberName(member.getMemberName())
                .memberEmail(member.getMemberEmail())
                .memberPhone(member.getMemberPhone())
                .createdAt(member.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                .build())
            .collect(Collectors.toList());
    }
}







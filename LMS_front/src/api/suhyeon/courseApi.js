// 강의 관련 API 함수들
import { http, baseURL } from "../../components/auth/http";
import qs from 'qs';


// 전체 세부과목 조회
export const getAllSubDetail = async () => {
  const response = await http.get("/api/subDetail/list");
  return response.data.data;
};

// 세부과목 등록
export const createSubDetail = async (subDetailData) => {
  const response = await http.post("/api/subDetail/create", subDetailData);
  return response.data;
};

// 세부과목 수정
export const updateSubDetail = async (id, subDetailData) => {
  const response = await http.post(`/api/subDetail/update/${id}`, subDetailData);
  return response.data;
};

// 세부과목 삭제
export const deleteSubDetail = async (id) => {
  const response = await http.post(`/api/subDetail/delete/${id}`);
  return response.data;
};

// 전체 과목 조회
export const getAllSubject = async () => {
  const response = await http.get("/api/subject/list"); 
  return response.data.data;
};

// 과목 등록
export const createSubject = async (subjectData) => {
  const response = await http.post("/api/subject/create", subjectData);
  return response.data;
};

// 과목 수정
export const updateSubject = async (id, subjectData) => {
  const response = await http.post(`/api/subject/update/${id}`, subjectData);
  return response.data;
};

// 과정 삭제
export const deleteSubject = async (id) => {
  const response = await http.post(`/api/subject/delete/${id}`);
  return response.data;
};

// 전체 과정 조회
export const getAllCourse = async () => {
  const response = await http.get("/api/course/list"); 
  return response.data.data;
};

// 과정 등록
export const createCourse = async (courseData) => {
  const response = await http.post("/api/course/create", courseData);
  return response.data;
};

// 과정 수정
export const updateCourse = async (id, courseData) => {
  const response = await http.post(`/api/course/update/${id}`, courseData);
  return response.data;
};

// 과정 삭제
export const deleteCourse = async (id) => {
  const response = await http.post(`/api/course/delete/${id}`);
  return response.data;
};

// 강의실정보 조회 (classId, courseStartDay, courseEndDay, courseDays로 예약 가능한 시간 조회)
export const getCourseClassroom = async (classId, courseStartDay, courseEndDay, courseDays) => {
  const params = {
    classId,
    courseStartDay,
    courseEndDay,
    courseDays,
  };
  const response = await http.get("/api/course/classroom", {
    params,
    paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' }),
  });
  return response.data.data;
};

// 해당 기관 전체 강사 조회
export const getAllEduInstructor = async () => {
  const response = await http.get("/api/course/teachers"); 
  return response.data.data;
};

// 해당 강의를 수강하는 학생 조회
export const getAdminStudentsByCourse = async (courseId) => {
  const response = await http.get(`/api/course/students/${courseId}`);
  return response.data.data;
};
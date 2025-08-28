// 평가 관련 API 함수들
import { http, baseURL } from "../../components/auth/http";

// ===== 평가 항목 관련 API =====

// 전체 평가 항목 조회
export const getAllEvaluation = async () => {
  const response = await http.get("/api/evaluation/list");
  return response.data.data;
};

// 평가 항목 등록
export const createEvaluation = async (evaluationData) => {
  const response = await http.post("/api/evaluation/create", evaluationData);
  return response.data.data;
};

// 평가 항목 삭제
export const deleteEvaluation = async (evalQuestionId) => {
  const response = await http.post(`/api/evaluation/delete/${evalQuestionId}`);
  return response.data.data;
};

// ===== 평가 항목 템플릿 관련 API =====
// 평가 항목 템플릿 등록
export const createEvaluationTemplate = async (evaluationData) => {
  const response = await http.post("/api/questiontemplate/create", evaluationData);
  return response.data.data;
};

// 평가 항목 템플릿 목록 조회
export const getEvaluationTemplateList = async () => {
  const response = await http.get("/api/questiontemplate/list");
  return response.data.data;
};

// 평가 항목 템플릿 삭제
export const deleteEvaluationTemplate = async (questionTemplateNum) => {
  const response = await http.post(`/api/questiontemplate/delete`, { questionTemplateNum });
  return response.data.data;
};

// 평가 항목 템플릿 수정
export const updateEvaluationTemplate = async (evaluationData) => {
  const response = await http.post(`/api/questiontemplate/update`, evaluationData);
  return response.data.data;
};

// 과정의 평가 목록 조회
export const getCourseEvaluation = async (courseId) => {
  const response = await http.get(`/api/templategroup/list`);
  return response.data.data;
};

// ===== 과정에 평가 템플릿 관련 API =====

// 과정에 평가 템플릿 설정
export const setCourseEvaluationTemplate = async (templateData) => {
  const response = await http.post(`/api/templategroup/create`, templateData);
  return response.data.data;
};

// 과정에 평가 템플릿 수정
export const updateCourseEvaluationTemplate = async (templateData) => {
  const response = await http.post(`/api/templategroup/update`, templateData);
  return response.data.data;
};

// 과정에 평가 템플릿 삭제
export const deleteCourseEvaluationTemplate = async (templateGroupId) => {
  const response = await http.post(`/api/templategroup/delete/${templateGroupId}`);
  return response.data;
};

// ===== 학생용 설문 평가 API =====

// 학생의 수강 강의 목록 조회 (설문 평가 가능한 강의들)
export const getStudentCourseList = async () => {
  const response = await http.get("/api/evaluation/student/course/list");
  return response.data.data;
};

// 특정 강의의 설문 문항 조회 - 학생 답변이 없는 경우 문항 조회
export const getCourseEvaluationQuestions = async (templateGroupId) => {
    const response = await http.get(`/api/evaluation/student/evaluation/list?templateGroupId=${templateGroupId}`);        
    return response.data.data;
 
};

// 설문 응답 제출
export const submitEvaluationResponse = async (responseData) => {
  const response = await http.post("/api/evaluation/student/evaluation/answer", responseData);
  return response.data.data;
};

// 학생이 작성한 설문 응답 조회  - 학생 답변이 있는 경우 문항 조회
export const getStudentEvaluationResponse = async (templateGroupId) => {
    const response = await http.get(`/api/evaluation/student/evaluation/response?templateGroupId=${templateGroupId}`);
    return response.data.data?.data || response.data.data || []; 
};

// ===== 강사용 설문 평가 API =====
export const getInstructorEvaluationList = async () => {
  const response = await http.get("/api/instructor/evaluations/list");
  return response.data.data;
};
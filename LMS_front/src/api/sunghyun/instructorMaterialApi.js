// 강의 자료 Presigned URL 기반 업로드 API
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:19091'
const FILE_SERVICE_URL = import.meta.env.VITE_FILE_SERVICE_URL || 'http://localhost:8080'
const DEFAULT_MEMBER_TYPE = 'INSTRUCTOR'
const DEFAULT_USER_ID = import.meta.env.VITE_DEFAULT_USER_ID || "0e1bfae2-fdb3-4db9-9294-a2460fad86a3"
const SUPPORTED_IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']
const SUPPORTED_AUDIO_EXTENSIONS = ['mp3', 'wav', 'ogg', 'aac', 'flac', 'm4a']
const SUPPORTED_VIDEO_EXTENSIONS = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv']

// 1. Presigned URL 요청
export const getPresignedUrl = async (fileName, ownerId, memberType, userId) => {
    const response = await axios.get(
        // `${API_BASE_URL}/api/instructor/file/upload/${fileName}`,
        `${API_BASE_URL}/api/v2/file/upload/${fileName}`,
        {
            params: { ownerId, memberType, userId }
        }
    );
    return response.data;
};

// 2. S3에 직접 업로드
export const uploadToS3 = async (presignedUrl, file) => {
    const response = await fetch(presignedUrl, {
        method: 'PUT',
        body: file,
        headers: {
            'Content-Type': file.type
        }
    });
    
    if (!response.ok) {
        throw new Error('S3 업로드 실패');
    }
    
    return true;
};

// 3. 파일 업로드 완료 알림
export const completeFileUpload = async (fileData) => {
    const response = await axios.post(
        `${API_BASE_URL}/api/instructor/file/upload/complete`,
        fileData
    );
    return response.data;
};

// 4. 강의 자료 업로드 수정
export const uploadCourseMaterial = async (lectureId, formData) => {
    try {
        const file = formData.get('file');
        const fileName = file.name;
        const ownerId = lectureId;
        const memberType = "INSTRUCTOR";
        const userId = "id"; // Hardcoded as per user's request
        
        // FileEnum 변환 함수 - 더 안전한 파일 타입 감지
        function getFileEnum(file) {
            if (!file || !file.type) {
                console.warn('파일 또는 파일 타입이 없습니다. 기본값 "file"을 사용합니다.');
                return 'file';
            }
            
            const mime = file.type.toLowerCase();
            const extension = file.name.split('.').pop()?.toLowerCase();
            
            // MIME 타입 기반 감지
            if (mime.startsWith('image/')) return 'image';
            if (mime.startsWith('audio/')) return 'audio';
            if (mime.startsWith('video/')) return 'video';
            
            // 확장자 기반 감지 (MIME 타입이 없는 경우)
            const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
            const audioExtensions = ['mp3', 'wav', 'ogg', 'aac', 'flac', 'm4a'];
            const videoExtensions = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv'];
            
            if (imageExtensions.includes(extension)) return 'image';
            if (audioExtensions.includes(extension)) return 'audio';
            if (videoExtensions.includes(extension)) return 'video';
            
            // 기본값
            return 'file';
        }
        
        // 1. Presigned URL 요청
        const presignedData = await getPresignedUrl(fileName, ownerId, memberType, userId);
        
        // presignedData 검증
        if (!presignedData.data.key) {
            throw new Error('Presigned URL에서 file_key를 가져올 수 없습니다.');
        }
        
        // 2. S3에 직접 업로드
        await uploadToS3(presignedData.data.url, file);
        
        // 3. 파일 업로드 완료 알림
        const completeData = await completeFileUpload({
            key: presignedData.data.key,
            name: file.name,
            size: file.size,
            type: getFileEnum(file), // FileEnum 값으로 변환
            width: 0,
            height: 0,
            index: 0,
            active: true,
            duration: 0,
            thumbnailKey: ''
        });
        
        // 4. 강의 자료 저장 (fileId 포함)
        const fileType = getFileEnum(file);
        
        // 파일 타입 검증
        if (!fileType || fileType === '') {
            throw new Error('파일 타입을 확인할 수 없습니다.');
        }
        
        // 썸네일 URL 생성 (이미지 파일인 경우에만)
        const isImageFile = (fileName) => {
            if (!fileName) return false;
            const extension = fileName.toLowerCase().split('.').pop();
            return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(extension);
        };
        
        const thumbnailUrl = isImageFile(file.name) 
            ? `${API_BASE_URL}/api/file/download/${presignedData.data.key}`
            : "";
        
        const materialData = {
            fileId: completeData.data.id,
            fileKey: presignedData.data.key,
            title: formData.get('title') || file.name,
            fileName: file.name,
            fileSize: file.size,
            fileType: fileType,
            thumbnailUrl: thumbnailUrl,
            memberId: "0e1bfae2-fdb3-4db9-9294-a2460fad86a3",
            memberType: "INSTRUCTOR",
        };
        

        
        // materialData의 모든 필드 null/undefined/빈 문자열 체크
        Object.entries(materialData).forEach(([k, v]) => {
            if (v === null || v === undefined || v === '') {
                console.warn(`필드 ${k}가 비어있음!`);
            }
        });
        
        const materialResponse = await axios.post(
            `${API_BASE_URL}/api/instructor/lectures/${lectureId}/materials`,
            materialData
        );
        
        return materialResponse.data;
        
    } catch (error) {
        console.error('파일 업로드 실패:', error);
        
        // 모든 HTTP 상태 코드에 대한 상세 정보 출력
        if (error.response) {
            console.error('=== 서버 응답 상세 정보 ===');
            console.error('상태 코드:', error.response.status);
            console.error('상태 텍스트:', error.response.statusText);
            console.error('응답 헤더:', error.response.headers);
            console.error('응답 데이터:', error.response.data);
            
            // 500 오류의 경우 더 자세한 정보 출력
            if (error.response.status === 500) {
                console.error('=== 500 오류 상세 분석 ===');
                console.error('요청 URL:', error.config?.url);
                console.error('요청 메서드:', error.config?.method);
                console.error('요청 데이터:', error.config?.data);
                console.error('전송된 payload:', materialData);
                
                if (error.response.data) {
                    console.error('서버 에러 응답 (문자열):', JSON.stringify(error.response.data, null, 2));
                    
                    const dataStr = JSON.stringify(error.response.data);
                    if (dataStr.includes('constraint')) {
                        console.error('🔍 DB 제약조건 위반 에러가 발생했습니다.');
                    }
                    if (dataStr.includes('duplicate')) {
                        console.error('🔍 중복 데이터 에러가 발생했습니다.');
                    }
                    if (dataStr.includes('foreign')) {
                        console.error('🔍 외래 키 제약조건 위반 에러가 발생했습니다.');
                    }
                }
            }
        }
        
        // 파일 타입 관련 에러인지 확인
        if (error.message && error.message.includes('파일 타입')) {
            console.error('파일 타입 감지 실패:', error.message);
        }
        
        throw error;
    }
};

// 강의 자료 삭제 - 파일 서비스 API 사용
export const deleteCourseMaterial = async (materialId) => {
    if (!materialId) {
        console.error('❌ materialId가 없습니다!');
        throw new Error('파일 ID 누락');
    }
    
    try {
        // 파일 서비스의 삭제 API 호출 (포트 8080)
        const response = await axios.delete(
            `http://localhost:8080/api/file/${materialId}`
        );
        
        if (response.status !== 200 && response.status !== 204) {
            throw new Error('파일 삭제 실패');
        }
        
        return response.data;
    } catch (error) {
        console.error('❌ 파일 삭제 오류:', error);
        console.error('❌ 에러 메시지:', error.message);
        console.error('❌ 에러 응답:', error.response);
        
        // 400 에러의 경우 더 자세한 정보 출력
        if (error.response?.status === 400) {
            console.error('🔍 400 에러 상세 정보:', {
                status: error.response.status,
                statusText: error.response.statusText,
                data: error.response.data,
                headers: error.response.headers
            });
            
            if (error.response.data) {
                console.error('🔍 파일 서비스 에러 응답:', error.response.data);
                console.error('🔍 에러 응답 (문자열):', JSON.stringify(error.response.data, null, 2));
            }
        }
        
        throw error;
    }
}; 
/**
 * 썸네일 처리와 파일 아이콘 생성을 위한 공통 유틸리티 함수들
 */

// 썸네일 이미지 URL 생성 함수
export const getThumbnailUrl = (material) => {
  if (!material) return null;
  
  // 먼저 material.thumbnailUrl이 있는지 확인
  if (material.thumbnailUrl && material.thumbnailUrl !== "") {
    return material.thumbnailUrl;
  }
  
  // fileId 추출 (다양한 필드명 지원)
  const fileId = material.materialId || material.fileId || material.id || material.file_id || material.material_id;
  if (!fileId) {
    console.log('썸네일 URL 생성 실패: fileId를 찾을 수 없음', material);
    return null;
  }
  
  // 파일명 추출
  const fileName = material.fileName || material.name || material.title;
  if (!fileName) {
    console.log('썸네일 URL 생성 실패: fileName을 찾을 수 없음', material);
    return null;
  }
  
  const extension = fileName.toLowerCase().split('.').pop();
  const isImage = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(extension);
  
  if (isImage) {
    // 이미지 파일인 경우 다양한 API 엔드포인트 시도
    const baseUrl = 'http://localhost:19091';
    const possibleUrls = [
      `${baseUrl}/api/v2/file/thumbnail/${fileId}`, // 전용 썸네일 API
      `${baseUrl}/api/v2/file/image/${fileId}`,     // 이미지 API
      `${baseUrl}/api/file/download/${fileId}`,     // 기존 다운로드 API
      `${baseUrl}/api/v2/file/download/${fileId}`,  // v2 다운로드 API
    ];
    
    console.log('썸네일 URL 생성:', {
      fileId,
      fileName,
      isImage,
      possibleUrls
    });
    
    // 첫 번째 URL 반환 (실제로는 여러 URL을 시도하는 로직을 구현할 수 있음)
    return possibleUrls[0];
  }
  
  return null;
};

// 여러 URL을 시도하여 이미지를 로드하는 함수
export const tryLoadImage = async (material) => {
  if (!material) return null;
  
  const thumbnailUrl = getThumbnailUrl(material);
  if (!thumbnailUrl) return null;
  
  try {
    const response = await fetch(thumbnailUrl, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      }
    });
    
    if (response.ok) {
      return thumbnailUrl;
    }
  } catch (error) {
    console.log('이미지 로드 실패:', error);
  }
  
  return null;
};

// 파일 타입 확인 함수
export const isImageFile = (fileName) => {
  if (!fileName) return false;
  const extension = fileName.toLowerCase().split('.').pop();
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(extension);
};

// 포괄적인 파일 아이콘 생성 함수
export const getFileIcon = (fileName) => {
  if (!fileName) return '📄'; // 기본 파일 아이콘
  
  const extension = fileName.toLowerCase().split('.').pop();
  
  switch (extension) {
    // 문서 파일
    case 'pdf':
      return '📄';
    case 'doc':
    case 'docx':
      return '📝';
    case 'txt':
      return '📄';
    case 'rtf':
      return '📄';
    
    // 프레젠테이션 파일
    case 'ppt':
    case 'pptx':
      return '📊';
    case 'key':
      return '📊';
    
    // 스프레드시트 파일
    case 'xls':
    case 'xlsx':
      return '📈';
    case 'csv':
      return '📈';
    
    // 이미지 파일
    case 'jpg':
    case 'jpeg':
      return '🖼️';
    case 'png':
      return '🖼️';
    case 'gif':
      return '🖼️';
    case 'bmp':
      return '🖼️';
    case 'webp':
      return '🖼️';
    case 'svg':
      return '🖼️';
    case 'tiff':
      return '🖼️';
    
    // 비디오 파일
    case 'mp4':
      return '🎥';
    case 'avi':
      return '🎥';
    case 'mov':
      return '🎥';
    case 'wmv':
      return '🎥';
    case 'flv':
      return '🎥';
    case 'webm':
      return '🎥';
    case 'mkv':
      return '🎥';
    
    // 오디오 파일
    case 'mp3':
      return '🎵';
    case 'wav':
      return '🎵';
    case 'aac':
      return '🎵';
    case 'ogg':
      return '🎵';
    case 'flac':
      return '🎵';
    
    // 압축 파일
    case 'zip':
      return '📦';
    case 'rar':
      return '📦';
    case '7z':
      return '📦';
    case 'tar':
      return '📦';
    case 'gz':
      return '📦';
    
    // 코드 파일
    case 'js':
    case 'jsx':
      return '💻';
    case 'ts':
    case 'tsx':
      return '💻';
    case 'html':
    case 'htm':
      return '🌐';
    case 'css':
      return '🎨';
    case 'java':
      return '☕';
    case 'py':
      return '🐍';
    case 'cpp':
    case 'c':
      return '⚙️';
    case 'php':
      return '🐘';
    case 'rb':
      return '💎';
    case 'go':
      return '🐹';
    case 'rs':
      return '🦀';
    case 'swift':
      return '🍎';
    case 'kt':
      return '🤖';
    
    // 데이터 파일
    case 'json':
      return '📋';
    case 'xml':
      return '📋';
    case 'yaml':
    case 'yml':
      return '📋';
    case 'sql':
      return '🗄️';
    
    // 기타 파일
    case 'exe':
      return '⚙️';
    case 'dmg':
      return '💿';
    case 'deb':
      return '📦';
    case 'rpm':
      return '📦';
    
    default:
      return '📄';
  }
};

// 파일 크기 포맷 함수
export const formatFileSize = (bytes) => {
  if (!bytes) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 썸네일 렌더링 컴포넌트 생성 함수
export const createThumbnailRenderer = (material, className = "w-8 h-8 object-cover rounded") => {
  const fileName = material?.fileName || material?.name || material?.title;
  const thumbnailUrl = getThumbnailUrl(material);
  const isImage = isImageFile(fileName);
  
  console.log('썸네일 렌더링:', {
    fileName,
    isImage,
    thumbnailUrl,
    material
  });
  
  if (isImage && thumbnailUrl) {
    return {
      type: 'image',
      src: thumbnailUrl,
      alt: fileName || '이미지',
      className,
      fallbackIcon: getFileIcon(fileName)
    };
  } else {
    return {
      type: 'icon',
      icon: getFileIcon(fileName)
    };
  }
}; 
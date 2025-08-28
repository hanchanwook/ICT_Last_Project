import React from 'react';
import { getThumbnailUrl, isImageFile, getFileIcon } from '../../api/sunghyun/thumbnailUtils';

/**
 * 썸네일과 파일 아이콘을 렌더링하는 컴포넌트
 * 
 * @param {Object} material - 파일 정보 객체
 * @param {string} className - 이미지 CSS 클래스 (기본값: "w-8 h-8 object-cover rounded")
 * @param {string} iconClassName - 아이콘 CSS 클래스 (기본값: "text-lg")
 * @param {boolean} showDebug - 디버그 로그 표시 여부 (기본값: false)
 * @returns {JSX.Element} 썸네일 또는 파일 아이콘
 */
const ThumbnailRenderer = ({ 
  material, 
  className = "w-8 h-8 object-cover rounded",
  iconClassName = "text-lg",
  showDebug = false 
}) => {
  const fileName = material?.fileName || material?.name || material?.title;
  const thumbnailUrl = getThumbnailUrl(material);
  const isImage = isImageFile(fileName);
  
  if (showDebug) {
    console.log('ThumbnailRenderer:', {
      fileName,
      isImage,
      thumbnailUrl,
      material
    });
  }
  
  if (isImage && thumbnailUrl) {
    return (
      <>
        <img 
          src={thumbnailUrl} 
          alt={fileName || '이미지'}
          className={className}
          crossOrigin="anonymous"
          loading="lazy"
          onError={(e) => {
            if (showDebug) {
              console.log('이미지 로드 실패, 아이콘으로 대체:', fileName);
            }
            e.target.style.display = 'none';
            e.target.nextSibling.style.display = 'block';
          }}
          onLoad={() => {
            if (showDebug) {
              console.log('이미지 로드 성공:', fileName);
            }
          }}
        />
        <span className={`hidden ${iconClassName}`}>
          {getFileIcon(fileName)}
        </span>
      </>
    );
  } else {
    return (
      <span className={iconClassName}>
        {getFileIcon(fileName)}
      </span>
    );
  }
};

export default ThumbnailRenderer; 
# 무중단 통합 교육관 관리 시스템 - LMSync

## 팀원 정보
- **팀명**: LMSync
- **팀장**: 김재겸
- **팀원**: 김가영, 김용호, 이수현, 최성현, 최영재, 한찬

---

## 프로젝트 목적
다수의 교육기관이 사용할 수 있는 통합 학사관리 시스템 구축
반응형 웹 기반의 사용자 맞춤 학사 서비스 제공


**트라이어드 솔루션**은 **인사관리** 및 **전자결제 시스템(ERP)**을 제공하는 비즈니스 플랫폼입니다. 이 시스템은 기업의 효율적인 인사 관리와 전자 결제 업무를 원활하게 처리할 수 있도록 돕습니다. 사용자는 각종 인사 정보 관리와 전자 결제를 통해 비즈니스 운영을 더욱 자동화하고 효율적으로 관리할 수 있습니다.

---

## 사용된 기술

### Front-End
- **React**: 컴포넌트 기반 UI 라이브러리로, 사용자 인터페이스를 효율적으로 구성
- **TypeScript (TS)**: 자바스크립트에 정적 타입을 추가해 코드 안정성과 가독성을 높이는 언어
- **Axios**: 브라우저와 Node.js에서 사용 가능한 Promise 기반 HTTP 클라이언트 (API 통신)
- **React Router**: SPA(Single Page Application)에서 라우팅(페이지 전환) 기능 제공
- **Tailwind CSS**: 유틸리티 클래스 기반의 CSS 프레임워크로 빠른 스타일링 지원
- **Lucide**: 가볍고 현대적인 아이콘 라이브러리
- **Sonner**: React 애플리케이션에서 간단한 알림(Toast) UI 제공 라이브러리
- **Radix**: 접근성(A11y)을 고려한 React UI 컴포넌트 라이브러리
- **Lexical**: Meta(구 Facebook)에서 만든 가볍고 확장성 있는 리치 텍스트 에디터 프레임워크
- **Monaco Editor**: VS Code에서 사용하는 코드 에디터 컴포넌트 (브라우저 내 코드 편집 기능 제공)
- **date-fns**: 날짜/시간 처리를 위한 경량 자바스크립트 유틸리티 라이브러리
- **Node.js**: 자바스크립트 런타임 환경으로 서버 사이드 실행 가능
- **Vite**: 빠른 빌드와 개발 서버 제공을 목표로 한 프론트엔드 빌드 툴
- **Nginx**: 웹 서버 및 리버스 프록시 서버로 정적 파일 서빙, 로드 밸런싱 지원

### Back-End
- **Spring Boot**: 자바 기반 애플리케이션 개발을 위한 프레임워크, 내장 서버와 다양한 스타터 제공
- **Spring Data JPA**: JPA(Java Persistence API)를 쉽게 사용할 수 있도록 도와주는 스프링 데이터 모듈
- **Spring Security**: 인증과 권한 부여(Authorization)를 처리하는 보안 프레임워크
- **Spring Cloud OpenFeign**: 마이크로서비스 간 통신을 쉽게 하기 위한 선언형 REST 클라이언트
- **Jakarta EE**: 자바 엔터프라이즈 애플리케이션 개발을 위한 표준 사양
- **STOMP**: WebSocket 위에서 동작하는 메시징 프로토콜 (실시간 양방향 통신 구현)
- **JWT (JSON Web Token)**: 사용자 인증과 정보 전달을 위한 토큰 기반 인증 방식
- **QueryDSL**: 타입 안전한 JPQL을 작성할 수 있는 쿼리 빌더 라이브러리
- **Gradle**: 빌드 자동화 도구로, 프로젝트 의존성 관리 및 빌드 스크립트 작성 지원
- **dotenv**: .env 파일을 통해 환경 변수 관리 (보안 설정 및 환경 분리)
- **Lombok**: 반복되는 Getter, Setter, 생성자 등을 자동으로 생성해주는 코드 단축 라이브러리
- **MySQL**: 관계형 데이터베이스 관리 시스템(RDBMS)
- **HSQLDB (HyperSQL Database)**: 가볍고 빠른 인메모리 데이터베이스 (테스트용으로 많이 사용)
- **Apache POI**: MS Office(특히 Excel, Word) 문서를 자바에서 읽고 쓰기 위한 라이브러리
- **REST API**: Representational State Transfer 원칙을 따른 API 설계 방식


### Version Control & Collaboration
- **GitHub**: 소스 코드 버전 관리 및 협업 플랫폼
- **GitHub Actions**: CI/CD(지속적 통합·배포) 워크플로우 자동화 도구
- **Sourcetree**: Git GUI 클라이언트로 브랜치 관리와 커밋 작업을 직관적으로 지원
- **Notion**: 프로젝트 문서화 및 협업을 위한 올인원 워크스페이스
- **Discord**: 실시간 음성/텍스트 채팅을 통한 팀 커뮤니케이션 도구
- **Ubuntu**: 리눅스 기반 서버 운영체제
- **PuTTY**: Windows 환경에서 SSH, Telnet 등 원격 접속을 지원하는 클라이언트
- **MobaXterm**: SSH, SFTP, X11 등 다양한 원격 접속 기능을 통합 제공하는 툴
- **AWS (Amazon Web Services)**: 클라우드 서비스 플랫폼
- **Amazon EC2**: 가상 서버(인스턴스) 제공 서비스
- **Amazon S3**: 객체 스토리지 서비스로 파일 및 데이터 저장 관리
- **Docker**: 애플리케이션을 컨테이너 단위로 배포/실행할 수 있는 플랫폼

---

### 프로젝트에 대한 PPT
**PPT**
https://www.canva.com/design/DAGsjb9QxUQ/4xjvpa2577XkvSgeSnmJUw/edit

**Flow-Chat**
https://www.figma.com/board/PcIatsSUZh5JugDRAgYtCk/LMSync-%ED%94%8C%EB%A1%9C%EC%9A%B0-%EC%B0%A8%ED%8A%B8?node-id=0-1

**시연영상 및 자료료**
https://drive.google.com/drive/folders/1DdCM3h6TjwsCP42KAWNusQrR8dmt08Gj

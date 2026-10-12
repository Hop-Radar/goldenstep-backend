# 👣 GoldenStep · 골든스텝

> 마지막으로 확인된 위치에서, 먼저 확인할 장소까지.

골든스텝은 실종자의 마지막 확인 위치와 시각을 바탕으로 **예상 수색 범위와 우선 확인 장소를 안내하는 지도 기반 수색 지원 서비스**입니다. 회원가입 없이 수색 정보를 입력하고, 시간점별 결과·장소 확인 상태·공유 링크를 통해 초기 수색에 필요한 정보를 확인할 수 있습니다.

이 저장소는 세션 관리, 비동기 분석 연동, 결과 저장과 공유를 담당하는 **백엔드 프로젝트**입니다.

<br>

## 🎯 서비스 의도

실종 상황에서 보호자와 수색 참여자는 마지막 위치를 알고 있어도, 시간이 지난 뒤 어느 곳을 먼저 확인해야 할지 판단하기 어렵습니다. 지도에 원형 반경만 표시하는 방법은 도로의 연결 구조와 보행 환경을 충분히 반영하기 어렵습니다.

골든스텝은 마지막 확인 위치·경과 시간·보행 네트워크·주변 장소 데이터를 연결하여 수색자가 참고할 수 있는 영역과 우선 확인 장소를 제공합니다. 확인한 장소를 기록하고, 같은 시점의 수색 정보를 다른 참여자에게 공유할 수 있도록 구성했습니다.

이를 통해 **“제한된 시간과 인력으로 초기 수색을 이어갈 수 있도록 돕는 생활안전 서비스”**를 구현하고자 했습니다. 경찰·소방·지자체의 수색 판단을 보조하는 공공 활용을 지향하며, 실제 기관 도입이나 발견 시간 단축 성과를 의미하지는 않습니다.

<br>

## 💻 주요 기능

| 기능 | 내용 |
| --- | --- |
| 수색 정보 입력 | 마지막 위치·시각·대상자 정보를 검증하고 수색 세션 생성 |
| 비동기 분석 | 입력 저장 이후 별도 스레드에서 분석하고 진행·완료·실패 상태 제공 |
| 시간점별 수색 결과 | 현재 및 30분·1시간·3시간·6시간 이후의 결과 생성·조회 |
| 우선 확인 장소 | 예측 영역과 최대 3개 장소를 제공하고 주소·장소명 보완 |
| 현장 확인 관리 | 확인한 장소의 완료 시각 저장 |
| 수색 이어가기 | 복원 쿠키로 24시간 유효한 세션 조회 |
| 공유 스냅샷 | 발급 당시 영역·장소·확인 상태를 복제하고 12시간 공유 링크 제공 |

공유 결과는 **발급 당시의 스냅샷**입니다. 원본의 후속 변경이 실시간으로 동기화되는 공동 편집 기능과 구분됩니다.

### 페이지 캡쳐
#### 메인페이지
![GoldenStep 메인페이지](https://github.com/user-attachments/assets/f466fc14-b06d-456a-b208-064247dd0b8c)

#### 탐색정보 입력페이지
![GoldenStep 탐색정보 입력페이지1](https://github.com/user-attachments/assets/a1009120-79f3-4f9a-bfe1-62f38a6dd193)

![GoldenStep 탐색정보 입력페이지2](https://github.com/user-attachments/assets/2370481b-2368-4721-85f5-afe682653452)

#### 탐색 결과 페이지
![GoldenStep 탐색 결과페이지](https://github.com/user-attachments/assets/6f1f8906-4f57-4f8d-a41c-1adc09414cc4)

#### 탐색 결과 상세 사이드바
![GoldenStep 탐색 결과 상세 사이드바](https://github.com/user-attachments/assets/3504f5be-6bf3-4f48-8356-2b61be00d1c7)

#### 탐색 요약 대시보드
![GoldenStep 탐색 요약 대시보드](https://github.com/user-attachments/assets/2f0cbe43-3867-429d-9095-2475fcd2607b)

#### 공유화면 스냅샷
![GoldenStep 공유 스냅샷 페이지1](https://github.com/user-attachments/assets/91e7e8d6-e0b9-47c0-823f-8fa7ca217f8d)

![GoldenStep 공유 스냅샷 페이지2](https://github.com/user-attachments/assets/6c8b6ab5-5656-4a2c-aa50-d71cb3bbaf3f)


<br>

## 시스템 아키텍처

![GoldenStep 통합 시스템 및 AWS 배포 아키텍처](https://github.com/user-attachments/assets/2e09f04c-18b7-4908-b2d2-161eb91b1ed4)

단일 EC2에서 Docker Compose로 Nginx·Spring Boot·FastAPI를 실행하며, 백엔드는 비공개 RDS MySQL에 데이터를 저장합니다. S3는 분석용 자료, ECR은 컨테이너 이미지를 보관합니다. 브라우저는 NAVER Maps·TMAP, 백엔드는 NAVER 위치·주소 API와 연동합니다.

이미지의 CI/CD는 구성 기준이며 최신 실행 성공 여부는 별도로 확인해야 합니다.

| 구성 요소 | 역할 | 저장소 |
| --- | --- | --- |
| Frontend | 지도·입력·수색 결과·공유 화면 | [goldenstep-frontend](https://github.com/Hop-Radar/goldenstep-frontend) |
| Backend | API·세션·분석 중계·영속 관리 | 현재 저장소 |
| Algorithm | 보행 네트워크 기반 영역·장소 분석 | [goldenstep-algorithm](https://github.com/Hop-Radar/goldenstep-algorithm) |

<br>

## ERD
![GoldenStep ERD](https://github.com/user-attachments/assets/30cddbf9-ca5e-40bd-962d-b001e4532a7f)

원본 세션이 삭제되면 분석·결과·확인 기록은 연쇄 삭제됩니다. 공유 스냅샷의 원본 세션 연결은 `ON DELETE SET NULL`로 정의하여 공유 데이터의 유효기간을 별도로 관리합니다. 세부 스키마는 [schema.sql](src/main/resources/schema.sql)에서 확인할 수 있습니다.

<br>

## 💡 트러블슈팅

### 1. 공유 링크 조회 시 404 응답

- **문제**: 공유 링크로 수색 결과를 조회할 때 404 응답이 발생했습니다.
- **원인**: 컨트롤러의 경로 변수 바인딩이 누락되어 URL에 포함된 공유 토큰이 서비스로 정상 전달되지 않았습니다.
- **해결**: `@PathVariable` 바인딩을 복원하고, 공유 토큰으로 조회하는 요청 흐름을 확인했습니다.
- **결과**: 수정 후 사용자 로컬 환경에서 공유 결과의 정상 조회를 확인했습니다.

관련 코드: [SnapshotController](src/main/java/com/tjoeun/goldenstep/snapshot/controller/SnapshotController.java), [SnapshotControllerTest](src/test/java/com/tjoeun/goldenstep/snapshot/controller/SnapshotControllerTest.java)

### 2. 추천 장소 식별 코드 데이터 가공

- **문제**: 추천 장소명이 없거나 `BLDG-숫자`, `OSM-숫자` 같은 식별 코드로 저장되어 사용자가 어떤 장소인지 파악하기 어려웠습니다.
- **원인**: FastAPI가 원천 장소 데이터의 이름을 전달하면서 이름이 없는 경우 식별 코드가 그대로 반환되었고, 백엔드에 이를 표시용 장소명으로 보완하는 처리가 없었습니다.
- **해결**: `PlaceNameResolver`를 구현하여 정상적인 장소명은 유지하고, 이름이 없거나 식별 코드인 경우 좌표로 주소를 구한 뒤 NAVER Maps의 Geocoding API에 주소와 좌표를 전달해 건물명을 조회하도록 했습니다. 건물명을 얻지 못하면 주소를 사용하고, 주소도 없으면 좌표로 대체합니다.
- **결과**: 원천 데이터의 식별 코드 대신 장소명·주소·좌표를 우선 사용하도록 개선했습니다. 외부 조회가 실패해도 복구 가능한 오류는 주소로 대체하도록 처리했습니다.

관련 코드: [PlaceNameResolver](src/main/java/com/tjoeun/goldenstep/location/service/PlaceNameResolver.java), [ReverseGeocodingService](src/main/java/com/tjoeun/goldenstep/location/service/ReverseGeocodingService.java), [PlaceNameResolverTest](src/test/java/com/tjoeun/goldenstep/location/service/PlaceNameResolverTest.java)

### 3. CI에서 여러 컴파일 오류가 연속 발생

- **문제**: CI 빌드에서 다수의 컴파일·import 오류가 함께 표시되었습니다.
- **원인**: `package` 선언 앞의 오탈자로 소스 파일을 정상적으로 해석하지 못했습니다.
- **해결**: 첫 번째 컴파일 오류의 위치를 확인하고 오탈자를 수정했습니다.
- **결과**: 오류를 유발한 선언을 바로잡았습니다. 최신 전체 CI 성공 여부는 해당 실행 로그로 별도 확인합니다.

<br>

## 🛠️ 기술 스택

| 구분 | 기술 | 역할 |
| --- | --- | --- |
| Language | Java 21 | 백엔드 개발 언어 |
| Framework | Spring Boot 4.1.1, Spring MVC | REST API 및 애플리케이션 구성 |
| Persistence | Spring Data JPA, MySQL | 세션·분석 결과·공유 데이터 저장 |
| Validation | Jakarta Bean Validation | 요청 입력값 검증 |
| API Integration | Spring RestClient, FastAPI, NAVER API | 수색 분석 요청·위치 검색·주소 및 장소명 보완 |
| API Documentation | springdoc OpenAPI | API 명세 및 Swagger UI |
| Test | JUnit 5, Spring Boot Test | 단위·HTTP·DB 통합 테스트 |
| Build | Gradle Wrapper | 의존성 관리·테스트·실행 JAR 빌드 |
| Infrastructure | AWS EC2·RDS·S3·ECR·SSM·Secrets Manager, Docker Compose, Nginx | 서비스 실행·데이터 및 이미지 관리·HTTPS |
| Version Control & CI/CD | Git, GitHub, GitHub Actions | 소스 관리·검증·배포 자동화 |

<br>

## 🧑 Contributors

**홉레이더** · 개발 기간: 2026.09.21 ~ 2026.10.06

| 이름 | 역할 | 담당 |
| --- | --- | --- |
| [이재훈](https://github.com/HuniHuns) | 팀장 / Backend | 세션·분석 연동·결과·공유 API, AWS 배포 |
| [김동규](https://github.com/kdg311378-star) | Algorithm | 보행 네트워크·지형 계산·수색 분석 알고리즘 |
| [박철우](https://github.com/pcw940530) | Frontend | 입력·지도·결과·탐색보드·공유 화면 |

<br>

## 📖 설치 및 동작 문서

설치와 실행, 내부 동작 및 API는 Wiki 게시용 문서로 관리합니다. 아래 링크는 저장소에서 바로 읽을 수 있는 원고입니다.

- [Wiki 홈](https://github.com/Hop-Radar/goldenstep-backend/wiki)
- [설치 및 실행](https://github.com/Hop-Radar/goldenstep-backend/wiki/Installation)
- [분석·세션·공유 동작 방식](https://github.com/Hop-Radar/goldenstep-backend/wiki/How-It-Works)
- [API 사용 흐름](https://github.com/Hop-Radar/goldenstep-backend/wiki/API-Guide)
- [배포 및 검증](https://github.com/Hop-Radar/goldenstep-backend/wiki/Deployment)

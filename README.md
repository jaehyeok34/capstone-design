# CAP_2 프로젝트

이 문서는 프론트엔드와 백엔드 간의 API 사용을 정리한 개발자용 README입니다. 특히 프론트엔드에서 사용하는 fetch 호출(엔드포인트, 전송 데이터, 응답 형식, 호출 위치)을 중심으로 정리했습니다.

## 실행 방법 (개발)

1. 백엔드 시작

```bash
# backend 폴더로 이동
cd backend
# uvicorn으로 FastAPI 실행
uvicorn mdConverter.main:app --reload
```

- 기본 포트: `http://localhost:8000`

2. 프론트엔드 시작

```bash
# frontend 폴더로 이동
cd frontend
npm install      # (한 번만)
npm run dev
```

- 기본 포트: `http://localhost:5173` (Vite)


## API 목록 (프론트엔드에서 호출하는 엔드포인트)

아래는 프론트엔드에서 사용하는 주요 API의 정리입니다. 각 항목에 요청 형식과 응답 형식, 호출 위치(컴포넌트/파일)를 함께 기재했습니다.

> 베이스 URL: `http://localhost:8000`


### 1) POST /api/find-join-keys
- 목적: 두 파일을 전달하여 결합키 후보를 분석
- 호출 위치: `frontend/src/components/JoinModal.jsx` (analyzeJoinKeys)
- 요청
  - Method: POST
  - Content-Type: multipart/form-data
  - Form fields:
    - `files` (파일) — 두 개의 파일을 업로드 (키 이름은 `files`, 여러번 append)
- 예시 (curl)
```bash
curl -X POST "http://localhost:8000/api/find-join-keys" \
  -F "files=@dataA.csv" -F "files=@dataB.csv"
```
- 응답 (200 OK) — JSON
```json
{
  "join_key_candidates": [
    { "dataA_column": "id", "dataB_column": "user_id", "normalized_name":"id", "value_similarity_score": 0.5, "recommended": true }
  ],
  "dataA_candidates": [...],
  "dataB_candidates": [...],
  "total_common_keys": 1,
  "recommended_keys": [...],
  "file_names": { "dataA": "dataA.csv", "dataB": "dataB.csv" }
}
```


### 2) POST /api/join
- 목적: 결합(프로젝트 생성) 요청 — 파일 및 메타데이터 전송
- 호출 위치: `frontend/src/components/JoinModal.jsx` (handleSubmit)
- 요청
  - Method: POST
  - Content-Type: multipart/form-data
  - Form fields:
    - `projectName` (string)
    - `processingType` (string) — ex: `join`
    - `joinKeys` (string, optional) — JSON.stringify(joinKeys) 형태
    - `files` (파일) — 여러 파일(같은 키 이름 `files`에 여러번 append)
- 예시 (curl)
```bash
curl -X POST "http://localhost:8000/api/join" \
  -F "projectName=내프로젝트" \
  -F "processingType=join" \
  -F "joinKeys=[{...}]" \
  -F "files=@a.csv" -F "files=@b.csv"
```
- 응답 (200 OK)
```json
{
  "message": "프로젝트 '내프로젝트'가 성공적으로 등록되었습니다.",
  "projectId": "<uuid>",
  "savedFiles": ["a.csv","b.csv"],
  "projectDir": "./join_projects/<uuid>",
  "autoJoinKeys": 2,
  "joinKeysFound": [...]
}
```


### 3) GET /api/join-projects
- 목적: 저장된 결합 프로젝트 목록 조회
- 호출 위치: `frontend/src/pages/Analysis.jsx` (useEffect)
- 요청
  - Method: GET
  - Query: 없음
- 응답 (200 OK)
```json
{
  "projects": [
    {
      "id": "<uuid>",
      "projectName": "내프로젝트",
      "files": ["a.csv","b.csv"],
      "joinKeys": [...],
      "status": "진행 중", // 또는 "승인 완료", "반려", "분석 완료"
      "review": { "status": "pending|approved|rejected", ... },
      "createdAt": "2025-10-30T...",
      "outputFile": null
    }
  ]
}
```


### 4) GET /api/admin/join-requests
- 목적: 관리자용 결합 요청 목록 조회 (옵션: status 필터)
- 호출 위치: `frontend/src/pages/AdminDashboard.jsx` (load)
- 요청
  - Method: GET
  - Query params: `status` (optional) — `pending`, `approved`, `rejected`
- 응답
```json
{ "projects": [ ...same as /api/join-projects entries... ] }
```


### 5) PATCH /api/admin/join-requests/{project_id}
- 목적: 관리자 승인/반려 처리
- 호출 위치: `frontend/src/pages/AdminDashboard.jsx` (updateStatus)
- 요청
  - Method: PATCH
  - Headers: `Content-Type: application/json`
  - Body JSON:
```json
{ "reviewStatus": "approved" }
```
- 응답
```json
{ "message": "상태가 업데이트되었습니다.", "project": { ...updated project... } }
```


### 6) GET /api/admin/join-requests/{project_id}/result
- 목적: 결합 결과(다운로드용) — CSV 파일을 Blob으로 반환
- 호출 위치: `frontend/src/pages/AdminDashboard.jsx`, `frontend/src/pages/Analysis.jsx`, `frontend/src/components/ProjectDetail.jsx`
- 요청
  - Method: GET
- 응답
  - Content-Type: `text/csv`
  - Body: 파일 바이너리 (프론트엔드에서 `response.blob()`로 받아 a 태그로 다운로드)
- 비고: 현재는 더미 CSV를 반환하도록 구현되어 있음


### 7) GET /api/file-preview/{project_id}/{file_name}
- 목적: 프로젝트 내 파일의 미리보기(텍스트 형태) 반환
- 호출 위치: `frontend/src/components/ProjectDetail.jsx` (handleFilePreview)
- 요청
  - Method: GET
  - Path params: `project_id`, `file_name` (파일명은 `encodeURIComponent`로 인코딩하여 요청)
- 응답
```json
{ "fileName": "a.csv", "content": "미리보기 문자열...", "fileSize": 12345 }
```


### 8) POST /convert-and-download  (유틸)
- 목적: 파일을 변환해서 즉시 다운로드 (현재 프론트에 유틸 함수만 존재)
- 호출 위치: `frontend/src/components/downloadButton.jsx` (유틸 함수)
- 요청
  - Method: POST
  - Content-Type: multipart/form-data
  - Form fields: `file` (File), `format` (string)
- 응답
  - Blob 파일
- 비고: 현재 백엔드에서 실제로 이 엔드포인트가 구현되어 있는지 확인 필요 (없다면 404 발생)


## 운영/개발 노트
- 환경변수
  - 프론트에서 API 베이스 URL을 환경변수로 분리하는 것을 권장합니다.
    - 예: `VITE_API_BASE=http://localhost:8000`
    - 사용 예: `fetch(`${import.meta.env.VITE_API_BASE}/api/join` ... )`

- CORS
  - 백엔드에서 `allow_origins`에 `http://localhost:5173` 등이 지정되어 있어야 프론트에서 호출 가능

- 관리자 접근 제어
  - 현재 관리자 버튼은 프론트에서 숨겨진 클릭(5회 클릭)으로 노출됩니다. 서버 측 권한 검증은 없습니다. 실 서비스에서는 인증/인가(JWT 등)를 반드시 추가하세요.

- 에러 처리
  - 프론트에서 `response.ok` 체크 후 사용자에게 알림을 띄우도록 구현되어 있습니다. 서버 에러 메시지는 JSON에 `detail` 또는 `message`로 올 수 있으니 프론트에서 적절히 처리하세요.


## 추가 제안(옵션)
- Postman/Insomnia용 컬렉션 생성
- `convert-and-download` 엔드포인트가 필요하다면 백엔드에 구현
- 프론트의 베이스 URL을 환경변수로 전환
- 관리자 동작에 대한 서버의 권한 검사 추가


---

## 전체 API 스펙 (매개변수와 반환 형식)

아래는 프로젝트에서 제공하는 모든 엔드포인트와 요청/응답의 상세 스펙입니다.

- 베이스 URL: `http://localhost:8000`

1) POST /api/find-join-keys
 - Request: multipart/form-data, files x2
 - Response: 200 JSON
   - keys: join_key_candidates (list), dataA_candidates, dataB_candidates, total_common_keys:int, recommended_keys:list, file_names: {dataA, dataB}

2) POST /api/join
 - Request: multipart/form-data
   - projectName: string
   - processingType: string
   - joinKeys: string (optional, JSON string of join key candidates)
   - files: file[]
 - Response: 200 JSON
   - message, projectId, savedFiles, projectDir, autoJoinKeys:int, joinKeysFound:list

3) GET /api/join-projects
 - Request: none
 - Response: 200 JSON { projects: [ { id, projectName, files, joinKeys, status, review, createdAt, outputFile, ... } ] }

4) GET /api/admin/join-requests
 - Request: optional query param `status` (pending|approved|rejected)
 - Response: 200 JSON { projects: [...] }

5) PATCH /api/admin/join-requests/{project_id}
 - Request: JSON body { reviewStatus: 'approved'|'rejected', reviewer?:string, reason?:string }
 - Response: 200 JSON { message, project }

6) GET /api/admin/join-requests/{project_id}/result
 - Request: none
 - Response: 200 CSV file (text/csv)
 - Notes: 파일이 없으면 내부 결합 로직을 실행해 생성하고, 실패 시 더미 CSV로 폴백

7) POST /api/admin/join-requests/{project_id}/pseudonymize
 - Request: none
 - Response: 200 CSV file (text/csv) — 가명처리된 CSV
 - Notes: 결과가 없으면 내부적으로 결합을 먼저 수행 후 가명처리

8) GET /api/file-preview/{project_id}/{file_name}
 - Request: path params
 - Response: 200 JSON { fileName, content (string), fileSize }


## 백엔드 구조 변경: 엔드포인트와 처리 로직 분리

목표: 엔드포인트는 HTTP 계층만 담당하고, 실제 데이터 처리(결합, 가명처리, 결합키 탐색)는 재사용 가능한 함수로 분리했습니다.

위치는 `backend/mdConverter/processor.py` 입니다. 주요 공개 함수(호출 가능한 형태)는 다음과 같습니다.

- `find_join_keys_for_dataframes(df_a: pd.DataFrame, df_b: pd.DataFrame, consistency_threshold: float=30.0, min_unique_ratio: float=0.3) -> dict`
  - 설명: 두 데이터프레임에서 결합키 후보를 찾아 JSON-serializable dict를 반환합니다.

- `perform_join_for_project(project_id: str) -> str`
  - 설명: 프로젝트의 파일을 읽어 sha256 기반 해시로 결합을 수행하고, 생성된 CSV 파일의 경로(문자열)를 반환합니다.
  - 예외: 프로젝트/파일 없음 또는 키 부족 시 예외 발생

- `pseudonymize_project(project_id: str) -> str`
  - 설명: 프로젝트의 결합 결과를 가명처리하여 CSV로 저장한 뒤 파일 경로를 반환합니다. 내부에서 `perform_join_for_project`를 호출하여 결과를 보장합니다.

- 유틸 함수들
  - `_compute_hash_series(df, columns) -> pd.Series`
  - `_mask_string(s) -> str`
  - `_numeric_range(val) -> str`

이제 `backend/mdConverter/main.py`의 HTTP 핸들러들은 위 함수들을 호출하는 형태로 간결해졌습니다. 예를 들어 `/api/admin/join-requests/{id}/pseudonymize` 엔드포인트는 내부적으로 `processor.pseudonymize_project(id)`를 호출합니다.

이 구조의 장점:
- 로직을 다른 스크립트(예: 테스트 스크립트, CLI 도구)에서 직접 호출 가능
- 단위 테스트 작성이 쉬워짐
- 복잡한 처리(파일 IO, pandas 조작)를 별도 모듈로 분리하여 유지보수성 향상

원하시면 제가 다음 작업을 자동으로 수행해 드립니다:
- `processor` 함수들에 대한 유닛 테스트 추가 (pytest)
- `ProjectDetail` 페이지에도 가명처리 버튼 추가
- 결합/가명처리 흐름에 대한 간단한 통합 테스트(샘플 파일 사용)

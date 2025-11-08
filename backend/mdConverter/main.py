from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from pydantic import BaseModel
from typing import Optional
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, FileResponse
import os
import tempfile
import uuid
import pandas as pd
import json
from datetime import datetime
from mdConverter.x_to_md_converter import convert_table_to_markdown, convert_json_to_markdown
from mdConverter.md_to_x_converter import convert_markdown_to_format
import sys
import hashlib
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from . import processor

app = FastAPI()

# CORS 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:5174"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

OUTPUT_DIR = "./output"
os.makedirs(OUTPUT_DIR, exist_ok=True)
OUTPUT_MD_PATH = os.path.join(OUTPUT_DIR, "output.md")

# 결합 프로젝트 저장용 디렉토리
JOIN_PROJECTS_DIR = "./join_projects"
os.makedirs(JOIN_PROJECTS_DIR, exist_ok=True)

# 결합 프로젝트 저장용 (실제로는 DB를 사용해야 하지만 임시로 파일에 저장)
join_projects_db = []  # 프로세스 생명주기 동안만 유지, 파일과 동기화 보조용

def _load_projects_from_file() -> list:
    projects_file = os.path.join(JOIN_PROJECTS_DIR, "projects.json")
    if os.path.exists(projects_file):
        try:
            with open(projects_file, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"프로젝트 파일 로드 실패: {e}")
    return []

def _save_projects_to_file(projects: list) -> None:
    projects_file = os.path.join(JOIN_PROJECTS_DIR, "projects.json")
    try:
        with open(projects_file, "w", encoding="utf-8") as f:
            json.dump(projects, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"프로젝트 파일 저장 실패: {e}")

class ReviewPayload(BaseModel):
    reviewStatus: str  # approved | rejected
    reviewer: Optional[str] = None
    reason: Optional[str] = None


@app.post("/api/convert")
async def convert_file(file: UploadFile = File(...)):
    suffix = os.path.splitext(file.filename)[1]
    if suffix not in [".csv", ".xlsx", ".json"]:
        raise HTTPException(status_code=400, detail="지원하지 않는 파일 형식입니다.")

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    try:
        if suffix in [".csv", ".xlsx"]:
            markdown = convert_table_to_markdown(tmp_path)
        elif suffix == ".json":
            markdown = convert_json_to_markdown(tmp_path)
        else:
            raise HTTPException(status_code=400, detail="파일 변환 실패")

        # ✅ 변환된 md를 서버에 저장
        with open(OUTPUT_MD_PATH, "w", encoding="utf-8") as f:
            f.write(markdown)

    finally:
        os.remove(tmp_path)

    return JSONResponse(content={"markdown": markdown})


@app.get("/api/download")
def download_file():
    """서버에 저장된 md 파일 다운로드"""
    if not os.path.exists(OUTPUT_MD_PATH):
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(
        path=OUTPUT_MD_PATH, 
        filename="converted.md", 
        media_type="application/octet-stream")


@app.post("/api/export")
async def export_file(format: str = Form(...)):
    """
    서버에 저장된 output.md 파일을 다른 포맷으로 변환
    """
    if not os.path.exists(OUTPUT_MD_PATH):
        raise HTTPException(status_code=404, detail="Markdown 파일이 존재하지 않습니다.")

    if format not in ["md", "html", "pdf", "csv", "json", "docx", "xlsx"]:
        raise HTTPException(status_code=400, detail="지원하지 않는 포맷입니다.")

    try:
        # 저장된 md 파일을 읽어서 변환
        with open(OUTPUT_MD_PATH, "r", encoding="utf-8") as f:
            markdown = f.read()

        # md 포맷인 경우 원본 마크다운 파일 그대로 반환
        if format == "md":
            return FileResponse(path=OUTPUT_MD_PATH, filename="output.md", media_type="text/markdown")
        
        # 다른 포맷인 경우 변환
        output_path = convert_markdown_to_format(markdown, format)
        filename = os.path.basename(output_path)

        return FileResponse(path=output_path, filename=filename, media_type="application/octet-stream")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"결과 파일 생성 중 오류가 발생했습니다: {str(e)}")


@app.get("/")
async def read_root():
    return {"message": "Markdown Viewer API is running"}



# 결합 API

@app.post("/api/join")
async def join_files(
    projectName: str = Form(...),
    processingType: str = Form(...),
    joinKeys: str = Form(None),  # 프론트에서 전송된 결합키 정보 (JSON 문자열)
    files: list[UploadFile] = File(...)
):
    """
    여러 파일을 받아서 하나의 프로젝트로 결합
    """
    if not files:
        raise HTTPException(status_code=400, detail="파일이 업로드되지 않았습니다.")
    
    try:
        # 파일명 저장
        file_names = [file.filename for file in files]
        
        # 프론트에서 전송된 결합키 정보 사용 (있을 경우)
        if joinKeys:
            try:
                join_keys_data = json.loads(joinKeys)
                # 프론트에서 받은 결합키를 백엔드 형식으로 변환
                join_keys = []
                for key in join_keys_data:
                    join_keys.append({
                        'column': key.get('dataA_column', ''),
                        'matchedColumn': key.get('dataB_column', ''),
                        'similarity': key.get('value_similarity_score', 0),
                        'recommended': key.get('recommended', False)
                    })
            except json.JSONDecodeError:
                print(f"결합키 JSON 파싱 실패: {joinKeys}")
                join_keys = []
        else:
            join_keys = []
            
        # 프로젝트 ID 생성 (UUID 사용)
        project_id = str(uuid.uuid4())
        
        # 프로젝트별 디렉토리 생성
        project_dir = os.path.join(JOIN_PROJECTS_DIR, project_id)
        os.makedirs(project_dir, exist_ok=True)
        
        # 업로드된 파일들을 프로젝트 디렉토리에 저장
        saved_files = []
        for file in files:
            if file.filename:
                file_path = os.path.join(project_dir, file.filename)
                try:
                    # 파일 내용을 디스크에 저장
                    content = await file.read()
                    with open(file_path, "wb") as f:
                        f.write(content)
                    saved_files.append(file.filename)
                    print(f"파일 저장 완료: {file_path}")
                except Exception as e:
                    print(f"파일 저장 실패 {file.filename}: {e}")
                    continue
        
        # 자동으로 결합키 찾기 (파일이 2개 이상일 때만)
        auto_join_keys = []
        if len(saved_files) >= 2:
            try:
                print("=== 자동 결합키 분석 시작 ===")
                # 저장된 파일들을 데이터프레임으로 로드
                dataframes = []
                for file_name in saved_files[:2]:  # 처음 2개 파일만 분석
                    file_path = os.path.join(project_dir, file_name)
                    try:
                        if file_name.endswith('.csv'):
                            df = pd.read_csv(file_path)
                        elif file_name.endswith(('.xlsx', '.xls')):
                            df = pd.read_excel(file_path)
                        else:
                            continue
                        dataframes.append(df)
                        print(f"파일 로드 완료: {file_name}, 컬럼: {list(df.columns)}")
                    except Exception as e:
                        print(f"파일 로드 실패 {file_name}: {e}")
                        continue
                
                # 결합키 분석 실행
                if len(dataframes) >= 2:
                        result = find_join_keys_for_dataframes(dataframes[0], dataframes[1])
                        if result and 'join_key_candidates' in result:
                            auto_join_keys = result['join_key_candidates']
                            print(f"자동 결합키 발견: {len(auto_join_keys)}개")
                            for key in auto_join_keys:
                                print(f"  - {key}")
                        else:
                            print("자동 결합키를 찾지 못했습니다.")
                else:
                    print("결합키 분석을 위한 충분한 데이터가 없습니다.")
                    
            except Exception as e:
                print(f"자동 결합키 분석 실패: {e}")
                auto_join_keys = []
        
        # 프론트에서 받은 결합키가 있으면 우선 사용, 없으면 자동으로 찾은 결합키 사용
        final_join_keys = auto_join_keys if auto_join_keys else join_keys
        # final_join_keys = join_keys if join_keys else auto_join_keys
        
        # 프로젝트 정보 저장
        project_info = {
            "id": project_id,
            "projectName": projectName,
            "processingType": processingType,
            "files": saved_files,  # 실제 저장된 파일들만 포함
            "joinKeys": final_join_keys,
            "status": "진행 중",
            "review": {
                "status": "pending",  # pending | approved | rejected
                "reviewer": None,
                "reason": None,
                "decidedAt": None
            },
            "progress": 0,
            "createdAt": datetime.now().isoformat(),
            "outputFile": None
        }
        # 기존 파일에서 로드 후 append (덮어쓰기 방지)
        existing = _load_projects_from_file()
        # 프로세스 메모리 배열도 동기화 시도 (간단히 재할당)
        global join_projects_db
        join_projects_db = existing.copy()
        join_projects_db.append(project_info)
        # 파일로 저장
        os.makedirs(JOIN_PROJECTS_DIR, exist_ok=True)
        _save_projects_to_file(join_projects_db)
        return JSONResponse(content={
            "message": f"프로젝트 '{projectName}'가 성공적으로 등록되었습니다.",
            "projectId": project_info["id"],
            "savedFiles": saved_files,
            "projectDir": project_dir,
            "autoJoinKeys": len(final_join_keys),
            "joinKeysFound": final_join_keys
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"프로젝트 등록 중 오류가 발생했습니다: {str(e)}")


def find_join_keys_for_dataframes(df_a: pd.DataFrame, df_b: pd.DataFrame, 
                                 consistency_threshold: float = 30.0,  # 50 -> 30으로 더 낮춤
                                 min_unique_ratio: float = 0.3) -> dict:  # 0.5 -> 0.3으로 더 낮춤
    """
    두 데이터프레임에서 결합키 후보를 찾는 함수
    """
    return processor.find_join_keys_for_dataframes(df_a, df_b, consistency_threshold, min_unique_ratio)


def _compute_hash_series(df: pd.DataFrame, columns: list) -> pd.Series:
    """주어진 컬럼 목록의 값을 이어붙여 sha256 해시 시리즈를 반환한다.
    누락 컬럼은 빈 문자열로 취급한다.
    """
    return processor._compute_hash_series(df, columns)


def perform_join_for_project(project_id: str) -> str:
    """프로젝트의 파일을 읽어 지정된 결합키로 sha256 해시를 생성하고
    동일한 해시값을 가진 행들끼리 inner join하여 CSV를 생성한다.
    반환값: 생성된 결과 CSV의 절대 경로
    """
    return processor.perform_join_for_project(project_id)


@app.post("/api/find-join-keys")
async def find_join_keys(files: list[UploadFile] = File(...)):
    """
    두 개의 파일을 업로드받아 결합키 후보를 찾는 API
    """
    if len(files) != 2:
        raise HTTPException(status_code=400, detail="정확히 2개의 파일을 업로드해주세요.")
    
    try:
        dataframes = []
        file_names = []
        
        for file in files:
            # 파일 확장자 체크
            suffix = os.path.splitext(file.filename)[1].lower()
            if suffix not in [".csv", ".xlsx", ".xls", ".json"]:
                raise HTTPException(status_code=400, detail=f"지원하지 않는 파일 형식입니다: {file.filename}")
            
            # 임시 파일로 저장
            with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
                tmp.write(await file.read())
                tmp_path = tmp.name
            
            try:
                # 파일을 DataFrame으로 로드
                if suffix == ".csv":
                    df = pd.read_csv(tmp_path, encoding='utf-8')
                elif suffix in [".xlsx", ".xls"]:
                    df = pd.read_excel(tmp_path)
                elif suffix == ".json":
                    df = pd.read_json(tmp_path)
                
                dataframes.append(df)
                file_names.append(file.filename)
                
            finally:
                os.remove(tmp_path)
        
        # 결합키 분석 수행
        result = find_join_keys_for_dataframes(dataframes[0], dataframes[1])
        
        # 파일명 정보 추가
        result['file_names'] = {
            'dataA': file_names[0],
            'dataB': file_names[1]
        }
        
        return JSONResponse(content=result)
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"결합키 분석 중 오류가 발생했습니다: {str(e)}")


@app.get("/api/join-projects")
async def get_join_projects():
    """
    저장된 결합 프로젝트 목록을 조회하는 API
    """
    try:
        # JSON 파일에서 프로젝트 정보 로드
        projects = _load_projects_from_file()

        # 디스크에 실제 폴더가 없는 항목은 정리(사용자가 수동으로 폴더를 지운 경우 대비)
        filtered = []
        changed = False
        for p in projects:
            pid = p.get("id")
            if pid and os.path.exists(os.path.join(JOIN_PROJECTS_DIR, pid)):
                filtered.append(p)
            else:
                changed = True

        if changed:
            _save_projects_to_file(filtered)

        return JSONResponse(content={"projects": filtered})
            
    except Exception as e:
        print(f"프로젝트 목록 조회 오류: {e}")
        return JSONResponse(content={"projects": join_projects_db})


@app.delete("/api/join-projects/{project_id}")
async def delete_join_project(project_id: str):
    """프로젝트 메타데이터와 디렉토리를 함께 삭제"""
    try:
        projects = _load_projects_from_file()
        new_projects = [p for p in projects if p.get("id") != project_id]
        _save_projects_to_file(new_projects)

        # 프로젝트 디렉토리 삭제
        project_dir = os.path.join(JOIN_PROJECTS_DIR, project_id)
        if os.path.exists(project_dir):
            import shutil
            shutil.rmtree(project_dir, ignore_errors=True)

        return JSONResponse(content={"ok": True, "deletedId": project_id})
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"삭제 중 오류가 발생했습니다: {str(e)}")


@app.get("/api/file-preview/{project_id}/{file_name}")
async def get_file_preview(project_id: str, file_name: str):
    """프로젝트의 특정 파일 미리보기 내용을 반환"""
    try:
        # 프로젝트 디렉토리 경로
        project_dir = os.path.join(JOIN_PROJECTS_DIR, project_id)
        
        if not os.path.exists(project_dir):
            raise HTTPException(status_code=404, detail="프로젝트를 찾을 수 없습니다.")
        
        # 파일 경로
        file_path = os.path.join(project_dir, file_name)
        
        if not os.path.exists(file_path):
            raise HTTPException(status_code=404, detail="파일을 찾을 수 없습니다.")
        
        # 파일 확장자에 따른 처리
        file_extension = os.path.splitext(file_name)[1].lower()
        
        try:
            if file_extension == '.csv':
                # CSV 파일 처리
                df = pd.read_csv(file_path, encoding='utf-8')
                # 처음 10행만 미리보기
                preview_df = df.head(10)
                content = preview_df.to_string(index=False)
                
            elif file_extension in ['.xlsx', '.xls']:
                # Excel 파일 처리
                df = pd.read_excel(file_path)
                # 처음 10행만 미리보기
                preview_df = df.head(10)
                content = preview_df.to_string(index=False)
                
            elif file_extension == '.json':
                # JSON 파일 처리
                with open(file_path, 'r', encoding='utf-8') as f:
                    json_data = json.load(f)
                content = json.dumps(json_data, ensure_ascii=False, indent=2)
                
            elif file_extension in ['.txt', '.md']:
                # 텍스트 파일 처리
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                # 너무 긴 경우 처음 1000자만 미리보기
                if len(content) > 1000:
                    content = content[:1000] + "\n\n... (파일이 길어서 일부만 표시됩니다)"
                    
            else:
                # 지원하지 않는 파일 형식
                content = f"파일 형식 '{file_extension}'은 미리보기를 지원하지 않습니다.\n지원 형식: CSV, Excel, JSON, TXT, MD"
                
        except UnicodeDecodeError:
            # 인코딩 문제가 있는 경우 다른 인코딩으로 시도
            try:
                if file_extension == '.csv':
                    df = pd.read_csv(file_path, encoding='cp949')
                    preview_df = df.head(10)
                    content = preview_df.to_string(index=False)
                else:
                    with open(file_path, 'r', encoding='cp949') as f:
                        content = f.read()
                        if len(content) > 1000:
                            content = content[:1000] + "\n\n... (파일이 길어서 일부만 표시됩니다)"
            except:
                content = "파일 인코딩 문제로 미리보기를 표시할 수 없습니다."
        
        except Exception as e:
            content = f"파일을 읽는 중 오류가 발생했습니다: {str(e)}"
        
        return JSONResponse(content={
            "fileName": file_name,
            "content": content,
            "fileSize": os.path.getsize(file_path) if os.path.exists(file_path) else 0
        })
        
    except HTTPException:
        raise
    except Exception as e:
        print(f"파일 미리보기 오류: {e}")
        raise HTTPException(status_code=500, detail="파일 미리보기 중 오류가 발생했습니다.")


@app.get("/")
async def read_root():
    return {"message": "Markdown Viewer API is running"}

# =============== Admin APIs ===============

@app.get("/api/admin/join-requests")
async def admin_list_join_requests(status: Optional[str] = None):
    """관리자용 결합 요청 목록 조회 (status 필터: pending/approved/rejected)"""
    projects = _load_projects_from_file()
    if status:
        filtered = [p for p in projects if p.get("review", {}).get("status") == status]
    else:
        filtered = projects
    # 최신 생성일 순으로 정렬
    filtered.sort(key=lambda p: p.get("createdAt", ""), reverse=True)
    return JSONResponse(content={"projects": filtered})


@app.patch("/api/admin/join-requests/{project_id}")
async def admin_update_join_request(project_id: str, payload: ReviewPayload):
    """관리자 승인/반려 처리"""
    if payload.reviewStatus not in ("approved", "rejected"):
        raise HTTPException(status_code=400, detail="reviewStatus는 'approved' 또는 'rejected' 여야 합니다.")

    projects = _load_projects_from_file()
    found = False
    for p in projects:
        if str(p.get("id")) == str(project_id):
            p.setdefault("review", {})
            p["review"]["status"] = payload.reviewStatus
            p["review"]["reviewer"] = payload.reviewer
            p["review"]["reason"] = payload.reason
            p["review"]["decidedAt"] = datetime.now().isoformat()
            # 처리 상태도 동기화(선택): 승인 시 '승인 완료', 반려 시 '반려'
            p["status"] = "승인 완료" if payload.reviewStatus == "approved" else "반려"
            found = True
            break

    if not found:
        raise HTTPException(status_code=404, detail="프로젝트를 찾을 수 없습니다.")

    _save_projects_to_file(projects)

    # 메모리 캐시도 갱신
    global join_projects_db
    join_projects_db = projects

    return JSONResponse(content={"message": "상태가 업데이트되었습니다.", "project": p})

##여기!!!! 결합로직!!!! 넣어주삼!
@app.get("/api/admin/join-requests/{project_id}/result")
async def get_join_result(project_id: str):
    """
    TODO:
    결합 결과 파일을 다운로드 (현재는 임시 더미 파일)
    추후 실제 결합 로직으로 교체 예정
    """
    try:
        projects = _load_projects_from_file()
        project = None
        for p in projects:
            if str(p.get("id")) == str(project_id):
                project = p
                break
        
        if not project:
            raise HTTPException(status_code=404, detail="프로젝트를 찾을 수 없습니다.")
        
        # 실제 결합 로직 실행: 이미 생성된 outputFile이 있으면 재사용,
        # 없으면 perform_join_for_project를 호출하여 생성
        result_path = project.get('outputFile')
        if result_path and os.path.exists(result_path):
            result_filename = os.path.basename(result_path)
        else:
            try:
                result_path = perform_join_for_project(project_id)
                result_filename = os.path.basename(result_path)
            except Exception as join_err:
                # 결합 실패 시 로그를 남기고, 기존 더미 결과로 폴백
                print(f"결합 실행 실패 ({project_id}): {join_err}")
                # 임시 결과 생성
                result_filename = f"{project.get('projectName', 'result')}_{project_id[:8]}_결합결과.csv"
                result_path = os.path.join(OUTPUT_DIR, result_filename)
                dummy_data = pd.DataFrame({
                    '이름': ['김철수', '이영희', '박민수', '최지영'],
                    '나이': [28, 32, 25, 29],
                    '직업': ['개발자', '디자이너', '학생', '마케터'],
                    '결합일시': [datetime.now().strftime("%Y-%m-%d %H:%M:%S")] * 4,
                    '프로젝트ID': [project_id[:8]] * 4
                })
                dummy_data.to_csv(result_path, index=False, encoding='utf-8-sig')

        return FileResponse(
            path=result_path,
            filename=result_filename,
            media_type='text/csv'
        )
        
    except HTTPException:
        raise
    except Exception as e:
        print(f"결과 파일 생성 오류: {e}")
        raise HTTPException(status_code=500, detail=f"결과 파일 생성 중 오류가 발생했습니다: {str(e)}")


def _mask_string(s: str) -> str:
    return processor._mask_string(s)


def _numeric_range(val) -> str:
    return processor._numeric_range(val)


@app.post("/api/admin/join-requests/{project_id}/pseudonymize")
async def admin_pseudonymize(project_id: str):
    """프로젝트 결과 파일에 대해 가명처리(masking)를 수행하고 가명처리된 CSV를 반환한다.
    기본적으로 프로젝트의 joinKeys로 지정된 컬럼들을 가명처리하며, 만약 joinKeys가 없으면
    모든 문자열/숫자 컬럼을 대상으로 처리한다.
    """
    try:
        pseudo_path = processor.pseudonymize_project(project_id)
        pseudo_filename = os.path.basename(pseudo_path)
        return FileResponse(path=pseudo_path, filename=pseudo_filename, media_type='text/csv')
    except HTTPException:
        raise
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail="프로젝트를 찾을 수 없습니다.")
    except Exception as e:
        print(f"가명처리 오류 (delegated): {e}")
        raise HTTPException(status_code=500, detail=f"가명처리 중 오류가 발생했습니다: {e}")


@app.get("/")
async def read_root():
    return {"message": "Markdown Viewer API is running"}

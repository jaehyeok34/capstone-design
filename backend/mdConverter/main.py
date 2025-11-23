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
from joinkey.analyze_pseudokey import analyze_table
from joinkey.cardinality_check import analyze_dataframe
from joinkey.join_key_finder import normalize_colname, standardize_columns

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
    try:
        print(f"=== 결합키 분석 시작 ===")
        print(f"일관성 임계값: {consistency_threshold}")
        print(f"고유성 임계값: {min_unique_ratio}")
        
        # 데이터프레임 기본 검증
        if df_a.empty or df_b.empty:
            print("❌ 빈 데이터프레임이 전달됨")
            return {'error': '빈 데이터프레임', 'join_key_candidates': [], 'dataA_candidates': [], 'dataB_candidates': [], 'total_common_keys': 0, 'recommended_keys': []}
        
        # joinkey 스크립트들을 직접 로드
        joinkey_path = os.path.join(os.path.dirname(__file__), '..', 'joinkey')
        sys.path.insert(0, joinkey_path)
        print(f"joinkey 경로: {joinkey_path}")
        
        try:
            from joinkey.analyze_pseudokey import analyze_table
            from joinkey.cardinality_check import analyze_dataframe
            from joinkey.join_key_finder import normalize_colname, standardize_columns
            print("✅ joinkey 모듈들 성공적으로 로드됨")
        except ImportError as e:
            print(f"❌ joinkey 모듈 로드 실패: {e}")
            return {'error': f'모듈 로드 실패: {e}', 'join_key_candidates': [], 'dataA_candidates': [], 'dataB_candidates': [], 'total_common_keys': 0, 'recommended_keys': []}
        
        # 1. 컬럼명 표준화
        print(f"=== 컬럼명 표준화 ===")
        print(f"DataA 원본 컬럼: {list(df_a.columns)}")
        print(f"DataB 원본 컬럼: {list(df_b.columns)}")
        
        df_a_std, mapping_a = standardize_columns(df_a)
        df_b_std, mapping_b = standardize_columns(df_b)
        
        print(f"DataA 표준화 매핑:")
        for orig, std in mapping_a.items():
            print(f"  '{orig}' -> '{std}'")
        
        print(f"DataB 표준화 매핑:")
        for orig, std in mapping_b.items():
            print(f"  '{orig}' -> '{std}'")
        
        # 2. dataA 일관성 및 고유성 검사
        candidates_a = []
        consistency_results_a = analyze_table(df_a)
        print(f"=== DataA 일관성 분석 결과 ===")
        print(f"DataA 컬럼 수: {len(df_a.columns)}")
        print(f"DataA 행 수: {len(df_a)}")
        print(f"DataA 컬럼 목록: {list(df_a.columns)}")
        print(f"일관성 분석 결과 키들: {list(consistency_results_a.keys())}")
        
        if 'candidates' in consistency_results_a:
            print(f"후보 컬럼 수: {len(consistency_results_a['candidates'])}")
        else:
            print("❌ 'candidates' 키가 결과에 없습니다!")
            print(f"실제 결과: {consistency_results_a}")
            
        if 'candidates' in consistency_results_a and 'columns' in consistency_results_a:
            for col_name in consistency_results_a['candidates']:
                if col_name in consistency_results_a['columns']:
                    col_info = consistency_results_a['columns'][col_name]
                    final_score = col_info.get('score', 0)
                    print(f"  컬럼 '{col_name}': 일관성 점수 {final_score:.2f}")
                else:
                    print(f"  컬럼 '{col_name}': 정보 없음")
                    continue
                
                if final_score >= consistency_threshold:
                    if col_name in df_a.columns:
                        # 고유성 검사 (더 완화된 조건)
                        unique_ratio = df_a[col_name].nunique() / len(df_a)
                        print(f"    -> 고유성 비율: {unique_ratio:.3f} (기준: {min_unique_ratio})")
                        
                        # 고유성 검사를 더 관대하게 - 최소 조건만 확인
                        is_potential_key = (
                            unique_ratio >= min_unique_ratio or  # 원본 조건
                            df_a[col_name].nunique() >= 2 or     # 최소 2개 이상 고유값
                            col_name in ['계좌번호', '주민번호', '계좌', '주민등록번호', 'id', 'ID', 'Id']  # 명시적 키 컬럼명
                        )
                        
                        if is_potential_key:
                            normalized_name = normalize_colname(col_name)
                            print(f"    -> ✅ 결합키 후보로 선정!")
                            print(f"       원본명: '{col_name}' -> 표준명: '{normalized_name}'")
                            candidates_a.append({
                                'original_name': col_name,
                                'normalized_name': normalized_name,
                                'consistency_score': final_score,
                                'unique_ratio': unique_ratio
                            })
                        else:
                            print(f"    -> ❌ 고유성 부족으로 제외")
                else:
                    print(f"    -> ❌ 일관성 점수 부족으로 제외 (기준: {consistency_threshold})")
        
        print(f"\n=== DataB 일관성 분석 결과 ===")
        print(f"DataB 컬럼 수: {len(df_b.columns)}")
        print(f"DataB 행 수: {len(df_b)}")
        print(f"DataB 컬럼 목록: {list(df_b.columns)}")
        
        # 3. dataB 일관성 및 고유성 검사
        candidates_b = []
        consistency_results_b = analyze_table(df_b)
        print(f"일관성 분석 결과 키들: {list(consistency_results_b.keys())}")
        
        if 'candidates' in consistency_results_b:
            print(f"후보 컬럼 수: {len(consistency_results_b['candidates'])}")
        else:
            print("❌ 'candidates' 키가 결과에 없습니다!")
            print(f"실제 결과: {consistency_results_b}")
            
        if 'candidates' in consistency_results_b and 'columns' in consistency_results_b:
            for col_name in consistency_results_b['candidates']:
                if col_name in consistency_results_b['columns']:
                    col_info = consistency_results_b['columns'][col_name]
                    final_score = col_info.get('score', 0)
                    print(f"  컬럼 '{col_name}': 일관성 점수 {final_score:.2f}")
                else:
                    print(f"  컬럼 '{col_name}': 정보 없음")
                    continue
                
                if final_score >= consistency_threshold:
                    if col_name in df_b.columns:
                        # 고유성 검사 (더 완화된 조건)
                        unique_ratio = df_b[col_name].nunique() / len(df_b)
                        print(f"    -> 고유성 비율: {unique_ratio:.3f} (기준: {min_unique_ratio})")
                        
                        # 고유성 검사를 더 관대하게 - 최소 조건만 확인
                        is_potential_key = (
                            unique_ratio >= min_unique_ratio or  # 원본 조건
                            df_b[col_name].nunique() >= 2 or     # 최소 2개 이상 고유값
                            col_name in ['계좌번호', '주민번호', '계좌', '주민등록번호', 'id', 'ID', 'Id']  # 명시적 키 컬럼명
                        )
                        
                        if is_potential_key:
                            normalized_name = normalize_colname(col_name)
                            print(f"    -> ✅ 결합키 후보로 선정! (정규화명: '{normalized_name}')")
                            candidates_b.append({
                                'original_name': col_name,
                                'normalized_name': normalized_name,
                                'consistency_score': final_score,
                                'unique_ratio': unique_ratio
                            })
                        else:
                            print(f"    -> ❌ 고유성 부족으로 제외")
                else:
                    print(f"    -> ❌ 일관성 점수 부족으로 제외 (기준: {consistency_threshold})")
        
        print(f"\n=== 공통 결합키 매칭 ===")
        print(f"DataA 후보: {len(candidates_a)}개")
        print(f"DataB 후보: {len(candidates_b)}개")
        
        # 4. 공통 결합키 후보 찾기 (정규화된 컬럼명으로 매칭)
        common_keys = []
        for cand_a in candidates_a:
            for cand_b in candidates_b:
                if cand_a['normalized_name'] == cand_b['normalized_name']:
                    print(f"\n매칭 발견: '{cand_a['original_name']}' ↔ '{cand_b['original_name']}'")
                    print(f"  정규화명: '{cand_a['normalized_name']}')")
                    # 실제 데이터 값들의 유사성도 체크
                    sample_values_a = set(str(v) for v in df_a[cand_a['original_name']].dropna().head(100))
                    sample_values_b = set(str(v) for v in df_b[cand_b['original_name']].dropna().head(100))
                    
                    intersection = sample_values_a.intersection(sample_values_b)
                    print(f"  데이터 유사성 분석:")
                    print(f"    DataA 샘플값 수: {len(sample_values_a)}")
                    print(f"    DataB 샘플값 수: {len(sample_values_b)}")
                    print(f"    공통값 수: {len(intersection)}")
                    
                    # 공통값이 없어도 같은 의미 컬럼이면 결합키로 간주
                    if len(intersection) > 0 or cand_a['normalized_name'] in ['account', 'resident_id', 'name']:
                        if len(intersection) > 0:
                            similarity_score = len(intersection) / min(len(sample_values_a), len(sample_values_b))
                            print(f"    유사도 점수: {similarity_score:.3f}")
                            print(f"    공통값 예시: {list(intersection)[:5]}")
                        else:
                            # 공통값이 없어도 표준화된 컬럼명이 같으면 의미적 유사도 부여
                            similarity_score = 0.8 if cand_a['normalized_name'] in ['account', 'resident_id', 'name'] else 0.1
                            print(f"    의미적 유사도 점수: {similarity_score:.3f} (표준명 매칭)")
                        
                        # 추천 조건을 더 완화
                        is_recommended = (
                            similarity_score > 0.05 or  # 유사도 기준을 0.1 -> 0.05로 낮춤
                            cand_a['normalized_name'] in ['account', 'resident_id', 'name', 'phone', 'gender', 'address']  # 주요 컬럼은 무조건 추천
                        )
                        print(f"    추천 여부: {'✅ 추천' if is_recommended else '❌ 비추천'}")
                        print(f"      유사도 조건: {similarity_score:.3f} > 0.1 = {'✅' if similarity_score > 0.1 else '❌'}")
                        print(f"      고유성 조건: min({cand_a['unique_ratio']:.3f}, {cand_b['unique_ratio']:.3f}) > {min_unique_ratio} = {'✅' if min(cand_a['unique_ratio'], cand_b['unique_ratio']) > min_unique_ratio else '❌'}")
                        
                        common_keys.append({
                            'dataA_column': cand_a['original_name'],
                            'dataB_column': cand_b['original_name'],
                            'normalized_name': cand_a['normalized_name'],
                            'dataA_consistency_score': cand_a['consistency_score'],
                            'dataB_consistency_score': cand_b['consistency_score'],
                            'dataA_unique_ratio': cand_a['unique_ratio'],
                            'dataB_unique_ratio': cand_b['unique_ratio'],
                            'value_similarity_score': similarity_score,
                            'recommended': is_recommended
                        })
                    else:
                        print(f"    공통값이 없어서 결합키에서 제외")
        
        # 추천 점수로 정렬
        common_keys.sort(key=lambda x: (x['recommended'], x['value_similarity_score']), reverse=True)
        
        print(f"\n=== 최종 결과 ===")
        print(f"총 공통 결합키 후보: {len(common_keys)}개")
        recommended_keys = [k for k in common_keys if k['recommended']]
        print(f"추천 결합키: {len(recommended_keys)}개")
        
        for i, key in enumerate(common_keys, 1):
            status = "🔥 추천" if key['recommended'] else "⚠️  일반"
            print(f"{i}. {status}: {key['dataA_column']} ↔ {key['dataB_column']} (유사도: {key['value_similarity_score']:.3f})")
        
        return {
            'join_key_candidates': common_keys,
            'dataA_candidates': candidates_a,
            'dataB_candidates': candidates_b,
            'total_common_keys': len(common_keys),
            'recommended_keys': recommended_keys
        }
        
    except Exception as e:
        return {
            'error': f"결합키 분석 중 오류 발생: {str(e)}",
            'join_key_candidates': [],
            'dataA_candidates': [],
            'dataB_candidates': [],
            'total_common_keys': 0,
            'recommended_keys': []
        }


def _compute_hash_series(df: pd.DataFrame, columns: list) -> pd.Series:
    """주어진 컬럼 목록의 값을 이어붙여 sha256 해시 시리즈를 반환한다.
    누락 컬럼은 빈 문자열로 취급한다.
    """
    # 안전하게 문자열로 변환하고 결측치는 빈 문자열로 대체
    parts = []
    for col in columns:
        if col in df.columns:
            # 결측치 대체, 문자열 변환
            s = df[col].fillna("").astype(str)
        else:
            # 컬럼이 없으면 빈 문자열 시리즈 생성
            s = pd.Series([""] * len(df), index=df.index)
        parts.append(s)

    # 모든 파트를 행단위로 합쳐서 해시 입력 문자열 생성
    concat = parts[0].astype(str)
    for p in parts[1:]:
        concat = concat.str.cat(p.astype(str))

    # sha256 해시 계산
    return concat.apply(lambda x: hashlib.sha256(x.encode('utf-8')).hexdigest())


def perform_join_for_project(project_id: str) -> str:
    """프로젝트의 파일을 읽어 지정된 결합키로 sha256 해시를 생성하고
    동일한 해시값을 가진 행들끼리 inner join하여 CSV를 생성한다.
    반환값: 생성된 결과 CSV의 절대 경로
    """
    projects = _load_projects_from_file()
    project = next((p for p in projects if str(p.get('id')) == str(project_id)), None)
    if not project:
        raise FileNotFoundError("프로젝트를 찾을 수 없습니다.")

    project_dir = os.path.join(JOIN_PROJECTS_DIR, project_id)
    if not os.path.exists(project_dir):
        raise FileNotFoundError("프로젝트 디렉토리가 존재하지 않습니다.")

    files = project.get('files', [])
    if len(files) < 2:
        raise ValueError("결합하려면 최소 2개 이상의 파일이 필요합니다.")

    # 현재는 처음 두 파일만 사용하여 결합 수행
    file_a = os.path.join(project_dir, files[0])
    file_b = os.path.join(project_dir, files[1])

    if not os.path.exists(file_a) or not os.path.exists(file_b):
        raise FileNotFoundError("입력 파일을 찾을 수 없습니다.")

    # 파일 로드 (CSV/Excel/JSON 지원)
    def _load_file(path):
        # Try multiple formats/encodings to be robust against varied input files
        if path.endswith('.csv'):
            # Try utf-8 first, then cp949 (common on Korean Windows)
            try:
                return pd.read_csv(path, encoding='utf-8')
            except Exception:
                try:
                    return pd.read_csv(path, encoding='cp949')
                except Exception as e:
                    raise RuntimeError(f"CSV 파일 로드 실패 ({path}): {e}")
        elif path.endswith(('.xlsx', '.xls')):
            try:
                return pd.read_excel(path)
            except Exception as e:
                raise RuntimeError(f"Excel 파일 로드 실패 ({path}): {e}")
        elif path.endswith('.json'):
            try:
                return pd.read_json(path)
            except Exception as e:
                raise RuntimeError(f"JSON 파일 로드 실패 ({path}): {e}")
        else:
            # 시도: CSV로 읽기
            try:
                return pd.read_csv(path, encoding='utf-8')
            except Exception:
                try:
                    return pd.read_csv(path, encoding='cp949')
                except Exception as e:
                    raise RuntimeError(f"파일 로드 실패 ({path}): {e}")

    df_a = _load_file(file_a)
    df_b = _load_file(file_b)

    # project['joinKeys']의 형식을 정리하여 컬럼 목록을 만듦
    join_keys = project.get('joinKeys') or []
    if not join_keys:
        # 자동으로 키 찾기 (추천키 사용)
        result = find_join_keys_for_dataframes(df_a, df_b)
        candidates = result.get('recommended_keys') or result.get('join_key_candidates', [])
        join_keys = candidates

    # join_keys는 [{ 'column'/'dataA_column': ..., 'matchedColumn'/'dataB_column': ... }, ...]
    cols_a = []
    cols_b = []
    for k in join_keys:
        a_col = k.get('column') or k.get('dataA_column') or k.get('original_name')
        b_col = k.get('matchedColumn') or k.get('dataB_column') or k.get('original_name')
        if a_col and b_col:
            cols_a.append(a_col)
            cols_b.append(b_col)

    if len(cols_a) == 0 or len(cols_b) == 0:
        raise ValueError("사용 가능한 결합키 컬럼이 없습니다.")

    # 해시 시리즈 생성
    hash_a = _compute_hash_series(df_a, cols_a)
    hash_b = _compute_hash_series(df_b, cols_b)

    df_a['_join_hash_'] = hash_a
    df_b['_join_hash_'] = hash_b

    # inner join on the hash
    merged = pd.merge(df_a, df_b, left_on='_join_hash_', right_on='_join_hash_', how='inner', suffixes=('_A', '_B'))

    # 결합키 컬럼 중복 제거: A 쪽 컬럼을 우선으로 사용하고, B 쪽 중복 컬럼은 제거
    # join 키 쌍(cols_a[i], cols_b[i])에 대해 처리
    for a_col, b_col in zip(cols_a, cols_b):
        # 가능한 컬럼 이름 후보들
        a_candidates = []
        b_candidates = []
        # A 쪽 후보: 원본명, 또는 원본명 + suffix
        if a_col in merged.columns:
            a_candidates.append(a_col)
        if f"{a_col}_A" in merged.columns:
            a_candidates.append(f"{a_col}_A")
        if f"{a_col}_B" in merged.columns:
            a_candidates.append(f"{a_col}_B")

        # B 쪽 후보
        if b_col in merged.columns:
            b_candidates.append(b_col)
        if f"{b_col}_A" in merged.columns:
            b_candidates.append(f"{b_col}_A")
        if f"{b_col}_B" in merged.columns:
            b_candidates.append(f"{b_col}_B")

        # 선택 로직: 가능한 A 후보 중 첫번째를 우선 canonical로 사용
        canonical_name = a_col
        chosen_a = a_candidates[0] if a_candidates else None
        chosen_b = None
        if b_candidates:
            # prefer a candidate that is truly from B (with _B) if exists
            chosen_b = None
            for c in b_candidates:
                if c.endswith('_B') or c == b_col:
                    chosen_b = c
                    break
            if not chosen_b:
                chosen_b = b_candidates[0]

        # If A candidate exists, ensure final canonical column holds its values
        if chosen_a:
            # rename chosen_a to canonical_name if needed
            if chosen_a != canonical_name:
                merged[canonical_name] = merged[chosen_a]
                # drop the original chosen_a column after copying if it's suffixed
                if chosen_a.endswith('_A'):
                    merged.drop(columns=[chosen_a], inplace=True)
            # drop B column(s)
            if chosen_b and chosen_b in merged.columns:
                merged.drop(columns=[chosen_b], inplace=True)
        else:
            # A 컬럼 후보가 없다면 B 후보를 canonical로 사용
            if chosen_b:
                if chosen_b != canonical_name:
                    merged[canonical_name] = merged[chosen_b]
                    if chosen_b.endswith('_B'):
                        merged.drop(columns=[chosen_b], inplace=True)

    # 내부 조인을 위해 만든 해시 컬럼은 제거
    if '_join_hash_' in merged.columns:
        merged.drop(columns=['_join_hash_'], inplace=True)

    # 결과 파일 생성
    safe_name = project.get('projectName', 'result').replace(' ', '_')
    result_filename = f"{safe_name}_{project_id[:8]}_결합결과.csv"
    result_path = os.path.join(OUTPUT_DIR, result_filename)
    merged.to_csv(result_path, index=False, encoding='utf-8-sig')

    # 프로젝트 메타데이터 업데이트
    project['outputFile'] = result_path
    project['status'] = '분석 완료'
    project['progress'] = 100

    # 파일에 저장
    # replace project in projects list and save
    for i, p in enumerate(projects):
        if str(p.get('id')) == str(project_id):
            projects[i] = project
            break
    _save_projects_to_file(projects)

    return result_path


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
            # Attempt to perform real join; if it fails, surface the error (don't silently fall back)
            try:
                result_path = perform_join_for_project(project_id)
                result_filename = os.path.basename(result_path)
            except Exception as join_err:
                # Log and return error to caller for easier debugging
                print(f"결합 실행 실패 ({project_id}): {join_err}")
                raise HTTPException(status_code=500, detail=f"결합 실행 실패: {str(join_err)}")

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


@app.get("/")
async def read_root():
    return {"message": "Markdown Viewer API is running"}

from io import BytesIO
import json
from typing import List
from fastapi import APIRouter, File, Form, Response, UploadFile, HTTPException
from fastapi.responses import JSONResponse, StreamingResponse

from service.user_service import UserService


router = APIRouter()
service = UserService()

@router.post("/convert")
async def convert_file(file: UploadFile = File(...)):
    if (markdown := await service.convert_file(file)) is None:
        raise HTTPException(status_code=500, detail="파일 변환 실패")
    
    return JSONResponse(content={"markdown": markdown})

@router.post("/export")
async def export_file(format: str = Form(...), file_name: str = Form(...)):
    if (file_content := await service.export_file(format=format, file_name=file_name)) is None:
        raise HTTPException(status_code=500, detail="파일 내보내기 실패")
    
    return StreamingResponse(BytesIO(file_content), media_type="application/octet-stream")

@router.post("/find_candidate_columns")
async def find_candidate_columns(files: List[UploadFile] = File(...)):
    if (candidate_columns := await service.find_candidate_columns(files)) is None:
        raise HTTPException(status_code=500, detail="후보 컬럼 찾기 실패")
    
    print(candidate_columns)
    
    return JSONResponse(candidate_columns)

@router.post("/create_project")
async def create_project(projectName: str = Form(...), candidateColumns: str | None = Form(None), files: List[UploadFile] = File(...)):
    candidate_columns = json.loads(candidateColumns) if candidateColumns else {}
    
    if (project_id := await service.create_project(project_name=projectName, candidate_columns=candidate_columns, files=files)) is None:
        raise HTTPException(status_code=500, detail="프로젝트 생성 실패")
    
    return project_id

@router.get("/projects")
async def get_projects():
    if (json_projects := await service.get_projects()) is None:
        raise HTTPException(status_code=500, detail="프로젝트 목록 조회 실패")
    
    return Response(content=json_projects, media_type="application/json")

@router.get("/create_ci/{project_id}")
async def create_ci(project_id: str):
    if not await service.create_ci(project_id):
        raise HTTPException(status_code=500, detail="결합 연계정보 생성 실패")
    
    return True

@router.get("/project/{project_id}")
async def get_project_information(project_id: str):
    if (json_information := await service.get_project(project_id)) is None:
        raise HTTPException(status_code=500, detail="프로젝트 정보 조회 실패")
    
    return Response(content=json_information, media_type="application/json")

@router.get("/join/{project_id}")
async def join(project_id: str):
    if not await service.join(project_id):
        raise HTTPException(status_code=500, detail="결합 요청 실패")
    
    return True

@router.get("/result/{project_id}")
async def get_result(project_id: str):
    if (file_content := await service.get_result(project_id)) is None:
        raise HTTPException(status_code=500, detail="결과 파일 조회 실패")
    
    return StreamingResponse(BytesIO(file_content), media_type="application/octet-stream")
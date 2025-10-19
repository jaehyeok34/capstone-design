from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, FileResponse
import os
import tempfile
import uuid
from x_to_md_converter import convert_table_to_markdown, convert_json_to_markdown
from md_to_x_converter import convert_markdown_to_format

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
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/join")
async def join_files(
    projectName: str = Form(...),
    processingType: str = Form(...),
    files: list[UploadFile] = File(...)
):
    """
    여러 파일을 받아서 하나의 프로젝트로 결합
    """
    if not files:
        raise HTTPException(status_code=400, detail="파일이 업로드되지 않았습니다.")
    
    try:
        combined_content = f"# {projectName}\n\n"
        combined_content += f"**처리 유형:** {processingType}\n\n"
        combined_content += f"**총 파일 수:** {len(files)}\n\n"
        combined_content += "---\n\n"
        
        for i, file in enumerate(files, 1):
            file_content = await file.read()
            file_extension = os.path.splitext(file.filename)[1].lower()
            
            combined_content += f"## 파일 {i}: {file.filename}\n\n"
            
            # 파일 형식에 따른 처리
            if file_extension in ['.txt', '.md']:
                combined_content += file_content.decode('utf-8', errors='ignore')
            elif file_extension in ['.csv', '.xlsx']:
                # 테이블 파일인 경우 마크다운으로 변환
                with tempfile.NamedTemporaryFile(delete=False, suffix=file_extension) as tmp:
                    tmp.write(file_content)
                    tmp_path = tmp.name
                
                try:
                    markdown = convert_table_to_markdown(tmp_path)
                    combined_content += markdown
                finally:
                    os.remove(tmp_path)
            elif file_extension == '.json':
                # JSON 파일인 경우 마크다운으로 변환
                with tempfile.NamedTemporaryFile(delete=False, suffix=file_extension) as tmp:
                    tmp.write(file_content)
                    tmp_path = tmp.name
                
                try:
                    markdown = convert_json_to_markdown(tmp_path)
                    combined_content += markdown
                finally:
                    os.remove(tmp_path)
            else:
                combined_content += f"```\n{file_content.decode('utf-8', errors='ignore')}\n```"
            
            combined_content += "\n\n---\n\n"
        
        # 결합된 내용을 파일로 저장
        output_filename = f"{projectName}_combined.md"
        output_path = os.path.join(OUTPUT_DIR, output_filename)
        
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(combined_content)
        
        return JSONResponse(content={
            "message": f"프로젝트 '{projectName}'가 성공적으로 결합되었습니다.",
            "filename": output_filename,
            "file_count": len(files)
        })
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"파일 결합 중 오류가 발생했습니다: {str(e)}")


@app.get("/")
async def read_root():
    return {"message": "Markdown Viewer API is running"}


import json
from pathlib import Path
from py_client.message.message import Message
from utils import PROJECT_DIR, to_dataframe


def get_project_file_preview(message: Message):
    try:
        payload = json.loads(message.payload.decode("utf-8"))
        project_id: str = payload.get('projectId', '')
        file_name: str = payload.get('fileName', '')
    except Exception:
        return None
    
    if not project_id or not file_name:
        return None
    
    file_path = PROJECT_DIR / project_id / file_name # file name == full name(with extension)
    if not file_path.exists():
        return None
    
    suffix = file_path.suffix.lower()
    if suffix in ['.csv', 'xlsx', 'xls']:
        return preview_csv(file_path)
    
    elif suffix in ['json']:
        return preview_json(file_path)
    
    elif suffix in ['.txt', '.md']:
        return preview_text(file_path)

    else:
        return None
    
def preview_csv(file_path: Path):
    with open(file_path, 'rb') as f:
        df = to_dataframe(f.read())
        if df is None:
            return None
        
        return df.head(10).to_string(index=False).encode('utf-8')
    
def preview_json(file_path: Path):
    with open(file_path, 'r', encoding='utf-8') as f:
        try:
            return json.dumps(json.load(f), ensure_ascii=False, indent=4).encode('utf-8')
        
        except Exception as e:
            print('[error] preview_json():', e)
            return None
        
def preview_text(file_path: Path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return (f.read(1000) + '\n... (생략)').encode('utf-8')
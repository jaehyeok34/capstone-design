
from pathlib import Path

HOST, PORT = 'localhost', 3401
CLIENT_ID = 'api_gateway'

def is_valid_file(filename: str) -> bool:
    suffix = Path(filename).suffix
    
    return suffix in [".csv", ".xlsx", ".json"]

async def async_run(callable, *args):
    import asyncio
    loop = asyncio.get_running_loop()
    return await loop.run_in_executor(None, callable, *args)
    

def to_dataframe(datas: bytes):
    from io import BytesIO
    import pandas as pd

    buf = BytesIO(datas)
    try:
        buf.seek(0)
        return pd.read_csv(buf)
    
    except Exception as e:
        print(f"[debug] read_csv(): {e}")
        pass

    try:
        buf.seek(0)
        return pd.read_excel(buf)
    
    except Exception as e:
        print(f"[debug] read_excel(): {e}")
        pass

    try:
        buf.seek(0)
        return pd.read_json(buf)
    
    except Exception as e:
        print(f"[debug] read_json(): {e}")
        pass

    return None
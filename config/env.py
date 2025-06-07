import os
from dotenv import load_dotenv
from pydantic import BaseModel

if not os.getenv("ENV"):
    load_dotenv(dotenv_path='.env.dev')    

class Env(BaseModel):
    host: str = os.getenv('HOST')
    port: int = int(os.getenv('PORT'))
    service_name: str = os.getenv('SERVICE_NAME')
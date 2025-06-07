import os
from dotenv import load_dotenv
from pydantic import BaseModel


if not os.getenv('ENV'):
    load_dotenv(dotenv_path='.env.dev')

class Env(BaseModel):
    host: str = os.getenv('HOST')
    port: str = os.getenv('PORT')
    service_name: str = os.getenv('SERVICE_NAME')

    db_host: str = os.getenv('DB_HOST')
    db_port: str = os.getenv('DB_PORT')
    db_user: str = os.getenv('DB_USER')
    db_password: str = os.getenv('DB_PASSWORD')
    db_name: str = os.getenv('DB_NAME')

    sbert_model_path: str = os.getenv('SBERT_MODEL_PATH')
    threshold: float = float(os.getenv('THRESHOLD'))
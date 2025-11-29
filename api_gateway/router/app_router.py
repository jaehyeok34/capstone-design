
from fastapi import APIRouter
from service.app_service import AppService


router = APIRouter()
service = AppService()

@router.get("/")
def home():
    return service.say_hello()

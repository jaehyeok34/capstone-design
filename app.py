from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from router import app_router, user_router

prefix = "/api"
app = FastAPI()
app.include_router(app_router.router)
app.include_router(user_router.router, prefix=prefix)

# CORS 허용
app.add_middleware(
    CORSMiddleware,
    allow_origin_regex=r"http://localhost:\d+",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app="app:app",
        host="localhost",
        port=8000,
        reload=True
    )
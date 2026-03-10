from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from config import settings
from routers import detection
from services.yolo_service import yolo_service


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: pre-load the model
    print("Loading YOLOv11 model...")
    yolo_service.load_model()
    print(f"Model loaded: {'Custom DermaLens model' if yolo_service.is_custom else 'Pretrained YOLOv11 (demo)'}")
    yield
    # Shutdown cleanup (if needed)
    print("Shutting down DermaLens API...")


app = FastAPI(
    title="DermaLens API",
    description="AI-powered skin condition detection using YOLOv11",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(detection.router, prefix="/api/v1", tags=["Detection"])


@app.get("/", tags=["Health"])
def root():
    return {
        "app": "DermaLens API",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs",
    }


@app.get("/health", tags=["Health"])
def health_check():
    return {
        "status": "healthy",
        "model_loaded": yolo_service.model is not None,
        "model_type": "custom" if yolo_service.is_custom else "pretrained_demo",
    }

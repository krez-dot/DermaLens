#!/bin/bash
# DermaLens Backend Startup Script

echo "Starting DermaLens API..."

# Optional: activate virtual environment
# source venv/bin/activate

uvicorn main:app --host 0.0.0.0 --port 8000 --reload

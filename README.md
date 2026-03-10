# DermaLens

**AI-powered skin condition detection app using YOLOv11 + Flutter (Android)**

DermaLens lets users photograph their skin and receive instant AI-powered analysis detecting common dermatological conditions using the YOLOv11 object detection model.

---

## Architecture

```
DermaLens/
├── app/          # Flutter Android app
└── backend/      # Python FastAPI + YOLOv11 backend
```

---

## Backend (Python / FastAPI / YOLOv11)

### Requirements
- Python 3.10+
- pip

### Setup

```bash
cd backend

# Create virtual environment
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Copy and configure environment
cp .env.example .env
```

### Custom Model

Place your trained YOLOv11 weights at `backend/models/dermalens.pt`.

**Expected class IDs (match your training labels):**

| ID | Condition              | Severity |
|----|------------------------|----------|
| 0  | Acne                   | Mild     |
| 1  | Eczema                 | Moderate |
| 2  | Psoriasis              | Moderate |
| 3  | Rosacea                | Mild     |
| 4  | Melanoma               | Severe   |
| 5  | Basal Cell Carcinoma   | Severe   |
| 6  | Seborrheic Keratosis   | Mild     |
| 7  | Healthy Skin           | None     |

> If no custom model is found, it falls back to a pretrained `yolo11n.pt` (demo only).

### Run the Server

```bash
cd backend
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# Or use the startup script:
bash start.sh
```

API docs available at: `http://localhost:8000/docs`

### API Endpoints

| Method | Path              | Description                  |
|--------|-------------------|------------------------------|
| GET    | `/`               | API info                     |
| GET    | `/health`         | Health check + model status  |
| POST   | `/api/v1/detect`  | Analyze a skin image (JPEG/PNG/WebP, max 20MB) |

---

## Flutter App (Android)

### Requirements
- Flutter SDK 3.19+
- Android Studio or VS Code with Flutter plugin
- Android device or emulator (API 21+)

### Setup

```bash
cd app
flutter pub get
```

### Configure Backend URL

Edit `app/lib/utils/constants.dart`:

```dart
static const String baseUrl = 'http://YOUR_BACKEND_IP:8000';
```

- For **emulator** use `http://10.0.2.2:8000`
- For **physical device** use your machine's LAN IP (e.g. `http://192.168.1.x:8000`)

### Run

```bash
cd app
flutter run
```

### Build APK

```bash
cd app
flutter build apk --release
# Output: build/app/outputs/flutter-apk/app-release.apk
```

---

## App Screens

| Screen  | Description |
|---------|-------------|
| Splash  | Animated launch screen |
| Home    | Overview, CTA to scan or view history |
| Scan    | Pick image from camera or gallery, submit for analysis |
| Results | Annotated image with bounding boxes + condition cards |
| History | List of saved past scans (swipe to delete) |

---

## Detected Conditions

| Condition              | Severity |
|------------------------|----------|
| Acne                   | Mild     |
| Eczema                 | Moderate |
| Psoriasis              | Moderate |
| Rosacea                | Mild     |
| Melanoma               | Severe   |
| Basal Cell Carcinoma   | Severe   |
| Seborrheic Keratosis   | Mild     |
| Healthy Skin           | None     |

---

## Training Your Own Model

```python
from ultralytics import YOLO

model = YOLO("yolo11n.pt")   # start from pretrained
model.train(
    data="derma_dataset.yaml",
    epochs=100,
    imgsz=640,
    batch=16,
    project="runs/train",
    name="dermalens",
)
```

Copy `runs/train/dermalens/weights/best.pt` to `backend/models/dermalens.pt`.

---

## Tech Stack

| Layer    | Technology                |
|----------|---------------------------|
| Mobile   | Flutter (Android)         |
| AI Model | YOLOv11 (Ultralytics)     |
| Backend  | Python FastAPI + Uvicorn  |
| Storage  | SharedPreferences (local) |

---

## Disclaimer

> DermaLens is for **informational and educational purposes only**. It does not provide medical advice, diagnosis, or treatment. Always consult a qualified dermatologist for any skin concerns.

import io
import os
from typing import Optional

from PIL import Image

from config import settings


class YOLOService:
    def __init__(self):
        self.model = None
        self.is_custom = False

    def load_model(self):
        from ultralytics import YOLO

        model_path = settings.MODEL_PATH
        if os.path.exists(model_path):
            print(f"Loading custom model from: {model_path}")
            self.model = YOLO(model_path)
            self.is_custom = True
        else:
            print(
                "Custom model not found. Using pretrained yolo11n.pt for demo purposes."
            )
            print(
                f"To use a custom model, place your .pt file at: {model_path}"
            )
            self.model = YOLO("yolo11n.pt")
            self.is_custom = False

    def detect(self, image_bytes: bytes) -> dict:
        if self.model is None:
            self.load_model()

        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

        # Resize if image is too large
        max_size = settings.MAX_IMAGE_SIZE
        if image.width > max_size or image.height > max_size:
            image.thumbnail((max_size, max_size), Image.LANCZOS)

        original_width = image.width
        original_height = image.height

        results = self.model(image, conf=settings.CONFIDENCE_THRESHOLD, verbose=False)

        detections = []
        for result in results:
            if result.boxes is None:
                continue
            for box in result.boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                conf = float(box.conf[0])
                cls = int(box.cls[0])

                condition_info = settings.SKIN_CONDITIONS.get(
                    cls,
                    {
                        "name": f"Unknown (class {cls})",
                        "severity": "Unknown",
                        "description": "This condition is not in the current database.",
                        "recommendation": "Please consult a certified dermatologist.",
                    },
                )

                detections.append(
                    {
                        "bbox": {
                            "x1": round(x1, 2),
                            "y1": round(y1, 2),
                            "x2": round(x2, 2),
                            "y2": round(y2, 2),
                        },
                        "confidence": round(conf, 4),
                        "class_id": cls,
                        "condition": condition_info,
                        "image_width": original_width,
                        "image_height": original_height,
                    }
                )

        # Sort by confidence descending
        detections.sort(key=lambda d: d["confidence"], reverse=True)

        return {
            "detections": detections,
            "total_count": len(detections),
            "model_version": "YOLOv11",
            "is_custom_model": self.is_custom,
            "image_width": original_width,
            "image_height": original_height,
        }


# Singleton instance
yolo_service = YOLOService()

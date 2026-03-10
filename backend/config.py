import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    MODEL_PATH: str = os.getenv("MODEL_PATH", "models/dermalens.pt")
    CONFIDENCE_THRESHOLD: float = float(os.getenv("CONFIDENCE_THRESHOLD", "0.25"))
    MAX_IMAGE_SIZE: int = int(os.getenv("MAX_IMAGE_SIZE", "1280"))

    # Skin condition class mapping
    # Update these to match your custom trained model's class order
    SKIN_CONDITIONS: dict = {
        0: {
            "name": "Acne",
            "severity": "Mild",
            "description": "A common skin condition caused by clogged hair follicles with oil and dead skin cells. Usually treatable with topical medications.",
            "recommendation": "Keep skin clean, avoid touching face, consider salicylic acid or benzoyl peroxide treatments.",
        },
        1: {
            "name": "Eczema",
            "severity": "Moderate",
            "description": "An inflammatory skin condition causing itchy, red, and cracked skin. Often triggered by environmental factors or allergens.",
            "recommendation": "Use gentle moisturizers, avoid known triggers, consult a dermatologist for prescription creams.",
        },
        2: {
            "name": "Psoriasis",
            "severity": "Moderate",
            "description": "A chronic autoimmune condition causing rapid skin cell buildup, resulting in scaling and inflammation.",
            "recommendation": "Topical treatments, light therapy, and systemic medications can help manage symptoms. Consult a dermatologist.",
        },
        3: {
            "name": "Rosacea",
            "severity": "Mild",
            "description": "A chronic skin condition causing redness and visible blood vessels on the face, sometimes with acne-like bumps.",
            "recommendation": "Avoid triggers (sun, alcohol, spicy foods), use sunscreen daily, and consult a dermatologist for treatment options.",
        },
        4: {
            "name": "Melanoma",
            "severity": "Severe",
            "description": "The most serious type of skin cancer, developing in melanocytes. Early detection is critical for successful treatment.",
            "recommendation": "URGENT: Consult a dermatologist or oncologist immediately. Early diagnosis significantly improves outcomes.",
        },
        5: {
            "name": "Basal Cell Carcinoma",
            "severity": "Severe",
            "description": "The most common type of skin cancer, arising from basal cells. Rarely spreads but requires prompt treatment.",
            "recommendation": "Consult a dermatologist immediately for evaluation and treatment options including excision or topical therapy.",
        },
        6: {
            "name": "Seborrheic Keratosis",
            "severity": "Mild",
            "description": "A common, benign skin growth that appears as a waxy or scaly brown, black, or tan patch. Usually harmless.",
            "recommendation": "Generally no treatment needed. If irritated or cosmetically bothersome, a dermatologist can remove it.",
        },
        7: {
            "name": "Healthy Skin",
            "severity": "None",
            "description": "No significant skin abnormalities detected. Skin appears healthy in the analyzed region.",
            "recommendation": "Continue good skin care habits: use sunscreen, moisturize regularly, and do periodic self-exams.",
        },
    }


settings = Settings()

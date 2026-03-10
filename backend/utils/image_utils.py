from PIL import Image
import io


def bytes_to_pil(image_bytes: bytes) -> Image.Image:
    """Convert raw bytes to a PIL Image in RGB mode."""
    return Image.open(io.BytesIO(image_bytes)).convert("RGB")


def resize_to_fit(image: Image.Image, max_size: int) -> Image.Image:
    """Resize image so neither dimension exceeds max_size, preserving aspect ratio."""
    if image.width <= max_size and image.height <= max_size:
        return image
    image.thumbnail((max_size, max_size), Image.LANCZOS)
    return image

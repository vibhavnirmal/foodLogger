import json
from typing import Optional, Tuple
from urllib.error import HTTPError, URLError
from urllib.request import urlopen


OFF_PRODUCT_URL = "https://world.openfoodfacts.org/api/v2/product/{barcode}.json"


def _to_float(value) -> Optional[float]:
    if value in (None, ""):
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def fetch_product_from_open_food_facts(barcode: str) -> Tuple[Optional[dict], Optional[str]]:
    """
    Fetch product metadata from Open Food Facts.

    Returns:
    - (product_dict, None) when product is found
    - (None, None) when product is not found
    - (None, "off_unavailable") when OFF is unreachable/unavailable
    """
    url = OFF_PRODUCT_URL.format(barcode=barcode)

    try:
        with urlopen(url, timeout=6) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except (HTTPError, URLError, TimeoutError, ValueError):
        return None, "off_unavailable"

    if payload.get("status") != 1:
        return None, None

    raw_product = payload.get("product") or {}
    name = raw_product.get("product_name") or raw_product.get("product_name_en")
    if not name:
        return None, None

    nutriments = raw_product.get("nutriments") or {}

    product = {
        "barcode": barcode,
        "name": name,
        "brand": raw_product.get("brands"),
        "category": raw_product.get("categories"),
        "serving_size": raw_product.get("serving_size"),
        "kcal": _to_float(nutriments.get("energy-kcal_100g")),
        "protein": _to_float(nutriments.get("proteins_100g")),
        "carbs": _to_float(nutriments.get("carbohydrates_100g")),
        "fat": _to_float(nutriments.get("fat_100g")),
    }

    return product, None

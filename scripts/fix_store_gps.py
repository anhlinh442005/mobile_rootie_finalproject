#!/usr/bin/env python3
"""Fix implausible store GPS coordinates in rootie_stores.json."""
from __future__ import annotations

import hashlib
import json
import math
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STORES_PATH = ROOT / "app" / "src" / "main" / "assets" / "rootie_stores.json"
REPORT_PATH = ROOT / "gps_fix_report.json"

BOUNDS = {
    "Hồ Chí Minh": (10.35, 11.05, 106.30, 107.05),
    "Cần Thơ": (9.80, 10.40, 105.40, 106.10),
    "Tiền Giang": (10.00, 10.60, 106.00, 106.85),
    "Bà Rịa - Vũng Tàu": (10.20, 10.80, 106.90, 107.50),
    "Đồng Nai": (10.70, 11.20, 106.80, 107.40),
    "Biên Hòa": (10.70, 11.20, 106.80, 107.40),
    "Bình Dương": (10.80, 11.40, 106.40, 107.00),
    "Long An": (10.30, 10.90, 105.90, 106.70),
    "An Giang": (10.00, 11.20, 104.50, 105.60),
    "Nam Định": (19.80, 20.70, 105.90, 106.50),
    "Hà Nội": (20.90, 21.20, 105.70, 106.00),
    "Đà Nẵng": (15.90, 16.20, 107.90, 108.40),
}

DISTRICT_CENTROIDS = {
    ("Đồng Nai", "Thành phố Biên Hòa"): (10.9574, 106.8426),
    ("Đồng Nai", "Thị xã Long Khánh"): (10.9308, 107.2406),
    ("Đồng Nai", "Huyện Long Thành"): (10.7760, 106.9590),
    ("Đồng Nai", "Huyện Nhơn Trạch"): (10.7110, 106.9440),
    ("Bình Dương", "Thành phố Thủ Dầu Một"): (10.9804, 106.6519),
    ("Bình Dương", "Thành phố Dĩ An"): (10.9068, 106.7694),
    ("Bình Dương", "Thành phố Thuận An"): (10.9244, 106.7132),
    ("Bình Dương", "Thành phố Tân Uyên"): (11.0842, 106.7886),
    ("Bình Dương", "Thành phố Bến Cát"): (11.1522, 106.5925),
    ("Bình Dương", "Huyện Bến Cát"): (11.1522, 106.5925),
    ("Bình Dương", "Huyện Bàu Bàng"): (11.2650, 106.6200),
    ("Bình Dương", "Huyện Phú Giáo"): (11.3400, 106.7800),
    ("Bình Dương", "Huyện Dầu Tiếng"): (11.2800, 106.3600),
    ("Long An", "Thành phố Tân An"): (10.5396, 106.4137),
    ("Long An", "Thành phố Bến Lức"): (10.6510, 106.4950),
    ("Long An", "Huyện Đức Hòa"): (10.8830, 106.4380),
    ("Long An", "Huyện Cần Giuộc"): (10.6080, 106.6720),
    ("Long An", "Huyện Châu Thành"): (10.4550, 106.4920),
    ("Long An", "Huyện Thủ Thừa"): (10.6200, 106.5200),
    ("Long An", "Huyện Tân Trụ"): (10.5900, 106.3800),
}

PROVINCE_CENTROIDS = {
    "Đồng Nai": (10.9400, 106.9200),
    "Bình Dương": (11.0000, 106.6600),
    "Long An": (10.6000, 106.5000),
}


def normalize_province(province: str) -> str:
    return "Đồng Nai" if province == "Biên Hòa" else province


def plausible(province: str, lat: float, lng: float) -> bool:
    if lat == 0 and lng == 0:
        return False
    province = normalize_province(province)
    if province in BOUNDS:
        min_lat, max_lat, min_lng, max_lng = BOUNDS[province]
        return min_lat <= lat <= max_lat and min_lng <= lng <= max_lng
    return 8.0 <= lat <= 23.5 and 102.0 <= lng <= 110.0


def clean_address(addr: dict) -> str:
    province = normalize_province((addr.get("tinh_thanh") or "").strip())
    district = (addr.get("quan_huyen") or "").strip()
    street = f"{addr.get('so_nha', '')} {(addr.get('duong') or '').strip()}".strip()
    ward = re.sub(r"^Xã Xã ", "Xã ", (addr.get("phuong_xa") or "").strip())
    ward = re.sub(r"^TT\.\s*", "", ward)
    parts = [p for p in [street, ward, district, province, "Việt Nam"] if p]
    return ", ".join(parts)


def geocode(query: str) -> tuple[float, float] | None:
    params = urllib.parse.urlencode(
        {
            "q": query,
            "format": "json",
            "limit": 1,
            "countrycodes": "vn",
        }
    )
    url = f"https://nominatim.openstreetmap.org/search?{params}"
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "RootieStoreGPSFix/1.0 (veganbeauty-app)"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    if not data:
        return None
    return float(data[0]["lat"]), float(data[0]["lon"])


def stable_hash(text: str) -> int:
    return int(hashlib.sha256(text.encode("utf-8")).hexdigest()[:12], 16)


def offset_coords(base_lat: float, base_lng: float, key: str, radius_m: float = 1800.0) -> tuple[float, float]:
    h = stable_hash(key)
    angle = (h % 360) * math.pi / 180.0
    dist_m = 250.0 + (h % 1000) * (radius_m / 1000.0)
    dlat = (dist_m / 111_320.0) * math.sin(angle)
    dlng = (dist_m / (111_320.0 * math.cos(math.radians(base_lat)))) * math.cos(angle)
    return round(base_lat + dlat, 6), round(base_lng + dlng, 6)


def district_base(addr: dict, cache: dict[str, tuple[float, float]]) -> tuple[float, float, str]:
    province = normalize_province((addr.get("tinh_thanh") or "").strip())
    district = (addr.get("quan_huyen") or "").strip()
    key = f"{province}|{district}"

    if key in cache:
        return cache[key][0], cache[key][1], "district-cache"

    pair = (province, district)
    if pair in DISTRICT_CENTROIDS:
        lat, lng = DISTRICT_CENTROIDS[pair]
        cache[key] = (lat, lng, "district-table")
        return lat, lng, "district-table"

    query = f"{district}, {province}, Việt Nam"
    try:
        result = geocode(query)
        time.sleep(1.1)
    except Exception:
        result = None

    if result and plausible(province, result[0], result[1]):
        cache[key] = (result[0], result[1], "district-geocode")
        return result[0], result[1], "district-geocode"

    lat, lng = PROVINCE_CENTROIDS.get(province, (10.8, 106.7))
    cache[key] = (lat, lng, "province-fallback")
    return lat, lng, "province-fallback"


def resolve_coords(store: dict, district_cache: dict[str, tuple[float, float]]) -> tuple[float, float, str]:
    addr = store.get("dia_chi", {})
    province = normalize_province((addr.get("tinh_thanh") or "").strip())
    code = store.get("ma_cua_hang", "")
    full_query = clean_address(addr)

    try:
        street_result = geocode(full_query)
        time.sleep(1.1)
    except Exception:
        street_result = None

    if street_result and plausible(province, street_result[0], street_result[1]):
        return round(street_result[0], 6), round(street_result[1], 6), "street-geocode"

    base_lat, base_lng, base_method = district_base(addr, district_cache)
    lat, lng = offset_coords(base_lat, base_lng, full_query + "|" + code)
    return lat, lng, f"{base_method}+offset"


def main() -> None:
    with STORES_PATH.open(encoding="utf-8") as f:
        stores = json.load(f)

    district_cache: dict[str, tuple[float, float]] = {}
    updated = []

    for store in stores:
        addr = store.get("dia_chi", {})
        province = (addr.get("tinh_thanh") or "").strip()
        toa_do = store.get("toa_do") or {}
        lat = float(toa_do.get("lat", 0) or 0)
        lng = float(toa_do.get("lng", 0) or 0)

        if plausible(province, lat, lng):
            continue

        new_lat, new_lng, method = resolve_coords(store, district_cache)
        store.setdefault("toa_do", {})["lat"] = new_lat
        store.setdefault("toa_do", {})["lng"] = new_lng
        updated.append(
            {
                "ma": store.get("ma_cua_hang"),
                "old": [lat, lng],
                "new": [new_lat, new_lng],
                "method": method,
            }
        )
        print(store.get("ma_cua_hang"))

    with STORES_PATH.open("w", encoding="utf-8") as f:
        json.dump(stores, f, ensure_ascii=False, indent=2)
        f.write("\n")

    with REPORT_PATH.open("w", encoding="utf-8") as f:
        json.dump(updated, f, ensure_ascii=False, indent=2)

    print(f"UPDATED={len(updated)}")


if __name__ == "__main__":
    main()

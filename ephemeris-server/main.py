from datetime import datetime
from zoneinfo import ZoneInfo
import swisseph as swe
from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Ghan Sarvato Bhadra Ephemeris", version="0.6.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

BODIES = {
    "sun": swe.SUN, "moon": swe.MOON, "mercury": swe.MERCURY,
    "venus": swe.VENUS, "mars": swe.MARS, "jupiter": swe.JUPITER,
    "saturn": swe.SATURN, "rahu": swe.MEAN_NODE,
}

AYANAMSHA_MAP = {
    "lahiri": swe.SIDM_LAHIRI, "raman": swe.SIDM_RAMAN,
    "krishnamurti": swe.SIDM_KRISHNAMURTI,
    "fagan_bradley": swe.SIDM_FAGAN_BRADLEY,
}

@app.get("/health")
def health():
    return {"ok": True, "version": "0.6.0"}

@app.get("/v1/chart")
def chart(
    datetime_iso: str = Query(..., alias="datetime"),
    tz: str = "Asia/Kolkata",
    lat: float = 0.0, lon: float = 0.0,
    ayanamsha: str = "lahiri", nodes: str = "mean",
):
    dt = datetime.fromisoformat(datetime_iso.replace("Z", "+00:00"))
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=ZoneInfo(tz))
    utc = dt.astimezone(ZoneInfo("UTC"))

    jd = swe.julday(utc.year, utc.month, utc.day,
                    utc.hour + utc.minute/60 + utc.second/3600)

    sid_mode = AYANAMSHA_MAP.get(ayanamsha.lower(), swe.SIDM_LAHIRI)
    swe.set_sid_mode(sid_mode)

    flags = swe.FLG_SWIEPH | swe.FLG_SPEED | swe.FLG_SIDEREAL

    bodies = []
    for key, body in BODIES.items():
        xx, _ = swe.calc_ut(jd, body, flags)
        bodies.append({
            "key": key,
            "sidereal": {
                "longitude": xx[0] % 360,
                "latitude": xx[1],
                "speed": xx[3],
                "retrograde": xx[3] < 0,
            },
        })

    rahu = next(x for x in bodies if x["key"] == "rahu")
    bodies.append({
        "key": "ketu",
        "sidereal": {
            "longitude": (rahu["sidereal"]["longitude"] + 180) % 360,
            "latitude": 0.0,
            "speed": rahu["sidereal"]["speed"],
            "retrograde": True,
        },
    })

    # REAL LAGNA calculation
    cusps, ascmc = swe.houses_ex(jd, lat, lon, b'P', swe.FLG_SIDEREAL)
    lagna = {
        "sidereal": {
            "longitude": ascmc[0] % 360,
            "latitude": 0.0, "speed": 0.0, "retrograde": False,
        }
    }

    sun = next(x for x in bodies if x["key"] == "sun")["sidereal"]["longitude"]
    moon = next(x for x in bodies if x["key"] == "moon")["sidereal"]["longitude"]
    tithi = int(((moon - sun) % 360) // 12) + 1
    weekday = dt.weekday()

    return {
        "meta": {"ayanamsha": ayanamsha, "timezone": tz,
                 "latitude": lat, "longitude": lon},
        "tithi": tithi, "weekday": weekday,
        "lagna": lagna, "bodies": bodies,
    }

@app.get("/")
def root():
    return {"service": "Ghan Sarvato Bhadra Ephemeris", "docs": "/docs"}

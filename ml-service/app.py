import os
import numpy as np
import joblib
from typing import Dict
from fastapi import FastAPI
from pydantic import BaseModel

MODEL_PATH = os.path.join(os.path.dirname(__file__), "model", "model.pkl")

app = FastAPI(title="iot-ids-ml-service")
_bundle = None


def _load():
    global _bundle
    if _bundle is None:
        if not os.path.exists(MODEL_PATH):
            raise RuntimeError(f"Model not found at {MODEL_PATH}; run train.py first")
        _bundle = joblib.load(MODEL_PATH)
    return _bundle


class PredictRequest(BaseModel):
    features: Dict[str, float] = {}


@app.get("/health")
def health():
    return {"status": "UP"}


@app.post("/predict")
def predict(req: PredictRequest):
    bundle = _load()
    feature_names = bundle["feature_names"]
    model = bundle["model"]
    le = bundle["label_encoder"]
    # Align incoming features to the model's training vector; missing -> 0.0
    vector = np.array([[float(req.features.get(name, 0.0)) for name in feature_names]], dtype=float)
    proba = model.predict_proba(vector)[0]
    idx = int(np.argmax(proba))
    return {"attack_type": str(le.classes_[idx]), "score": float(proba[idx])}

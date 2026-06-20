import os
import numpy as np
import pandas as pd
import joblib
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
from xgboost import XGBClassifier

DATA_CSV = os.environ.get("RT_IOT2022_CSV", "data/RT_IOT2022.csv")
MODEL_PATH = os.path.join(os.path.dirname(__file__), "model", "model.pkl")

SYNTHETIC_FEATURES = [
    "fwd_pkts_tot", "bwd_pkts_tot", "flow_duration",
    "flow_pkts_per_sec", "fwd_data_pkts_tot", "fwd_pkts_per_sec",
]


def to_coarse(label) -> str:
    """Map an RT-IoT2022 raw label to a coarse AttackType name (mirrors Java AttackType.fromLabel)."""
    if label is None:
        return "OTHER"
    l = str(label).lower()
    if "normal" in l or "benign" in l:
        return "NORMAL"
    if "ddos" in l:
        return "DDOS"
    if "dos" in l:
        return "DOS"
    if "scan" in l:
        return "PORT_SCAN"
    if "brute" in l:
        return "BRUTE_FORCE"
    if "recon" in l or "nmap" in l:
        return "RECON"
    return "OTHER"


def synthetic_dataset(n_per_class=400, seed=42):
    """Class-conditional synthetic data so the classifier learns separable patterns."""
    rng = np.random.default_rng(seed)
    profiles = {
        "NORMAL":      dict(fwd_pkts_tot=50,   bwd_pkts_tot=40,  flow_duration=5,   flow_pkts_per_sec=10,   fwd_data_pkts_tot=30,   fwd_pkts_per_sec=8),
        "DOS":         dict(fwd_pkts_tot=5000, bwd_pkts_tot=20,  flow_duration=3,   flow_pkts_per_sec=2000, fwd_data_pkts_tot=4000, fwd_pkts_per_sec=1500),
        "DDOS":        dict(fwd_pkts_tot=9000, bwd_pkts_tot=15,  flow_duration=2,   flow_pkts_per_sec=4000, fwd_data_pkts_tot=8000, fwd_pkts_per_sec=3500),
        "PORT_SCAN":   dict(fwd_pkts_tot=300,  bwd_pkts_tot=5,   flow_duration=1,   flow_pkts_per_sec=300,  fwd_data_pkts_tot=10,   fwd_pkts_per_sec=250),
        "RECON":       dict(fwd_pkts_tot=120,  bwd_pkts_tot=110, flow_duration=120, flow_pkts_per_sec=2,    fwd_data_pkts_tot=80,   fwd_pkts_per_sec=1),
        "BRUTE_FORCE": dict(fwd_pkts_tot=600,  bwd_pkts_tot=590, flow_duration=40,  flow_pkts_per_sec=20,   fwd_data_pkts_tot=550,  fwd_pkts_per_sec=12),
        "OTHER":       dict(fwd_pkts_tot=200,  bwd_pkts_tot=200, flow_duration=20,  flow_pkts_per_sec=15,   fwd_data_pkts_tot=150,  fwd_pkts_per_sec=10),
    }
    rows, labels = [], []
    for cls, prof in profiles.items():
        for _ in range(n_per_class):
            rows.append({f: max(0.0, rng.normal(prof[f], prof[f] * 0.25 + 1)) for f in SYNTHETIC_FEATURES})
            labels.append(cls)
    df = pd.DataFrame(rows)
    df["__label__"] = labels
    return df, list(SYNTHETIC_FEATURES)


def load_real(path):
    df = pd.read_csv(path)
    label_col = next((c for c in df.columns if c.lower() in ("attack_type", "label")), None)
    if label_col is None:
        raise ValueError("No 'Attack_type'/'label' column found in CSV")
    y = df[label_col].map(to_coarse)
    drop_like = {"attack_type", "label", "proto", "service", "id.orig_p", "id.resp_p"}
    feature_cols = [c for c in df.columns
                    if c.lower() not in drop_like and pd.api.types.is_numeric_dtype(df[c])]
    out = df[feature_cols].fillna(0.0).copy()
    out["__label__"] = y.values
    return out, feature_cols


def build_dataset():
    if os.path.exists(DATA_CSV):
        print(f"Training on real dataset: {DATA_CSV}")
        return load_real(DATA_CSV)
    print("Real dataset not found; generating synthetic labeled data")
    return synthetic_dataset()


def train(df, feature_names):
    le = LabelEncoder().fit(df["__label__"].values)
    X = df[feature_names].astype(float).values
    y = le.transform(df["__label__"].values)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
    model = XGBClassifier(
        n_estimators=200, max_depth=6, learning_rate=0.1, subsample=0.9,
        objective="multi:softprob", num_class=len(le.classes_),
        eval_metric="mlogloss", tree_method="hist",
    )
    model.fit(X_tr, y_tr)
    print(classification_report(y_te, model.predict(X_te), target_names=le.classes_, zero_division=0))
    return model, le


def save(model, le, feature_names):
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    joblib.dump({"model": model, "label_encoder": le, "feature_names": feature_names}, MODEL_PATH)
    print(f"Saved model to {MODEL_PATH} with {len(feature_names)} features")


def main():
    df, feature_names = build_dataset()
    model, le = train(df, feature_names)
    save(model, le, feature_names)


if __name__ == "__main__":
    main()

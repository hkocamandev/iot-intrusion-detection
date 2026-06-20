import os
import sys
import joblib

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import train as train_mod  # noqa: E402


def test_train_produces_usable_model(tmp_path, monkeypatch):
    # Force synthetic path and a temp model location
    monkeypatch.setattr(train_mod, "DATA_CSV", str(tmp_path / "nope.csv"))
    monkeypatch.setattr(train_mod, "MODEL_PATH", str(tmp_path / "model" / "model.pkl"))

    train_mod.main()

    bundle = joblib.load(train_mod.MODEL_PATH)
    assert set(bundle.keys()) == {"model", "label_encoder", "feature_names"}
    assert "fwd_pkts_tot" in bundle["feature_names"]


def test_predict_returns_attack_type_and_score(tmp_path, monkeypatch):
    import importlib
    monkeypatch.setattr(train_mod, "DATA_CSV", str(tmp_path / "nope.csv"))
    model_path = str(tmp_path / "model" / "model.pkl")
    monkeypatch.setattr(train_mod, "MODEL_PATH", model_path)
    train_mod.main()

    import app as app_mod
    importlib.reload(app_mod)
    monkeypatch.setattr(app_mod, "MODEL_PATH", model_path)
    app_mod._bundle = None  # force reload of the temp model

    from fastapi.testclient import TestClient
    client = TestClient(app_mod.app)

    # DOS-like flow: very high forward packet counts
    resp = client.post("/predict", json={"features": {"fwd_pkts_tot": 6000, "flow_pkts_per_sec": 2500, "fwd_data_pkts_tot": 5000}})
    assert resp.status_code == 200
    body = resp.json()
    assert body["attack_type"] in {"DOS", "DDOS", "NORMAL", "PORT_SCAN", "RECON", "BRUTE_FORCE", "OTHER"}
    assert 0.0 <= body["score"] <= 1.0

    # missing features must not error (aligned to 0.0)
    assert client.post("/predict", json={"features": {}}).status_code == 200
    assert client.get("/health").json() == {"status": "UP"}

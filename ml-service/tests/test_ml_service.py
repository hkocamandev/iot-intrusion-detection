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

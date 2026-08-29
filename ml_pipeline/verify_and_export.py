import os
import sys
import json
import pickle
import torch
import numpy as np
import onnxruntime as ort
from pathlib import Path

# Add project root to sys.path to import TabularNN
PROJECT_ROOT = Path(__file__).resolve().parents[1]
TRAIN_MODULE_PATH = PROJECT_ROOT / "app" / "applet" / "ml_pipeline"
sys.path.append(str(TRAIN_MODULE_PATH))

# Import the original model class
from train_clinical_models import TabularNN

EXPORT_DIR = Path(__file__).resolve().parent / 'exported_models'

# Helper to load preprocessing and determine input dimension

def load_preproc(preproc_path):
    """Load preprocessing artifact and return total input dimension.
    If the pickle file is corrupted or unreadable, fall back to inferring the input
    dimension from the model checkpoint's first linear layer weight shape.
    """
    try:
        with open(preproc_path, "rb") as f:
            preproc = pickle.load(f)
        # preproc contains imputer_num, scaler_num, ohe_cat, feature_num, feature_cat
        num_features = len(preproc["feature_num"]) if "feature_num" in preproc else 0
        # For categorical features, use the OneHotEncoder to obtain number of output columns
        cat_encoder = preproc.get("ohe_cat")
        if cat_encoder is not None:
            cat_features = cat_encoder.transform(np.zeros((1, len(preproc.get("feature_cat", [])))))
            cat_dim = cat_features.shape[1]
        else:
            cat_dim = 0
        return num_features + cat_dim
    except Exception as e:
        print(f"Failed to load preprocessing pickle ({preproc_path}): {e}")
        return None

def infer_input_dim_from_checkpoint(state_dict):
    """Infer input dimension from the first linear layer weight shape.
    Assumes the first weight matrix has shape (out_features, in_features).
    """
    for key, tensor in state_dict.items():
        if key.endswith('.weight') and isinstance(tensor, torch.Tensor):
            # Return the second dimension (in_features)
            return tensor.shape[1]
    return None

# Define checkpoints metadata
checkpoints = [
    {
        "name": "ds2_heart_model.pt",
        "preproc": EXPORT_DIR / "ds2_preprocessing.pkl",
        "output": EXPORT_DIR / "ds2_heart_model.onnx",
    },
    {
        "name": "ds3_stroke_model.pt",
        "preproc": EXPORT_DIR / "ds3_preprocessing.pkl",
        "output": EXPORT_DIR / "ds3_stroke_model.onnx",
    },
    {
        "name": "ds3_stroke_model_improved.pt",
        "preproc": EXPORT_DIR / "ds3_preprocessing_improved.pkl",
        "output": EXPORT_DIR / "ds3_stroke_model_improved.onnx",
    },
]

report = []

for ckpt in checkpoints:
    ckpt_path = EXPORT_DIR / ckpt["name"]
    preproc_path = ckpt["preproc"]
    print(f"\nProcessing {ckpt_path.name} ...")

    # Determine input dimension from preprocessing file
    # Determine input dimension either from preprocessing or checkpoint
    input_dim = load_preproc(preproc_path)
    state_dict = torch.load(str(ckpt_path), map_location="cpu")
    if input_dim is None:
        # Fallback inference
        input_dim = infer_input_dim_from_checkpoint(state_dict)
        print(f"Inferred input dimension from checkpoint: {input_dim}")
    else:
        print(f"Inferred input dimension from preprocessing: {input_dim}")

    # Instantiate model with exact hyper‑parameters used during training
    dropout = 0.4 if "ds3" in ckpt_path.name else 0.3
    model = TabularNN(input_dim=input_dim, hidden_dims=[128, 64, 32], dropout_rate=dropout)
    model.eval()

    # Load the checkpoint state_dict into model
    load_result = model.load_state_dict(state_dict, strict=True)
    missing = load_result.missing_keys
    unexpected = load_result.unexpected_keys
    print(f"Missing keys: {missing}")
    print(f"Unexpected keys: {unexpected}")

    if missing or unexpected:
        report.append({"checkpoint": ckpt_path.name, "status": "load_failed", "missing": missing, "unexpected": unexpected})
        continue

    # Dummy forward pass to ensure inference works
    dummy_input = torch.randn(1, input_dim)
    with torch.no_grad():
        torch_out = model(dummy_input)
    print(f"Dummy forward output shape: {torch_out.shape}")

    # Export to ONNX
    onnx_path = ckpt["output"]
    torch.onnx.export(
        model,
        dummy_input,
        str(onnx_path),
        export_params=True,
        opset_version=12,
        do_constant_folding=True,
        input_names=["input"],
        output_names=["output"],
    )
    print(f"ONNX model written to {onnx_path}")

    # Verify ONNX model with onnxruntime
    sess = ort.InferenceSession(str(onnx_path))
    ort_input = {sess.get_inputs()[0].name: dummy_input.numpy()}
    ort_out = sess.run(None, ort_input)[0]
    if np.allclose(torch_out.numpy(), ort_out, atol=1e-5, rtol=1e-4):
        print("ONNX verification passed.")
        report.append({"checkpoint": ckpt_path.name, "status": "success"})
    else:
        print("ONNX verification FAILED.")
        report.append({"checkpoint": ckpt_path.name, "status": "onnx_verification_failed"})

# Write summary report
summary_path = EXPORT_DIR / "verification_report.json"
with open(summary_path, "w") as f:
    json.dump(report, f, indent=2)
print(f"\nVerification summary written to {summary_path}")

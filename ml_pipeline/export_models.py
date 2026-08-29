import os
import torch
import json
from pathlib import Path
import logging

# Define project root and relevant directories
PROJECT_ROOT = Path(__file__).resolve().parent.parent
EXPORT_DIR = PROJECT_ROOT / "ml_pipeline" / "exported_models"
LOG_DIR = PROJECT_ROOT / "ml_pipeline" / "logs"

# Ensure directories exist
EXPORT_DIR.mkdir(parents=True, exist_ok=True)
LOG_DIR.mkdir(parents=True, exist_ok=True)

# Configure logging
log_file = LOG_DIR / "export_models.log"
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    handlers=[logging.FileHandler(log_file), logging.StreamHandler()],
)

REPORT_PATH = EXPORT_DIR / "export_report.json"
report = []

def export_to_onnx(model_path: Path, onnx_path: Path):
    try:
        model = torch.load(model_path, map_location="cpu")
        model.eval()
        # Determine input shape
        if hasattr(model, "input_shape"):
            input_shape = model.input_shape
        else:
            # Heuristic: look for first Linear or Conv2d layer
            input_shape = None
            for m in model.modules():
                if isinstance(m, torch.nn.Linear):
                    input_shape = (m.in_features,)
                    break
                if isinstance(m, torch.nn.Conv2d):
                    input_shape = (m.in_channels, 224, 224)
                    break
            if input_shape is None:
                input_shape = (1,)
        dummy_input = torch.randn(1, *input_shape)
        torch.onnx.export(
            model,
            dummy_input,
            onnx_path.as_posix(),
            export_params=True,
            opset_version=12,
            do_constant_folding=True,
            input_names=["input"],
            output_names=["output"],
        )
        logging.info(f"Exported ONNX: {onnx_path.name}")
        report.append({
            "model": model_path.name,
            "onnx": onnx_path.name,
            "status": "success",
            "input_shape": list(input_shape),
        })
    except Exception as e:
        logging.error(f"Failed ONNX export for {model_path.name}: {e}")
        report.append({
            "model": model_path.name,
            "onnx": None,
            "status": f"failure: {e}",
        })

def main():
    for pt_file in EXPORT_DIR.glob("*.pt"):
        base_name = pt_file.stem
        onnx_file = EXPORT_DIR / f"{base_name}.onnx"
        export_to_onnx(pt_file, onnx_file)
    # Write report
    with open(REPORT_PATH, "w") as f:
        json.dump(report, f, indent=2)
    logging.info(f"Export report written to {REPORT_PATH}")

if __name__ == "__main__":
    main()

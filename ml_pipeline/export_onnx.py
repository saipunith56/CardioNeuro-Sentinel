import os
import json
import torch
import onnxruntime as ort
from pathlib import Path
import logging

# Paths
WORKSPACE_ROOT = Path(r"c:/Users/hp/Downloads/cardioneuro-sentinel")
EXPORT_DIR = WORKSPACE_ROOT / "ml_pipeline" / "exported_models"
REPORT_PATH = EXPORT_DIR / "export_report.json"
LOG_PATH = WORKSPACE_ROOT / "ml_pipeline" / "logs" / "export_onnx.log"

os.makedirs(LOG_PATH.parent, exist_ok=True)
logging.basicConfig(
    filename=LOG_PATH,
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)

def load_torch_model(pt_path: Path):
    """Try to load a torch model (torchscript or regular state dict)."""
    try:
        model = torch.jit.load(str(pt_path), map_location="cpu")
        model.eval()
        logging.info(f"Loaded TorchScript model {pt_path.name}")
        return model
    except Exception as e:
        logging.warning(f"torch.jit.load failed for {pt_path.name}: {e}")
        try:
            model = torch.load(str(pt_path), map_location="cpu")
            if isinstance(model, torch.nn.Module):
                model.eval()
                logging.info(f"Loaded nn.Module model {pt_path.name}")
                return model
            else:
                logging.error(f"Loaded object from {pt_path.name} is not a nn.Module.")
                return None
        except Exception as e2:
            logging.error(f"Failed to load PyTorch model {pt_path.name}: {e2}")
            return None

def infer_input_shape(model):
    """Return a dummy input shape for the model.
    Uses model.input_shape attribute if present, otherwise falls back to a generic shape.
    """
    shape = getattr(model, "input_shape", None)
    if shape:
        if not isinstance(shape, (list, tuple)):
            shape = (shape,)
        return (1, *shape)
    # fallback: try to infer from first Conv2d layer
    for module in model.modules():
        if isinstance(module, torch.nn.Conv2d):
            c = module.in_channels
            return (1, c, 224, 224)
    # generic default
    return (1, 3, 224, 224)

def export_to_onnx(model, dummy_input, onnx_path: Path):
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

def verify_onnx(onnx_path: Path):
    sess = ort.InferenceSession(str(onnx_path))
    inp = sess.get_inputs()[0]
    out = sess.get_outputs()[0]
    inp_shape = [1 if (dim is None or dim == "?") else dim for dim in inp.shape]
    out_shape = [1 if (dim is None or dim == "?") else dim for dim in out.shape]
    return inp_shape, out_shape

def main():
    report = {}
    for pt_file in EXPORT_DIR.glob("*.pt"):
        name = pt_file.stem
        model = load_torch_model(pt_file)
        if model is None:
            report[name] = {
                "success": False,
                "error": "Failed to load model",
                "output_path": None,
                "input_shape": None,
                "output_shape": None,
            }
            continue
        try:
            dummy_shape = infer_input_shape(model)
            dummy_input = torch.randn(*dummy_shape)
            onnx_path = EXPORT_DIR / f"{name}.onnx"
            export_to_onnx(model, dummy_input, onnx_path)
            inp_shape, out_shape = verify_onnx(onnx_path)
            report[name] = {
                "success": True,
                "output_path": str(onnx_path),
                "input_shape": dummy_shape,
                "output_shape": out_shape,
            }
            logging.info(f"Exported ONNX for {name} -> {onnx_path.name}")
        except Exception as e:
            logging.error(f"Export/verification failed for {name}: {e}")
            report[name] = {
                "success": False,
                "error": str(e),
                "output_path": None,
                "input_shape": None,
                "output_shape": None,
            }
    with open(REPORT_PATH, "w") as f:
        json.dump(report, f, indent=2)
    logging.info(f"Export report written to {REPORT_PATH}")

if __name__ == "__main__":
    main()

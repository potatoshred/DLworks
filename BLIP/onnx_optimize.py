import onnxruntime.tools
import onnxruntime.tools.optimize_onnx_model
from pathlib import Path
model_path = Path("D:\LibPy\dachuang\BLIP\model.onnx")
output_path = Path("D:\LibPy\dachuang\BLIP\model_optimized.onnx")
onnxruntime.tools.optimize_onnx_model.optimize_model(model_path=model_path, output_path=output_path)
from models.blip import blip_decoder
from torch.utils.mobile_optimizer import optimize_for_mobile
import onnx
import onnxruntime
from PIL import Image
import requests
import torch
from torchvision import transforms
from torchvision.transforms.functional import InterpolationMode

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')


def load_demo_image(image_size, device):
    img_url = "D:\cars.webp"
    raw_image = Image.open(img_url).convert('RGB')
    transform = transforms.Compose([
        transforms.Resize((image_size, image_size),
                          interpolation=InterpolationMode.BICUBIC),
        transforms.ToTensor(),
        transforms.Normalize((0.48145466, 0.4578275, 0.40821073),
                             (0.26862954, 0.26130258, 0.27577711))
    ])
    image = transform(raw_image).unsqueeze(0).to(device)
    return image


image_size = 384
image = load_demo_image(image_size=image_size, device=device)

model_url = 'https://storage.googleapis.com/sfr-vision-language-research/BLIP/models/model_base_capfilt_large.pth'
# model_url = "D:\LibPy\dachuang\BLIP\models\model_base_capfilt_large.pth"

model = blip_decoder(pretrained=model_url, image_size=image_size, vit='base')
# model = torch.load(model_url, map_location=device)
model = model.to(device)

model.eval()

# with torch.no_grad():
#     # beam search
#     caption = model.generate(image, sample=False, num_beams=3, max_length=20, min_length=5)
#     # nucleus sampling
#     # caption = model.generate(image, sample=True, top_p=0.9, max_length=20, min_length=5)
#     print('caption: '+caption[0])

torch.onnx.export(model, (image, ""), "model.onnx", verbose=True, training=torch.onnx.TrainingMode.EVAL,
                  export_params=True, input_names=['image', "caption"])
print("SUCCESS")


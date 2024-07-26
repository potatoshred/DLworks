package com.example.sademo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.pytorch.IValue;
//import org.pytorch.Module;
//import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri curImageUri;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private Uri photoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.demo);
    }

    public void LoadImage(View view) {
        openImageChooser();
    }
    // 请求码，用于在onActivityResult中区分请求

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            curImageUri = selectedImageUri;
            imageView.setImageURI(selectedImageUri);
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageView.setImageURI(photoURI);
        }
    }

    private String getRealPathFromURI(Uri uri) {
        String result;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // 创建文件来保存拍摄的图片
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // 处理错误
                Log.e("TAG", "Error creating image file", ex);
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private String currentPhotoPath;

    private File createImageFile() throws IOException {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // 保存文件路径
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    Module module;

    //    private float[] load_image(int imageSize, String imagePath) {
//        float[] outputData = null;
//        try {
//            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(imagePath));
//            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true);
//
//            float[] mean = {0.48145466f, 0.4578275f, 0.40821073f};
//            float[] std = {0.26862954f, 0.26130258f, 0.27577711f};
//            Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, mean, std);
//
//            // Add batch dimension
//            inputTensor = addBatchDimension(inputTensor);
//
//            // 调用模型并获得输出
//            Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
//            outputData = outputTensor.getDataAsFloatArray();
//
//            // 处理输出数据 (根据你的需求)
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        ;
//        return outputData;
//    }
    private Tensor load_image(int imageSize, String imagePath) {
        Tensor outputTensor = null;
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(imagePath));
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true);

            float[] mean = {0.48145466f, 0.4578275f, 0.40821073f};
            float[] std = {0.26862954f, 0.26130258f, 0.27577711f};
            Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, mean, std);

            // Add batch dimension
            inputTensor = addBatchDimension(inputTensor);

            // 调用模型并获得输出
            outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

            // 处理输出数据 (根据你的需求)
        } catch (IOException e) {
            e.printStackTrace();
        }
        ;
        return outputTensor;
    }

    private Tensor addBatchDimension(Tensor tensor) {
        float[] data = tensor.getDataAsFloatArray();
        long[] shape = tensor.shape();
        long[] newShape = new long[shape.length + 1];
        newShape[0] = 1;  // batch size
        System.arraycopy(shape, 0, newShape, 1, shape.length);
        return Tensor.fromBlob(data, newShape);
    }

    public void Explain(View view) {
        if (curImageUri == null) {
            android.app.AlertDialog.Builder tmp = new android.app.AlertDialog.Builder(MainActivity.this);
            tmp.setMessage("Please select an image first").setCancelable(false).setPositiveButton("OK", null);
            android.app.AlertDialog alertDialog = tmp.create();
            alertDialog.show();
            return;
        }
        Bitmap bitmap = null;
        Module module = null;
        try {
            bitmap = BitmapFactory.decodeFile(getRealPathFromURI(curImageUri));
            Log.d("TAG", "Explain: Image loaded" + curImageUri.getEncodedPath());
            Log.d("TAG", "Explain: Image size: " + bitmap.getWidth() + " " + bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, 640, bitmap.getHeight() * 640 / bitmap.getWidth(), false);
//            module = LiteModuleLoader.load(getClass().getResource("/assets/model.ptl").toString());
            module = Module.load(assetFilePath(this, "model_base_capfilt_large.pt"));
//            module = LiteModuleLoader.load(MainActivity.assetFilePath(this, "model.ptl"));
        } catch (IOException e) {
            Log.e("TAG", "Error reading assets", e);
            finish();
        }
//        Log.d("TAG", "Image size: " + bitmap.getWidth() + " " + bitmap.getHeight());
//        Module module = LiteModuleLoader.load("app/src/assets/model.pt");
//        Log.d("TAG", "Explain: Model loaded");
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        assert module != null;

        Tensor loaded_image = load_image(384, getRealPathFromURI(curImageUri));
        IValue output = module.runMethod("generate", IValue.from(loaded_image), IValue.from(0), IValue.from(3), IValue.from(20), IValue.from(5));

//        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
//        float[] scores = outputTensor.getDataAsFloatArray();
//        float maxScore = -Float.MAX_VALUE;
//        int maxScoreIdx = -1;
//        for (int i = 0; i < scores.length; i++) {
//            if (scores[i] > maxScore) {
//                maxScore = scores[i];
//                maxScoreIdx = i;
//            }
//        }
//        String className = ImageNetClasses.IMAGENET_CLASSES[maxScoreIdx];
        String message = output.toString();
        Log.d("TAG", "Explain: " + message);

        // Show the result in the Alert dialog
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setMessage(message).setCancelable(false).setPositiveButton("OK", null);
        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        Log.d("TAG", "Trying to copy asset file: " + file.getAbsolutePath());
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            Log.d("TAG", "File copied to " + file.getAbsolutePath());
            return file.getAbsolutePath();
        }
    }

    private static final int REQUEST_CAMERA_PERMISSION = 3; // 定义请求权限的常量

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    public void Camera(View view) {
        requestPermissions();
        dispatchTakePictureIntent();
    }
}
package com.example.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static int REQ_1 = 1;
    private static int REQ_2 = 2;
    private ImageView img;
    private Button btn2;
    private Button btn3;
    private String mFilePath;
    private int writeFlag = 0;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = findViewById(R.id.img);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        checkWritePermission();
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermission();
            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CustomCameraActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String path = getIntent().getStringExtra("picPath");
        if (!TextUtils.isEmpty(path)) {
            try {
                String str = getIntent().getStringExtra("str");
                Bitmap waterMark = null;
                if (!TextUtils.isEmpty(str)) {
                    waterMark = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(waterMark);
                    Paint paint = new Paint();
                    paint.setColor(getResources().getColor(R.color.purple_200));
                    paint.setTextSize(150);
                    canvas.drawText(str, 100, 100, paint);
                }
                FileInputStream fis = new FileInputStream(path);
                Bitmap bitmap = BitmapFactory.decodeStream(fis);
                Matrix matrix = new Matrix();
                matrix.setRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(newBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                if (waterMark != null) {
                    canvas.drawBitmap(waterMark, newBitmap.getWidth() / 2 + (newBitmap.getWidth() / 2 - waterMark.getWidth()) - 50,
                            newBitmap.getHeight() / 2 + (newBitmap.getHeight() / 2 - waterMark.getHeight()), null);
                    img.setImageBitmap(newBitmap);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        } else {
            writeFlag = 1;
        }
    }

    private void checkCameraPermission() {
        if (writeFlag == 0) {
            Toast.makeText(this, "没有存储权限，请接收权限申请获前往设置添加权限", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            } else {
                startCamera2();
            }
        }
    }

    public void startCamera1(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQ_1);
    }

    public void startCamera2() {
        mFilePath = "/sdcard/DCIM/aa.jpg";
        File file = new File(mFilePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            if (Build.VERSION.SDK_INT > 24) {
                uri = FileProvider.getUriForFile(this, "com.example.camera.fileprovider", file);
            } else {
                uri = Uri.fromFile(new File(mFilePath));
            }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, REQ_2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_1) {
                Bundle bundle = data.getExtras();
                Bitmap bitmap = (Bitmap) bundle.get("data");
                img.setImageBitmap(bitmap);
            } else if (requestCode == REQ_2) {
                ContentResolver contentResolver = getContentResolver();
                InputStream fis = null;
                try {
                    fis = contentResolver.openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(fis);
                    img.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera2();
                } else {
                    Toast.makeText(this, "没有相机权限，请接收权限申请获前往设置添加权限", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeFlag = 1;
                } else {
                    Toast.makeText(this, "没有存储权限，请接收权限申请获前往设置添加权限", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
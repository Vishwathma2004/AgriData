package com.example.imagedescriber;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageDetailsActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1001;
    private static final String TAG = "ImageDetailsActivity";

    private ImageView previewImageView;
    private EditText editTitle, editDescription, editFarmerName, editPlantDisease, editAdditionalDetails;
    private TextView locationText;
    private Button updateBtn;

    private DatabaseHelper dbHelper;
    private ExecutorService executorService;
    private long imageId;
    private ImageEntry currentImage;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        previewImageView = findViewById(R.id.preview_image);
        editTitle = findViewById(R.id.edit_title);
        editDescription = findViewById(R.id.edit_description);
        editFarmerName = findViewById(R.id.edit_farmer_name);
        editPlantDisease = findViewById(R.id.edit_plant_disease);
        editAdditionalDetails = findViewById(R.id.edit_additional_details);
        locationText = findViewById(R.id.location_text);
        updateBtn = findViewById(R.id.update_button);

        dbHelper = new DatabaseHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        // Export PDF Button logic
        Button btnExportPDF = findViewById(R.id.btn_export_pdf);
        btnExportPDF.setOnClickListener(v -> {
            if (currentImage != null) {
                File pdfFile = PDFUtils.generatePDF(this, currentImage);
                if (pdfFile != null) {
                    sharePDF(pdfFile);
                } else {
                    Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imageId = getIntent().getLongExtra("IMAGE_ID", -1);
        if (imageId == -1) {
            showToast("Error: No image ID provided");
            finish();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            loadImageDetails(imageId);
        }

        updateBtn.setOnClickListener(v -> {
            String newTitle = editTitle.getText().toString().trim();
            String newDesc = editDescription.getText().toString().trim();
            String farmerName = editFarmerName.getText().toString().trim();
            String plantDisease = editPlantDisease.getText().toString().trim();
            String additionalDetails = editAdditionalDetails.getText().toString().trim();

            String location = currentImage.getLocation();
            long timestamp = currentImage.getTimestamp();

            boolean updated = dbHelper.updateImageDetailsFull(
                    imageId, newTitle, newDesc, farmerName, additionalDetails, plantDisease, location, timestamp
            );

            if (updated) {
                showToast("Image metadata updated!");
                finish();
            } else {
                showToast("Update failed.");
            }
        });
    }

    private void loadImageDetails(long id) {
        currentImage = dbHelper.getImageById(id);
        if (currentImage == null) {
            showToast("Image not found in database");
            finish();
            return;
        }

        String path = currentImage.getImagePath();
        executorService.execute(() -> {
            Bitmap bitmap = loadBitmapFromPath(path);
            runOnUiThread(() -> {
                if (bitmap != null) {
                    previewImageView.setImageBitmap(bitmap);
                } else {
                    showToast("Unable to load image.");
                    previewImageView.setImageResource(R.drawable.ic_launcher_foreground);
                }
            });
        });

        editTitle.setText(currentImage.getTitle());
        editDescription.setText(currentImage.getDescription());
        editFarmerName.setText(currentImage.getFarmerName());
        editPlantDisease.setText(currentImage.getPlantDisease());
        editAdditionalDetails.setText(currentImage.getAdditionalDetails());

        if (currentImage.getLocation() != null) {
            locationText.setText("Location: " + currentImage.getLocation());
        }
    }

    private Bitmap loadBitmapFromPath(String imagePath) {
        try {
            if (imagePath.startsWith("http")) {
                URL url = new URL(imagePath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } else {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    return rotateImageIfRequired(bitmap, imageFile.getAbsolutePath());
                } else {
                    Uri uri = Uri.parse(imagePath);
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    return BitmapFactory.decodeStream(inputStream);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            return null;
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return img;
            }
            return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        } catch (IOException e) {
            Log.e(TAG, "Rotation error", e);
            return img;
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void sharePDF(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        // ✅ Grant permission to external apps
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "Share Report via");

// Grant URI permission to all resolved apps
        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(chooser);

        startActivity(Intent.createChooser(intent, "Share Report via"));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadImageDetails(imageId);
        } else {
            showToast("Permission denied");
            finish();
        }
    }
}

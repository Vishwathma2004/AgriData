package com.example.imagedescriber;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewImageActivity extends AppCompatActivity {
    private static final String TAG = "ViewImageActivity";

    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView timestampTextView;
    private ImageView imageView;

    private DatabaseHelper dbHelper;
    private ImageEntry imageEntry;
    private long imageId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        setupToolbar();

        titleTextView = findViewById(R.id.title_text);
        descriptionTextView = findViewById(R.id.description_text);
        timestampTextView = findViewById(R.id.timestamp_text);
        imageView = findViewById(R.id.image_view);

        dbHelper = new DatabaseHelper(this);

        imageId = getIntent().getLongExtra("IMAGE_ID", -1);
        if (imageId == -1) {
            showErrorAndExit("Error: No image specified");
            return;
        }

        loadImageDetails(imageId);

        findViewById(R.id.delete_button).setOnClickListener(v -> showDeleteConfirmationDialog());

        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(ViewImageActivity.this, ImageDetailsActivity.class);
            intent.putExtra("IMAGE_ID", imageId);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Image Details");
        }
    }

    private void loadImageDetails(long id) {
        imageEntry = dbHelper.getImageById(id);
        if (imageEntry == null) {
            showErrorAndExit("Image not found");
            return;
        }

        String formattedDate = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault())
                .format(new Date(imageEntry.getTimestamp()));
        timestampTextView.setText(formattedDate);

        titleTextView.setText(imageEntry.getTitle() != null ? imageEntry.getTitle() : "Untitled");
        descriptionTextView.setText(imageEntry.getDescription() != null ? imageEntry.getDescription() : "No description");

        // Log image path
        Log.d(TAG, "Trying to load image from path: " + imageEntry.getImagePath());

        // Try local path first, fallback to Cloudinary if needed
        if (imageEntry.getImagePath() != null && !imageEntry.getImagePath().trim().isEmpty()) {
            loadImageFromPath(imageEntry.getImagePath());
        } else if (imageEntry.getCloudinaryUrl() != null && !imageEntry.getCloudinaryUrl().trim().isEmpty()) {
            loadImageFromUrl(imageEntry.getCloudinaryUrl());
        } else {
            showError("No valid image path found.");
        }
    }

    private void loadImageFromPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            showError("Invalid image path");
            return;
        }

        if (path.startsWith("http")) {
            loadImageFromUrl(path);
        } else if (path.startsWith("content://")) {
            try (InputStream inputStream = getContentResolver().openInputStream(Uri.parse(path))) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load image from content URI", e);
                showError("Could not load image");
            }
        } else {
            File file = new File(path);
            if (file.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                bitmap = rotateImageIfRequired(bitmap, file.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
            } else {
                Log.e(TAG, "File does not exist: " + path);
                showError("Image file not found");
            }
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
            Log.e(TAG, "Exif rotation error", e);
            return img;
        }
    }


    private void loadImageFromUrl(String urlString) {
        executor.execute(() -> {
            Bitmap bitmap = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                try (InputStream input = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(input);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image from URL", e);
            }

            Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                if (finalBitmap != null) {
                    imageView.setImageBitmap(finalBitmap);
                } else {
                    showError("Failed to load image from the internet");
                }
            });
        });
    }

    private void showDeleteConfirmationDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Memory")
                .setMessage("Are you sure you want to delete this memory? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage() {
        if (imageEntry != null) {
            boolean deleted = dbHelper.deleteImage(imageEntry.getId());
            if (deleted) {
                Toast.makeText(this, "Memory deleted", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                showError("Failed to delete memory");
            }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        imageView.setImageResource(R.drawable.placeholder); // Add a placeholder drawable in res/drawable/
    }

    private void showErrorAndExit(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

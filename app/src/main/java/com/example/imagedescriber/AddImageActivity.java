package com.example.imagedescriber;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddImageActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;

    private ImageView imagePreview;
    private EditText farmerNameInput, plantNameInput, plantDiseaseInput, locationInput, additionalDetailsInput, descriptionInput;
    private Button saveBtn, getLocationBtn;

    private ProgressDialog progressDialog;
    private Bitmap selectedBitmap;
    private Uri cameraImageUri;
    private File imageFile;

    private DatabaseHelper dbHelper;
    private FusedLocationProviderClient fusedLocationClient;

    private String cloudinaryUrlFromIntent = null;
    private String defaultTitleFromIntent = null;
    private String publicIdFromIntent = null;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        try {
                            bitmap = rotateImageIfRequired(bitmap, imageFile.getAbsolutePath());
                            selectedBitmap = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true);
                            imagePreview.setImageBitmap(selectedBitmap);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_image);

        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();

        findViewById(R.id.take_photo_button).setOnClickListener(v -> checkCameraPermission());
        saveBtn.setOnClickListener(v -> validateAndUpload());
        getLocationBtn.setOnClickListener(v -> checkLocationPermissionAndFetch());

        Intent intent = getIntent();
        cloudinaryUrlFromIntent = intent.getStringExtra("IMAGE_PATH");
        defaultTitleFromIntent = intent.getStringExtra("DEFAULT_TITLE");
        publicIdFromIntent = intent.getStringExtra("PUBLIC_ID"); // ✅ New

        if (cloudinaryUrlFromIntent != null) {
            Glide.with(this).load(cloudinaryUrlFromIntent).into(imagePreview);
            if (defaultTitleFromIntent != null && !defaultTitleFromIntent.trim().isEmpty()) {
                String titleWithoutExtension = defaultTitleFromIntent.replaceAll("\\.[^.]+$", "");
                plantNameInput.setText(titleWithoutExtension);
            }
        }
    }

    private void initViews() {
        imagePreview = findViewById(R.id.image_preview);
        saveBtn = findViewById(R.id.save_button);
        getLocationBtn = findViewById(R.id.get_location_button);
        farmerNameInput = findViewById(R.id.farmer_name_input);
        plantNameInput = findViewById(R.id.plant_name_input);
        plantDiseaseInput = findViewById(R.id.plant_disease_input);
        locationInput = findViewById(R.id.location_input);
        additionalDetailsInput = findViewById(R.id.additional_details_input);
        descriptionInput = findViewById(R.id.description_input);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                String locationName = "";
                                if (address.getLocality() != null) locationName += address.getLocality();
                                if (address.getSubAdminArea() != null && !address.getSubAdminArea().equals(address.getLocality()))
                                    locationName += ", " + address.getSubAdminArea();
                                if (address.getAdminArea() != null) locationName += ", " + address.getAdminArea();
                                if (address.getCountryName() != null) locationName += ", " + address.getCountryName();
                                locationInput.setText(locationName);
                            } else {
                                locationInput.setText("Unknown Location");
                            }
                        } catch (IOException e) {
                            locationInput.setText("Location error");
                        }
                    } else {
                        locationInput.setText("Location not available");
                    }
                });
    }

    private void openCamera() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        imageFile = new File(getExternalFilesDir(null), "IMG_" + timeStamp + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(cameraIntent);
    }

    private void validateAndUpload() {
        String farmerName = farmerNameInput.getText().toString().trim();
        String plantName = plantNameInput.getText().toString().trim();
        String disease = plantDiseaseInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String additionalDetails = additionalDetailsInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        if (plantName.isEmpty()) {
            plantNameInput.setError("Plant name is required");
            return;
        }
        if (disease.isEmpty()) {
            plantDiseaseInput.setError("Disease is required");
            return;
        }
        if (location.isEmpty()) {
            locationInput.setError("Location is required");
            return;
        }

        saveBtn.setEnabled(false);
        showProgressDialog("Saving data...");

        if (cloudinaryUrlFromIntent != null) {
            // File picker flow
            saveToDatabase(cloudinaryUrlFromIntent, null, farmerName, plantName, disease, location, additionalDetails, description, publicIdFromIntent);
        } else if (selectedBitmap != null) {
            // Camera flow
            uploadToCloudinary(selectedBitmap, farmerName, plantName, disease, location, additionalDetails, description);
        } else {
            hideProgressDialog();
            saveBtn.setEnabled(true);
            Toast.makeText(this, "Please capture or select an image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadToCloudinary(Bitmap bitmap, String farmerName, String plantName, String disease,
                                    String location, String additionalDetails, String description) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageData = baos.toByteArray();

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", getString(R.string.cloudinary_cloud_name),
                "api_key", getString(R.string.cloudinary_api_key),
                "api_secret", getString(R.string.cloudinary_api_secret)
        ));

        new Thread(() -> {
            try {
                String timeStr = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
                Map uploadResult = cloudinary.uploader().upload(imageData, ObjectUtils.asMap(
                        "resource_type", "image",
                        "context", String.format(
                                "alt=%s|farmer_name=%s|plant_name=%s|disease=%s|location=%s|details=%s|timestamp=%s",
                                description, farmerName, plantName, disease, location, additionalDetails, timeStr)
                ));

                String uploadedUrl = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");

                runOnUiThread(() -> saveToDatabase(uploadedUrl,
                        imageFile != null ? imageFile.getAbsolutePath() : null,
                        farmerName, plantName, disease, location, additionalDetails, description, publicId));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    saveBtn.setEnabled(true);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveToDatabase(String cloudUrl, String localPath,
                                String farmerName, String plantName, String disease,
                                String location, String additionalDetails, String description,
                                String publicId) {
        ImageEntry entry = new ImageEntry();
        entry.setCloudinaryUrl(cloudUrl);
        entry.setImagePath(localPath != null ? localPath : cloudUrl);
        entry.setTitle(plantName);
        entry.setDescription(disease);
        entry.setTimestamp(System.currentTimeMillis());
        entry.setLocation(location);
        entry.setAdditionalDetails(additionalDetails);
        entry.setFarmerName(farmerName);
        entry.setPublicId(publicId);
        long insertedId = dbHelper.saveImage(entry);

        runOnUiThread(() -> {
            hideProgressDialog();
            saveBtn.setEnabled(true);
            if (insertedId != -1) {
                Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Database save failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, String imagePath) throws IOException {
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}

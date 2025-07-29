package com.example.imagedescriber;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_PICK_REQUEST = 100;
    private static final int ADD_IMAGE_REQUEST = 101;

    private String CLOUD_NAME;
    private String API_KEY;
    private String API_SECRET;

    private RecyclerView recyclerView;
    private ImageGalleryAdapter adapter;
    private List<ImageEntry> imageEntries = new ArrayList<>();
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Load secrets from res/values/secrets.xml
        CLOUD_NAME = getString(R.string.cloudinary_cloud_name);
        API_KEY = getString(R.string.cloudinary_api_key);
        API_SECRET = getString(R.string.cloudinary_api_secret);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        FloatingActionButton fabCamera = findViewById(R.id.fab_camera);
        fabCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddImageActivity.class);
            startActivityForResult(intent, ADD_IMAGE_REQUEST);
        });

        findViewById(R.id.btn_upload).setOnClickListener(v -> pickImageFromFiles());

        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterImages(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterImages(newText);
                return true;
            }
        });

        loadImages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImages();
    }

    private void loadImages() {
        imageEntries = dbHelper.getAllImages();
        if (adapter == null) {
            adapter = new ImageGalleryAdapter(this, imageEntries);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.setFilteredList(imageEntries);
        }
    }

    private void filterImages(String query) {
        List<ImageEntry> allImages = dbHelper.getAllImages();
        List<ImageEntry> filtered = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(allImages);
        } else {
            String lower = query.toLowerCase();
            for (ImageEntry entry : allImages) {
                if ((entry.getTitle() != null && entry.getTitle().toLowerCase().contains(lower)) ||
                        (entry.getDescription() != null && entry.getDescription().toLowerCase().contains(lower))) {
                    filtered.add(entry);
                }
            }
        }

        if (adapter != null) {
            adapter.setFilteredList(filtered);
        }
    }

    private void pickImageFromFiles() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), FILE_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                uploadFileToCloudinary(fileUri);
            } else {
                Toast.makeText(this, "File selection failed", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == ADD_IMAGE_REQUEST && resultCode == RESULT_OK) {
            loadImages();
        }
    }

    private void uploadFileToCloudinary(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            if (inputStream == null) {
                Toast.makeText(this, "Unable to open image", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }

            byte[] imageData = bos.toByteArray();

            new Thread(() -> {
                try {
                    Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                            "cloud_name", CLOUD_NAME,
                            "api_key", API_KEY,
                            "api_secret", API_SECRET
                    ));

                    Map uploadResult = cloudinary.uploader().upload(imageData, ObjectUtils.emptyMap());

                    String imageUrl = uploadResult.get("secure_url").toString();
                    String publicId = uploadResult.get("public_id").toString();

                    Log.d("CloudinaryUpload", "Upload successful! URL: " + imageUrl + ", Public ID: " + publicId);

                    String fileName = getFileName(uri);

                    runOnUiThread(() -> {
                        Intent intent = new Intent(MainActivity.this, AddImageActivity.class);
                        intent.putExtra("IMAGE_PATH", imageUrl);
                        intent.putExtra("DEFAULT_TITLE", fileName);
                        intent.putExtra("PUBLIC_ID", publicId);
                        intent.putExtra("SOURCE", "FILES");
                        startActivityForResult(intent, ADD_IMAGE_REQUEST);
                    });

                } catch (Exception e) {
                    Log.e("UploadError", "Upload failed", e);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();

        } catch (Exception e) {
            Log.e("FileError", "Error processing file", e);
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = "untitled.jpg";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1 && cursor.moveToFirst()) {
                    result = cursor.getString(index);
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sync) {
            new Thread(() -> {
                dbHelper.syncAllMetadataToCloudinary();
                runOnUiThread(() ->
                        Toast.makeText(this, "Metadata synced to Cloudinary", Toast.LENGTH_SHORT).show()
                );
            }).start();
            return true;

        } else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

package com.example.imagedescriber;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "image_store.db";
    private static final int DATABASE_VERSION = 6;

    private static final String TABLE_IMAGES = "images";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_LOCATION_NAME = "location_name";
    private static final String COLUMN_FARMER_NAME = "farmer_name";
    private static final String COLUMN_ADDITIONAL_DETAILS = "additional_details";
    private static final String COLUMN_CLOUDINARY_URL = "cloudinary_url";
    private static final String COLUMN_PUBLIC_ID = "public_id";
    private static final String COLUMN_PLANT_DISEASE = "plant_disease";

    private static final String CREATE_TABLE_IMAGES = "CREATE TABLE " + TABLE_IMAGES + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_IMAGE_PATH + " TEXT NOT NULL, "
            + COLUMN_TITLE + " TEXT, "
            + COLUMN_DESCRIPTION + " TEXT NOT NULL, "
            + COLUMN_TIMESTAMP + " INTEGER NOT NULL, "
            + COLUMN_LOCATION_NAME + " TEXT, "
            + COLUMN_FARMER_NAME + " TEXT, "
            + COLUMN_ADDITIONAL_DETAILS + " TEXT, "
            + COLUMN_CLOUDINARY_URL + " TEXT, "
            + COLUMN_PUBLIC_ID + " TEXT, "
            + COLUMN_PLANT_DISEASE + " TEXT"
            + ")";

    private final Cloudinary cloudinary;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", context.getString(R.string.cloudinary_cloud_name));
        config.put("api_key", context.getString(R.string.cloudinary_api_key));
        config.put("api_secret", context.getString(R.string.cloudinary_api_secret));
        cloudinary = new Cloudinary(config);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_IMAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2 && !columnExists(db, TABLE_IMAGES, COLUMN_TITLE))
            db.execSQL("ALTER TABLE " + TABLE_IMAGES + " ADD COLUMN " + COLUMN_TITLE + " TEXT");

        if (oldVersion < 3 && !columnExists(db, TABLE_IMAGES, COLUMN_LOCATION_NAME))
            db.execSQL("ALTER TABLE " + TABLE_IMAGES + " ADD COLUMN " + COLUMN_LOCATION_NAME + " TEXT");

        if (oldVersion < 4) {
            if (!columnExists(db, TABLE_IMAGES, COLUMN_FARMER_NAME))
                db.execSQL("ALTER TABLE " + TABLE_IMAGES + " ADD COLUMN " + COLUMN_FARMER_NAME + " TEXT");
            if (!columnExists(db, TABLE_IMAGES, COLUMN_ADDITIONAL_DETAILS))
                db.execSQL("ALTER TABLE " + TABLE_IMAGES + " ADD COLUMN " + COLUMN_ADDITIONAL_DETAILS + " TEXT");
            if (!columnExists(db, TABLE_IMAGES, COLUMN_CLOUDINARY_URL))
                db.execSQL("ALTER TABLE " + TABLE_IMAGES + " ADD COLUMN " + COLUMN_CLOUDINARY_URL + " TEXT");
        }

        if (oldVersion < 5 && !columnExists(db, TABLE_IMAGES, COLUMN_PUBLIC_ID))
            db.execSQL("ALTER TABLE " + TABLE_IMAGES + " ADD COLUMN " + COLUMN_PUBLIC_ID + " TEXT");

        if (oldVersion < 6 && !columnExists(db, TABLE_IMAGES, COLUMN_PLANT_DISEASE))
            db.execSQL("ALTER TABLE " + TABLE_IMAGES + " ADD COLUMN " + COLUMN_PLANT_DISEASE + " TEXT");
    }

    private boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    if (columnName.equals(name)) {
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Downgrade not supported");
    }

    public long saveImage(ImageEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_PATH, entry.getImagePath());
        values.put(COLUMN_TITLE, entry.getTitle());
        values.put(COLUMN_DESCRIPTION, entry.getDescription());
        values.put(COLUMN_TIMESTAMP, entry.getTimestamp());
        values.put(COLUMN_LOCATION_NAME, entry.getLocation());
        values.put(COLUMN_FARMER_NAME, entry.getFarmerName());
        values.put(COLUMN_ADDITIONAL_DETAILS, entry.getAdditionalDetails());
        values.put(COLUMN_CLOUDINARY_URL, entry.getCloudinaryUrl());
        values.put(COLUMN_PUBLIC_ID, entry.getPublicId());
        values.put(COLUMN_PLANT_DISEASE, entry.getPlantDisease());

        long id = -1;
        try {
            id = db.insertOrThrow(TABLE_IMAGES, null, values);
            Log.d(TAG, "Image inserted with ID: " + id);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting image: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }

    public boolean updateImageDetailsFull(long id, String title, String description, String farmerName, String additionalDetails, String plantDisease, String location, long timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_FARMER_NAME, farmerName);
        values.put(COLUMN_ADDITIONAL_DETAILS, additionalDetails);
        values.put(COLUMN_PLANT_DISEASE, plantDisease);
        values.put(COLUMN_LOCATION_NAME, location);
        values.put(COLUMN_TIMESTAMP, timestamp);

        int rows = db.update(TABLE_IMAGES, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();

        if (rows > 0) {
            ImageEntry entry = getImageById(id);
            if (entry != null && entry.getPublicId() != null) {
                updateCloudinaryMetadata(entry);
            }
        }

        return rows > 0;
    }

    private void updateCloudinaryMetadata(ImageEntry entry) {
        String readableTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(entry.getTimestamp()));

        String context = String.format("alt=%s|farmer_name=%s|plant_name=%s|disease=%s|location=%s|details=%s|timestamp=%s",
                entry.getDescription(),
                entry.getFarmerName(),
                entry.getTitle(),
                entry.getPlantDisease(),
                entry.getLocation(),
                entry.getAdditionalDetails(),
                readableTimestamp);

        new Thread(() -> {
            try {
                cloudinary.uploader().explicit(entry.getPublicId(), ObjectUtils.asMap(
                        "type", "upload",
                        "context", context
                ));
                Log.d(TAG, "✅ Cloudinary metadata updated for: " + entry.getPublicId());
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to update Cloudinary metadata: " + e.getMessage());
            }
        }).start();
    }

    public void syncAllMetadataToCloudinary() {
        List<ImageEntry> entries = getAllImages();
        for (ImageEntry entry : entries) {
            if (entry.getCloudinaryUrl() != null && entry.getPublicId() != null) {
                updateCloudinaryMetadata(entry);
            }
        }
    }

    public void fetchCloudinaryMetadata(String publicId) {
        new Thread(() -> {
            try {
                Map result = cloudinary.api().resource(publicId, ObjectUtils.asMap("context", true));
                Map<String, String> context = (Map<String, String>) ((Map) result.get("context")).get("custom");
                for (Map.Entry<String, String> entry : context.entrySet()) {
                    Log.d("Metadata", entry.getKey() + ": " + entry.getValue());
                }
            } catch (Exception e) {
                Log.e("CloudinaryMetadata", "Failed to fetch metadata", e);
            }
        }).start();
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;
            String afterUpload = url.substring(uploadIndex + "/upload/".length());
            if (afterUpload.startsWith("v")) {
                int slashIndex = afterUpload.indexOf("/");
                if (slashIndex != -1) {
                    afterUpload = afterUpload.substring(slashIndex + 1);
                }
            }
            int dotIndex = afterUpload.lastIndexOf(".");
            if (dotIndex != -1) {
                afterUpload = afterUpload.substring(0, dotIndex);
            }
            return afterUpload;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting public ID", e);
            return null;
        }
    }

    public List<ImageEntry> getAllImages() {
        List<ImageEntry> entries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_IMAGES + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                entries.add(cursorToImageEntry(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching images: " + e.getMessage());
        } finally {
            db.close();
        }

        return entries;
    }

    public ImageEntry getImageById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        ImageEntry entry = null;
        try (Cursor cursor = db.query(TABLE_IMAGES, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                entry = cursorToImageEntry(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching image by ID: " + e.getMessage());
        } finally {
            db.close();
        }
        return entry;
    }

    public boolean deleteImage(long id) {
        // Fetch the image entry first to get public_id
        ImageEntry entry = getImageById(id);
        if (entry == null) return false;

        // Delete from SQLite
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_IMAGES, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();

        // Delete from Cloudinary in background
        if (rows > 0 && entry.getPublicId() != null && !entry.getPublicId().isEmpty()) {
            new Thread(() -> {
                try {
                    cloudinary.uploader().destroy(entry.getPublicId(), ObjectUtils.emptyMap());
                    Log.d(TAG, "✅ Image deleted from Cloudinary: " + entry.getPublicId());
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to delete image from Cloudinary: " + e.getMessage());
                }
            }).start();
        }

        return rows > 0;
    }


    private ImageEntry cursorToImageEntry(Cursor cursor) {
        ImageEntry entry = new ImageEntry();
        entry.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        entry.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
        entry.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
        entry.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
        entry.setTimestamp(cursor.getLong( cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
        entry.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_NAME)));
        entry.setFarmerName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FARMER_NAME)));
        entry.setAdditionalDetails(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDITIONAL_DETAILS)));
        entry.setCloudinaryUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLOUDINARY_URL)));
        entry.setPublicId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PUBLIC_ID)));
        entry.setPlantDisease(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLANT_DISEASE)));
        return entry;
    }
}

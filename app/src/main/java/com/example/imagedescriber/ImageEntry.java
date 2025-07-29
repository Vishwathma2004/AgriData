package com.example.imagedescriber;

/**
 * Represents an image entry with all metadata required for local database and Cloudinary storage.
 */
public class ImageEntry {

    private long id;
    private String imagePath;        // Local path or Cloudinary URL
    private String cloudinaryUrl;    // Cloudinary secure URL
    private String publicId;         // Cloudinary public ID

    private String title;            // Plant name
    private String description;      // Description or short caption
    private long timestamp;          // When image was captured or uploaded

    private String location;         // Human-readable location (via Geocoder)
    private String farmerName;       // Optional
    private String plantDisease;     // Optional
    private String additionalDetails;// Optional notes or tags

    // Default constructor
    public ImageEntry() {}

    // Full constructor
    public ImageEntry(long id, String imagePath, String cloudinaryUrl, String publicId,
                      String title, String description, long timestamp,
                      String location, String farmerName, String plantDisease, String additionalDetails) {
        this.id = id;
        this.imagePath = imagePath;
        this.cloudinaryUrl = cloudinaryUrl;
        this.publicId = publicId;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.location = location;
        this.farmerName = farmerName;
        this.plantDisease = plantDisease;
        this.additionalDetails = additionalDetails;
    }

    // Simplified constructor without cloud fields
    public ImageEntry(long id, String imagePath, String title, String description,
                      long timestamp, String location, String farmerName,
                      String plantDisease, String additionalDetails) {
        this(id, imagePath, null, null, title, description, timestamp,
                location, farmerName, plantDisease, additionalDetails);
    }

    // Getters and setters
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getCloudinaryUrl() {
        return cloudinaryUrl;
    }
    public void setCloudinaryUrl(String cloudinaryUrl) {
        this.cloudinaryUrl = cloudinaryUrl;
    }

    public String getPublicId() {
        return publicId;
    }
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    public String getFarmerName() {
        return farmerName;
    }
    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getPlantDisease() {
        return plantDisease;
    }
    public void setPlantDisease(String plantDisease) {
        this.plantDisease = plantDisease;
    }

    public String getAdditionalDetails() {
        return additionalDetails;
    }
    public void setAdditionalDetails(String additionalDetails) {
        this.additionalDetails = additionalDetails;
    }
}

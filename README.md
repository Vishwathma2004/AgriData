# 🌾 ImageDescriber – AgriData Management App
ImageDescriber is an Android application that allows users (farmers or field workers) to **capture, upload, and manage agricultural images** with detailed metadata. It supports **Cloudinary integration**, **SQLite local storage**, **Firebase Authentication**, and **PDF report generation** for field reports.
---
## 🚀 Features
✅ **Capture or Upload Images** – Take photos with the camera or select images from files  
✅ **Cloudinary Integration** – Automatically uploads images to Cloudinary for cloud storage  
✅ **SQLite Database** – Stores metadata (title, description, farmer name, plant name, disease info, location, timestamp) locally  
✅ **Search Functionality** – Search images by title or description  
✅ **PDF Report Generation** – Generate a professional field report with images and metadata  
✅ **Firebase Authentication** – Secure login/logout with session management  
✅ **Metadata Sync** – Sync all image metadata to Cloudinary  
✅ **Responsive UI** – Uses `RecyclerView` with Grid layout for gallery view  
---
## 🛠️ Tech Stack
- **Language:** Java  
- **Database:** SQLite  
- **Cloud Storage:** Cloudinary API  
- **Authentication:** Firebase Auth  
- **UI:** Material Design, RecyclerView  
- **Libraries:** Glide (Image loading), iTextPDF (PDF generation), Google Play Services (Location)  
---
## 📂 Project Structure
```plaintext
ImageDescriber/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/imagedescriber/
│   │   │   │   ├── AddImageActivity.java
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── LoginActivity.java
│   │   │   │   ├── ImageGalleryAdapter.java
│   │   │   │   ├── ImageDetailsActivity.java
│   │   │   │   ├── DatabaseHelper.java
│   │   │   │   ├── PDFUtils.java
│   │   │   │   └── ...
│   │   │   ├── res/ (Layouts, Drawables, Values)
│   │   │   ├── AndroidManifest.xml
│   │   ├── test/ (Unit tests)
│   │   └── androidTest/ (Instrumented tests)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
└── settings.gradle.kts
````

---

## ⚙️ Setup Instructions

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/Vishwathma2004/AgriData-.git  
cd ImageDescriber
```

### 2️⃣ Open in Android Studio

* Open Android Studio → **"Open Project"** → select this folder
* Let Gradle sync automatically

### 3️⃣ Add Firebase Configuration

* Place your `google-services.json` file inside the `app/` directory

### 4️⃣ Cloudinary Credentials

Update the constants in `MainActivity.java`:

```java
private static final String CLOUD_NAME = "your_cloud_name";  
private static final String API_KEY = "your_api_key";  
private static final String API_SECRET = "your_api_secret";
```

### 5️⃣ Build and Run

* Connect an Android device or use an emulator
* Click **Run ▶️** in Android Studio

---



## 🤝 Contribution

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

---

## 📜 License

This project is licensed under the MIT License – see the LICENSE file for details.


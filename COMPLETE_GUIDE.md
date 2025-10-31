# SFC - Image Analysis App - Complete Guide

## Features Implemented

### 1. **Navigation Bar (Toolbar)**
- ✅ App name "SFC" displayed in center
- ✅ Menu icon with Settings option
- ✅ Professional Material Design toolbar

### 2. **Settings Screen**
- ✅ Back button navigation
- ✅ Image storage path configuration
- ✅ Data storage path configuration
- ✅ Auto-create directories
- ✅ Saved to SharedPreferences

### 3. **Enhanced Image Analysis Workflow**
- ✅ Capture image with camera
- ✅ ML Kit image labeling
- ✅ "Find Match & Analyze" button appears after capture
- ✅ Searches for matching images in configured path
- ✅ Loads corresponding JSON data
- ✅ Displays matched results

### 4. **PDF Export**
- ✅ Combines image + data in professional layout
- ✅ Generates PDF with title, image, and data
- ✅ Saves to Documents folder
- ✅ Timestamped filenames

---

## How to Use

### Step 1: Login
- Username: `Test@example.com`
- Password: `test123`

### Step 2: Configure Settings
1. Tap menu icon (three dots) in toolbar
2. Select "Settings"
3. Enter paths:
   - **Image Path**: `/storage/emulated/0/SFC_Images`
   - **Data Path**: `/storage/emulated/0/SFC_Data`
4. Tap "Save Settings"

### Step 3: Prepare Reference Data

#### Create folder structure:
```
/storage/emulated/0/
├── SFC_Images/
│   ├── rajveer.png
│   ├── john.jpg
│   └── ...
└── SFC_Data/
    ├── rajveer.json
    ├── john.json
    └── ...
```

#### Sample JSON format (rajveer.json):
```json
{
  "name": "Rajveer Singh",
  "id": "EMP001",
  "department": "Engineering",
  "position": "Software Developer",
  "email": "rajveer@company.com",
  "phone": "+91-9876543210",
  "joinDate": "2023-01-15"
}
```

### Step 4: Capture & Analyze
1. Tap "Start Camera"
2. Grant camera permission
3. Tap "Capture Image"
4. Image is saved and analyzed with ML Kit
5. Tap "Find Match & Analyze"
6. App searches for matching image
7. Displays matched image + data
8. Button changes to "Export to PDF"

### Step 5: Export PDF
1. Tap "Export to PDF"
2. PDF is generated with:
   - Title: "SFC - Image Analysis Report"
   - Matched image
   - All data from JSON file
3. Saved to: `/storage/emulated/0/Android/data/com.example.imageanalysis/files/Documents/`

---

## File Naming Convention

**IMPORTANT**: Image and data files must have matching names!

✅ Correct:
- Image: `rajveer.png` → Data: `rajveer.json`
- Image: `john.jpg` → Data: `john.json`

❌ Wrong:
- Image: `rajveer.png` → Data: `raj.json` (names don't match)

---

## Technical Details

### Dependencies Added:
- CameraX 1.3.4 (Camera functionality)
- ML Kit Image Labeling 17.0.8 (AI analysis)
- iText7 7.2.5 (PDF generation)
- Gson 2.10.1 (JSON parsing)

### Permissions Required:
- CAMERA
- READ_EXTERNAL_STORAGE
- WRITE_EXTERNAL_STORAGE

### Storage Locations:
- **Captured Images**: `/Android/data/com.example.imageanalysis/files/SFC/`
- **Reference Images**: User-configured path (Settings)
- **Reference Data**: User-configured path (Settings)
- **Exported PDFs**: `/Android/data/com.example.imageanalysis/files/Documents/`

---

## Workflow Diagram

```
Login → Home Screen (with Toolbar)
          ↓
    Menu → Settings (Configure Paths)
          ↓
    Start Camera → Capture Image
          ↓
    ML Kit Analysis (Auto)
          ↓
    Find Match & Analyze Button
          ↓
    Search Reference Images → Match Found
          ↓
    Load JSON Data → Display Results
          ↓
    Export to PDF → Save Document
```

---

## Example Use Cases

### 1. Employee Verification
- Capture employee photo
- Match with database image
- Display employee details from JSON
- Export verification report as PDF

### 2. Product Catalog
- Capture product photo
- Match with catalog images
- Display product details
- Generate product info PDF

### 3. Inventory Management
- Capture item photo
- Match with inventory
- Display item details
- Export inventory report

---

## Build & Run

1. Open project in Android Studio
2. Sync Gradle files
3. Build project
4. Run on Android device (API 24+)
5. Grant all permissions
6. Configure settings
7. Start using!

---

## Future Enhancements (Possible)

- Advanced image matching algorithms (SIFT, ORB)
- Face recognition for people
- Barcode/QR code scanning
- Cloud storage integration
- Multi-page PDF support
- Email sharing feature
- Search history
- Batch processing

---

## Support

For issues or questions, check logs in Logcat with tag "HomeActivity" or "SettingsActivity".

---

**Version**: 1.0  
**Last Updated**: October 27, 2025  
**App Name**: SFC

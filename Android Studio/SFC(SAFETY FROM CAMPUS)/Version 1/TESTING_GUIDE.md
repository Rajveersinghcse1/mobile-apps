# SFC Image Analysis - Testing Guide

## 📱 Automatic Folder Setup

When you install and run the app for the first time, it will automatically create the following folders on your device:

```
/Android/data/com.example.imageanalysis/files/SFC/
├── ReferenceImages/   (Put your test images here)
└── ReferenceData/     (Put your JSON files here)
```

## 🗂️ Finding the Folders on Your Phone

### Method 1: Using File Manager
1. Open **Files** or **My Files** app on your Android device
2. Navigate to: **Internal Storage** → **Android** → **data** → **com.example.imageanalysis** → **files** → **SFC**
3. You will see two folders:
   - **ReferenceImages** - for images
   - **ReferenceData** - for JSON data files

### Method 2: Using Android Studio Device File Explorer
1. Connect your phone via USB
2. In Android Studio: **View** → **Tool Windows** → **Device File Explorer**
3. Navigate to: `/sdcard/Android/data/com.example.imageanalysis/files/SFC/`

### Method 3: Using ADB (Command Line)
```bash
adb shell ls /sdcard/Android/data/com.example.imageanalysis/files/SFC/
```

## 📂 Adding Test Files

### Step 1: Prepare Your Test Images
Create images with matching JSON files. For example:

**ReferenceImages/rajveer.jpg**
```
(Your reference photo)
```

**ReferenceData/rajveer.json**
```json
{
    "name": "Rajveer Singh",
    "id": "EMP001",
    "department": "IT",
    "email": "rajveer@example.com",
    "phone": "+91 98765 43210",
    "designation": "Senior Developer"
}
```

### Step 2: Copy Files to Device

#### Option A: Using USB Cable (Easiest)
1. Connect your Android phone to computer via USB
2. Enable **File Transfer** mode on your phone
3. Open your phone's storage on computer
4. Navigate to: `Android/data/com.example.imageanalysis/files/SFC/`
5. Copy images to `ReferenceImages/` folder
6. Copy JSON files to `ReferenceData/` folder

#### Option B: Using ADB Push
```bash
# Push an image
adb push rajveer.jpg /sdcard/Android/data/com.example.imageanalysis/files/SFC/ReferenceImages/

# Push JSON data
adb push rajveer.json /sdcard/Android/data/com.example.imageanalysis/files/SFC/ReferenceData/
```

#### Option C: Download Directly on Phone
1. Download test images and JSON files to your phone's Download folder
2. Use File Manager app to move them to the SFC folders

## 🧪 Testing the App

### Step 1: Login
- **Username:** Test@example.com
- **Password:** test123

### Step 2: Check Settings (Optional)
- Tap menu (3 dots) → **Settings**
- You should see the paths already filled:
  - Image Path: `/storage/emulated/0/Android/data/com.example.imageanalysis/files/SFC/ReferenceImages`
  - Data Path: `/storage/emulated/0/Android/data/com.example.imageanalysis/files/SFC/ReferenceData`

### Step 3: Test Image Matching
1. Tap **Start Camera**
2. Tap **Capture** to take a photo (take a photo similar to your reference image)
3. Tap **Analyze Match** button
4. The app will:
   - Compare your captured image with reference images
   - Show similarity percentage
   - Load and display matching JSON data
   - Allow PDF export

### Step 4: View Results
- If match found (75%+ similarity):
  - See matched image and data
  - Similarity score displayed
  - Option to export as PDF
- If no match:
  - Shows best similarity score
  - Indicates threshold needed (75%)

## 📝 Sample Test Data

### Example 1: Employee Card
**ReferenceImages/john.jpg** + **ReferenceData/john.json**
```json
{
    "name": "John Doe",
    "employeeId": "EMP002",
    "department": "Marketing",
    "joiningDate": "2023-01-15",
    "email": "john.doe@company.com"
}
```

### Example 2: Product Label
**ReferenceImages/product_a.jpg** + **ReferenceData/product_a.json**
```json
{
    "productName": "Widget A",
    "sku": "WDG-001",
    "price": "$49.99",
    "manufacturer": "TechCorp",
    "warranty": "2 years"
}
```

## ✅ Verification Checklist

- [ ] App installed successfully
- [ ] Login works with demo credentials
- [ ] Folders created automatically in File Manager
- [ ] Reference images copied to ReferenceImages folder
- [ ] JSON files copied to ReferenceData folder (same names as images)
- [ ] Settings screen shows correct paths
- [ ] Camera starts and captures photos
- [ ] Image matching finds correct reference image
- [ ] JSON data loads and displays properly
- [ ] PDF export works

## 🚨 Troubleshooting

### Problem: Folders not visible
**Solution:** Launch the app once, it creates folders on first run

### Problem: "No reference images found"
**Solution:** Make sure images are in exact path shown in Settings

### Problem: "No match found"
**Solution:** 
- Ensure reference image is similar to captured image
- Try capturing from different angle/lighting
- Check similarity threshold (needs 75%+)

### Problem: "No data file found"
**Solution:** Ensure JSON file has exact same name as image (e.g., `rajveer.jpg` → `rajveer.json`)

## 📍 Default Folder Paths

The app uses these default paths (automatically set):

| Folder | Path |
|--------|------|
| Reference Images | `/sdcard/Android/data/com.example.imageanalysis/files/SFC/ReferenceImages` |
| Reference Data | `/sdcard/Android/data/com.example.imageanalysis/files/SFC/ReferenceData` |
| Captured Images | `/sdcard/Android/data/com.example.imageanalysis/files/SFC_Images` |

---

**Ready to test!** 🚀 Install the app and the folders will be created automatically!

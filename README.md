# 🧾 OMR Scanner (Android + OpenCV)

A **template-based Optical Mark Recognition (OMR)** scanner built using **Kotlin** and **OpenCV**.  
It detects printed anchors, warps the sheet to a normalized view, and evaluates filled bubbles from camera-captured OMR sheets.

---

## 🚀 Features

- 🧩 **Template-based detection** – create once, reuse across scans  
- 🎯 **Anchor-based perspective correction** using 4 fiducial points  
- ⚙️ **Auto-adaptive bubble detection** powered by HoughCircles  
- 🧠 **Accurate filled/unfilled classification** using dark-pixel ratio  
- 🗂️ **JSON-based template system** defining anchors, bubbles & questions  
- 🖼️ **Real-time camera overlay** for anchor alignment  
- 🧪 **Debug visualization** for masked, thresholded, and CLAHE images  

---

## 🏗️ Architecture Overview

### 1. Template Creation Phase
1. Import a blank OMR sheet image.  
2. Detect **4 anchor points** (Top-Left, Top-Right, Bottom-Right, Bottom-Left).  
3. **Crop or warp** the image using anchors to isolate only the bubble region.  
4. **Preprocess** the image:
   - Convert to grayscale  
   - Apply CLAHE for contrast enhancement  
   - Apply Gaussian & Median blur  
5. Run **HoughCircles** to automatically detect all bubble centers.  
6. Sort bubbles **top → bottom**, **left → right**.  
7. Group every 4 bubbles as one question (options A–D).  
8. Save all coordinates and anchors in a **template JSON** file.

### 2. Scanning Phase
1. User selects a template (loads anchors and bubble coordinates).  
2. The camera overlay shows expected anchor positions.  
3. Detect anchors in the camera frame → compute **homography** → warp captured image to the template size.  
4. For each bubble `(x, y, r)` in the template:
   - Extract region of interest  
   - Convert to grayscale  
   - Apply threshold and compute dark-pixel fraction  
   - Decide if filled based on a confidence threshold  
5. Map detected filled bubbles to question numbers.  
6. Output final OMR result (selected options).

---

## 🧩 Template JSON Example

```json
{
  "name": "10 Questions Single Column",
  "version": "1.0",
  "sheet_width": 1080.0,
  "sheet_height": 1527.0,
  "anchor_top_left": { "x": 470.5, "y": 269.5 },
  "anchor_top_right": { "x": 719.0, "y": 269.5 },
  "anchor_bottom_left": { "x": 471.0, "y": 1394.5 },
  "anchor_bottom_right": { "x": 728.5, "y": 1395.0 },
  "options_per_question": 4,
  "questions": [
    {
      "q_no": 1,
      "options": [
        { "option": "A", "x": 57.0, "y": 60.0, "r": 19.3 },
        { "option": "B", "x": 116.0, "y": 60.0, "r": 19.4 },
        { "option": "C", "x": 171.0, "y": 60.0, "r": 19.4 },
        { "option": "D", "x": 226.0, "y": 60.0, "r": 19.3 }
      ]
    }
  ]
}

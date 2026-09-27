# Rupam AI Studio - Adobe Premiere Pro CEP Extension
**Extension ID:** `com.rupam.aistudio`  
**Host Application:** Adobe Premiere Pro (PPRO 13.0+) & After Effects (AEFT 16.0+)  
**Version:** 1.0.0  

A complete, self-contained CEP extension for Adobe Premiere Pro that brings **AI Text-to-Image**, **Image-to-Image (Reference Image)**, and **3-5 Second AI Video Generation** directly inside Premiere Pro with **one-click timeline sequence import**.

---

## 🚀 Quick Installation (Windows)

1. Double-click **`install.bat`**.
2. The batch script will automatically:
   - Enable `PlayerDebugMode=1` in Windows Registry for Adobe CSXS versions 9 through 16.
   - Copy all extension files directly to `%AppData%\Adobe\CEP\extensions\com.rupam.aistudio`.
3. Launch or restart **Adobe Premiere Pro**.
4. Navigate to: **Window** > **Extensions** > **Rupam AI Studio**.

---

## 🍏 Quick Installation (macOS)

1. Open Terminal and enable PlayerDebugMode:
   ```bash
   defaults write com.adobe.CSXS.9 PlayerDebugMode 1
   defaults write com.adobe.CSXS.10 PlayerDebugMode 1
   defaults write com.adobe.CSXS.11 PlayerDebugMode 1
   defaults write com.adobe.CSXS.12 PlayerDebugMode 1
   defaults write com.adobe.CSXS.13 PlayerDebugMode 1
   defaults write com.adobe.CSXS.14 PlayerDebugMode 1
   defaults write com.adobe.CSXS.15 PlayerDebugMode 1
   defaults write com.adobe.CSXS.16 PlayerDebugMode 1
   ```
2. Copy the extension folder to:
   ```bash
   mkdir -p ~/Library/Application\ Support/Adobe/CEP/extensions/com.rupam.aistudio
   cp -r * ~/Library/Application\ Support/Adobe/CEP/extensions/com.rupam.aistudio/
   ```
3. Restart Premiere Pro and open **Window** > **Extensions** > **Rupam AI Studio**.

---

## 🎨 Features

### 1. Text-to-Image Generation
- **Prompts & Inspiration:** Custom prompt box with "🎲 Inspire Me" randomizer.
- **Style Presets:** Cinematic, Realistic, Anime, 3D Render, Digital Art, Fantasy.
- **Aspect Ratios:** 16:9 (Landscape - Premiere Standard), 9:16 (Shorts/Reels/TikTok), 1:1 (Square), 4:3 (Classic).
- **High-Performance Models:** Flux Schnell, Flux Realism, and SDXL Turbo via free, fast endpoints.

### 2. Image-to-Image (Reference Image Support)
- **Local File Drag & Drop or URL:** Drop reference images directly into the panel or paste an image link.
- **Influence Strength Slider:** Control how strictly the AI adheres to the reference composition.
- **Style Transformation:** Re-render sketches, storyboards, or concept frames in selected styles.

### 3. Short AI Video Generation (3 to 5 Seconds)
- **Video Motion Prompts:** Create B-roll, establishing shots, and transitions.
- **Camera Movement Controls:** Slow Pan, Zoom In, Orbit 360, Aerial Drone, Static.
- **Instant Video Playback:** High-performance preview player with looping.

### 4. One-Click Import to Premiere Pro
- **Import to Sequence:** Downloads the media and places it on your active sequence timeline at the playhead!
- **Add to Project Bin:** Organizes generated assets into a dedicated `"Rupam AI Studio Assets"` bin.
- **Save to Disk:** Quick export to your local drive.

---

## 📁 Project Structure

```
├── CSXS/
│   └── manifest.xml     # Adobe CEP Manifest (com.rupam.aistudio, 350x600 panel)
├── jsx/
│   └── index.jsx        # ExtendScript host bridge for Premiere Pro project & timeline
├── index.html           # Main vanilla HTML interface
├── main.js              # Controller logic, AI API calls & CSInterface bridge
├── style.css            # Premiere Pro Spectrum dark theme styles
├── install.bat          # Automated Windows registry and file installer
└── README.md            # Documentation and instructions
```

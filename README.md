# Features
## Drawing

- Freehand drawing with customizable color and width
- Stylus pressure sensitivity
- Smooth rendering using Android `Path`

## Shapes

Supported shapes: - Rectangle - Circle - Line - Polygon

Capabilities: - Drag to move - Resize handles - Polygon vertex editing

## Text

- Add text to canvas
- Drag text to reposition
- Double‑tap to edit text

## Eraser

- Circular eraser tool
- Removes strokes, shapes, and text
- Visual eraser indicator

## Touch Support

- Finger drawing
- Stylus support

------------------------------------------------------------------------

# Setup Instructions (IFP Deployment)

## 1 Install Android Studio

Download Android Studio:

https://developer.android.com/studio

Recommended:

    Android Studio Hedgehog+
    Kotlin 1.9+
    Android SDK 36

------------------------------------------------------------------------

## 2 Clone Repository

    git clone https://github.com/priyasatim/Whiteboard_App.git

Open project in Android Studio.

------------------------------------------------------------------------

## 3 Build Project

    Build → Make Project

------------------------------------------------------------------------

## 4 Deploy to Interactive Flat Panel

Enable developer mode on the device:

    Settings → About → Tap Build Number 7 times

Enable:

    Developer Options → USB Debugging

Install APK:

    adb install app-debug.apk

------------------------------------------------------------------------

# File Format Explanation

Whiteboard content can be saved as JSON.

Example:

``` json
{
  "strokes":[
    {
      "color":-16777216,
      "width":5,
      "points":[
        [120.0,400.0],
        [130.0,410.0]
      ]
    }
  ],
  "shapes":[
    {
      "type":"rectangle",
      "topLeft":[200,200],
      "bottomRight":[400,400]
    }
  ],
  "texts":[
    {
      "text":"Hello",
      "position":[300,300],
      "size":48
    }
  ]
}
```

Coordinate format:

    [x, y]

Example:

    [250.0, 450.0]

------------------------------------------------------------------------

# Architecture Overview

The application follows **MVVM architecture**.

         Activity
            │
            ▼
    DrawingCanvas (View)
            │
            ▼
    WhiteboardViewModel
            │
            ▼
        Data Models
    (Stroke / Shape / TextItem)

------------------------------------------------------------------------

# Rendering Flow

    Touch Event
         │
         ▼
    DrawingCanvas.onTouchEvent()
         │
         ▼
    ViewModel updates state
         │
         ▼
    Canvas redraw

------------------------------------------------------------------------

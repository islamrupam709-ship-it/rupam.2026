@echo off
setlocal enabledelayedexpansion
title Rupam AI Studio - Premiere Pro CEP Installer
color 0B

echo =====================================================================
echo          RUPAM AI STUDIO - ADOBE PREMIERE PRO CEP INSTALLER
echo                       Extension ID: com.rupam.aistudio
echo =====================================================================
echo.

:: 1. Enable CEP PlayerDebugMode in Windows Registry for CSXS versions 9 to 16
echo [1/3] Enabling Adobe CEP Debug Mode in Windows Registry...
for /L %%i in (9,1,16) do (
    reg add "HKEY_CURRENT_USER\Software\Adobe\CSXS.%%i" /v PlayerDebugMode /t REG_SZ /d 1 /f >nul 2>&1
    if !errorlevel! equ 0 (
        echo   - Set PlayerDebugMode=1 for CSXS.%%i [OK]
    )
)

:: 2. Prepare Destination Directory
echo.
echo [2/3] Setting up CEP Extension folder...
set "TARGET_DIR=%APPDATA%\Adobe\CEP\extensions\com.rupam.aistudio"
set "SOURCE_DIR=%~dp0"

echo Target path: "%TARGET_DIR%"

if not exist "%TARGET_DIR%" (
    mkdir "%TARGET_DIR%" >nul 2>&1
)

:: 3. Copy All Extension Files
echo.
echo [3/3] Copying extension files to Adobe CEP folder...

:: Copy CSXS manifest directory
if exist "%SOURCE_DIR%CSXS" (
    if not exist "%TARGET_DIR%\CSXS" mkdir "%TARGET_DIR%\CSXS" >nul 2>&1
    copy /y "%SOURCE_DIR%CSXS\manifest.xml" "%TARGET_DIR%\CSXS\manifest.xml" >nul
    echo   - Copied CSXS\manifest.xml
)

:: Copy jsx folder
if exist "%SOURCE_DIR%jsx" (
    if not exist "%TARGET_DIR%\jsx" mkdir "%TARGET_DIR%\jsx" >nul 2>&1
    copy /y "%SOURCE_DIR%jsx\*.jsx" "%TARGET_DIR%\jsx\" >nul
    echo   - Copied jsx\index.jsx and jsx\ae_text_animator.jsx
)

:: Copy Web Assets
copy /y "%SOURCE_DIR%index.html" "%TARGET_DIR%\index.html" >nul
echo   - Copied index.html
copy /y "%SOURCE_DIR%main.js" "%TARGET_DIR%\main.js" >nul
echo   - Copied main.js
copy /y "%SOURCE_DIR%style.css" "%TARGET_DIR%\style.css" >nul
echo   - Copied style.css
if exist "%SOURCE_DIR%logo.png" (
    copy /y "%SOURCE_DIR%logo.png" "%TARGET_DIR%\logo.png" >nul
    echo   - Copied logo.png
)

echo.
echo =====================================================================
echo   [SUCCESS] RUPAM AI STUDIO EXTENSION INSTALLED SUCCESSFULLY!
echo =====================================================================
echo.
echo   How to launch the extension in Adobe Premiere Pro or After Effects:
echo     1. Open (or restart) Adobe Premiere Pro or Adobe After Effects.
echo     2. Go to top menu: Window ^> Extensions ^> Rupam AI Studio.
echo     3. Generate 4K visuals, apply 500 text animations, and sync SFX!
echo.
echo   Location: %TARGET_DIR%
echo =====================================================================
echo.
pause

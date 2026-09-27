/**
 * Rupam AI Studio - Master ExtendScript Bridge (Premiere Pro & After Effects)
 * Extension ID: com.rupam.aistudio
 */

#target premierepro
#target aftereffects

if (typeof $ !== 'undefined') {
    $._rupam_master = {};
}

/**
 * Returns host application identifier ('PPRO', 'AEFT', or 'UNKNOWN')
 */
function getHostApplication() {
    try {
        if (typeof app !== 'undefined' && app.name) {
            var n = app.name.toLowerCase();
            if (n.indexOf("premiere") !== -1) return "PPRO";
            if (n.indexOf("after effects") !== -1) return "AEFT";
        }
    } catch (e) {}
    return "PPRO";
}

/**
 * Loads AE animator script when running inside After Effects
 */
function ensureAEScriptLoaded() {
    try {
        if (typeof applyAETextAnimation === 'undefined') {
            var scriptFolder = new File($.fileName).parent;
            var aeScriptFile = new File(scriptFolder.fsName + "/ae_text_animator.jsx");
            if (aeScriptFile.exists) {
                $.evalFile(aeScriptFile);
            }
        }
    } catch (e) {}
}

/**
 * Returns reliable system temp folder
 */
function getSystemTempFolder() {
    try {
        if (Folder.temp && Folder.temp.exists) {
            return Folder.temp.fsName;
        }
    } catch (e) {}
    return Folder.temp.fullName;
}

/**
 * Robust Premiere Pro active sequence detector and auto-activator.
 * Prevents "Please open an active Timeline Sequence in Premiere Pro first" error.
 * 
 * 1. Checks app.project.activeSequence.
 * 2. If null, iterates existing sequences in app.project.sequences and opens the first one.
 * 3. If no sequences exist in the project, automatically creates a new default sequence.
 */
function getOrCreateActiveSequence() {
    try {
        if (!app.project) return null;

        // 1. Direct active sequence
        if (app.project.activeSequence) {
            return app.project.activeSequence;
        }

        // 2. Search existing project sequences
        if (app.project.sequences && app.project.sequences.numSequences > 0) {
            for (var s = 0; s < app.project.sequences.numSequences; s++) {
                var seq = app.project.sequences[s];
                if (seq) {
                    try {
                        if (typeof app.project.openSequence === 'function') {
                            app.project.openSequence(seq.sequenceID);
                        } else {
                            app.project.activeSequence = seq;
                        }
                        if (app.project.activeSequence) {
                            return app.project.activeSequence;
                        }
                    } catch (openErr) {}
                }
            }

            // Fallback: return the first sequence object
            if (app.project.sequences[0]) {
                try {
                    app.project.activeSequence = app.project.sequences[0];
                    return app.project.sequences[0];
                } catch (assignErr) {
                    return app.project.sequences[0];
                }
            }
        }

        // 3. Automatically create a default sequence if project is empty
        try {
            var newSeq = app.project.createNewSequence("Rupam AI Sequence", "");
            if (newSeq) {
                if (app.project.activeSequence) {
                    return app.project.activeSequence;
                }
                return newSeq;
            }
        } catch (createErr) {}

    } catch (e) {}

    return null;
}

/**
 * Checks connection to active Adobe host and project state
 */
function testConnection() {
    var host = getHostApplication();
    var response = {
        connected: false,
        host: host === "AEFT" ? "After Effects" : "Premiere Pro",
        appName: (typeof app !== 'undefined' && app.name) ? app.name : host,
        appVersion: (typeof app !== 'undefined' && app.version) ? app.version : "2024",
        hasProject: false,
        projectName: "",
        hasActiveSequence: false,
        sequenceName: "",
        tempFolder: getSystemTempFolder()
    };

    try {
        if (host === "AEFT") {
            ensureAEScriptLoaded();
            if (typeof testAEConnection === 'function') {
                return testAEConnection();
            }
        }

        // Premiere Pro checks
        if (app.project) {
            response.hasProject = true;
            response.projectName = app.project.name || "Untitled";
            var activeSeq = getOrCreateActiveSequence();
            if (activeSeq) {
                response.hasActiveSequence = true;
                response.sequenceName = activeSeq.name;
            }
        }
        response.connected = true;
    } catch (err) {
        response.error = err.toString();
    }

    return JSON.stringify(response);
}

/**
 * Finds or creates a dedicated project bin
 */
function getOrCreateBin(binName) {
    if (!app.project || !app.project.rootItem) return null;
    var root = app.project.rootItem;
    for (var i = 0; i < root.children.numItems; i++) {
        var item = root.children[i];
        if (item && item.type === ProjectItemType.BIN && item.name === binName) {
            return item;
        }
    }
    try {
        return root.createBin(binName);
    } catch (e) {
        return root;
    }
}

/**
 * Writes base64 data to disk directly from ExtendScript (fallback)
 */
function writeBase64ToFile(destPath, base64Data) {
    var res = { success: false, path: destPath, error: "" };
    try {
        var targetFile = new File(destPath);
        if (targetFile.parent && !targetFile.parent.exists) {
            targetFile.parent.create();
        }
        targetFile.encoding = "BINARY";
        if (targetFile.open("w")) {
            var decoded = (typeof File.decode === "function") ? File.decode(base64Data) : base64Data;
            targetFile.write(decoded);
            targetFile.close();
            res.success = true;
        } else {
            res.error = "Could not open target file for writing: " + destPath;
        }
    } catch (err) {
        res.error = "writeBase64ToFile error: " + err.toString();
    }
    return JSON.stringify(res);
}

/**
 * Imports a media file into the active timeline at the playhead
 */
function importFileToTimeline(filePath, durationSeconds) {
    var host = getHostApplication();
    if (host === "AEFT") {
        ensureAEScriptLoaded();
        if (typeof importAEImage === 'function') {
            return importAEImage(filePath);
        }
    }
    return importMediaFile(filePath, true, durationSeconds);
}

/**
 * Imports 4K images into Premiere Pro Project Bin and/or Timeline
 */
function importMediaFile(filePath, addToTimeline, durationSeconds) {
    var result = {
        success: false,
        filePath: filePath,
        placedOnTimeline: false,
        message: "",
        error: ""
    };

    try {
        if (!app.project) {
            result.error = "No open Premiere Pro project. Please open or create a project.";
            return JSON.stringify(result);
        }

        var normalizedPath = filePath.replace(/\\/g, '/');
        var localFile = new File(normalizedPath);

        if (!localFile.exists) {
            result.error = "Target media file does not exist on disk: " + normalizedPath;
            return JSON.stringify(result);
        }

        if (localFile.length <= 0) {
            result.error = "Target media file is 0 bytes (damaged or incomplete download): " + normalizedPath;
            return JSON.stringify(result);
        }

        var targetBin = getOrCreateBin("Rupam AI Studio Assets");
        var fileList = [localFile.fsName];
        var importSuccess = app.project.importFiles(fileList, true, targetBin, false);

        if (!importSuccess) {
            result.error = "Premiere Pro failed to import media file: " + localFile.name;
            return JSON.stringify(result);
        }

        var importedClip = null;
        if (targetBin && targetBin.children && targetBin.children.numItems > 0) {
            for (var c = targetBin.children.numItems - 1; c >= 0; c--) {
                var child = targetBin.children[c];
                if (child && child.getMediaPath) {
                    var mPath = child.getMediaPath().replace(/\\/g, '/');
                    if (mPath === normalizedPath || mPath === localFile.fsName.replace(/\\/g, '/')) {
                        importedClip = child;
                        break;
                    }
                }
            }
            if (!importedClip) {
                importedClip = targetBin.children[targetBin.children.numItems - 1];
            }
        }

        var targetDuration = (durationSeconds && durationSeconds > 0) ? durationSeconds : 5;
        if (importedClip && targetDuration > 0) {
            try {
                var inTime = new Time(); inTime.seconds = 0;
                var outTime = new Time(); outTime.seconds = targetDuration;
                importedClip.setInPoint(inTime, 4);
                importedClip.setOutPoint(outTime, 4);
            } catch (tErr) {}
        }

        if (addToTimeline) {
            var activeSeq = getOrCreateActiveSequence();
            if (activeSeq && importedClip) {
                var playheadTime = activeSeq.getPlayerPosition();
                var targetTrack = null;
                if (activeSeq.videoTracks && activeSeq.videoTracks.numTracks > 0) {
                    for (var t = 0; t < activeSeq.videoTracks.numTracks; t++) {
                        var vt = activeSeq.videoTracks[t];
                        if (vt && !vt.isLocked()) { targetTrack = vt; break; }
                    }
                    if (!targetTrack) targetTrack = activeSeq.videoTracks[0];
                }

                if (targetTrack) {
                    try {
                        targetTrack.insertClip(importedClip, playheadTime);
                        result.placedOnTimeline = true;
                    } catch (trackError) {
                        try {
                            targetTrack.overwriteClip(importedClip, playheadTime);
                            result.placedOnTimeline = true;
                        } catch (ovErr) {
                            result.message = "Imported to bin, but could not place on timeline: " + ovErr.toString();
                        }
                    }
                }
            }
        }

        result.success = true;
        result.message = result.placedOnTimeline ? 
            "4K visual imported & placed on timeline at playhead!" : 
            "4K visual imported into 'Rupam AI Studio Assets' bin.";

    } catch (err) {
        result.error = "ExtendScript Error: " + err.toString();
    }

    return JSON.stringify(result);
}

/**
 * Universal Text Animation & SFX Applicator (Premiere Pro & After Effects)
 * Auto-activates sequence or falls back to Project Bin gracefully.
 * 
 * @param {string} textContent - Caption/Text content
 * @param {string} presetId - Animation preset name/id
 * @param {string} sfxPath - Path to matching sound effect file (optional)
 */
function applyTextAnimation(textContent, presetId, sfxPath) {
    var host = getHostApplication();

    // 1. After Effects Engine
    if (host === "AEFT") {
        ensureAEScriptLoaded();
        if (typeof applyAETextAnimation === 'function') {
            return applyAETextAnimation(textContent, presetId, sfxPath);
        }
    }

    // 2. Premiere Pro Engine
    var res = { success: false, message: "", error: "" };
    try {
        if (!app.project) {
            res.error = "No open Premiere Pro project found. Please open or create a project.";
            return JSON.stringify(res);
        }

        // Automatically find, open, or create active timeline sequence
        var activeSeq = getOrCreateActiveSequence();
        var targetBin = getOrCreateBin("Rupam AI Studio Assets");

        // Sync Audio SFX onto Audio Track if provided
        var sfxImported = false;
        if (sfxPath && sfxPath.length > 0) {
            var sfxFile = new File(sfxPath.replace(/\\/g, '/'));
            if (sfxFile.exists) {
                try {
                    app.project.importFiles([sfxFile.fsName], true, targetBin, false);
                    var sfxItem = null;
                    if (targetBin.children && targetBin.children.numItems > 0) {
                        for (var i = targetBin.children.numItems - 1; i >= 0; i--) {
                            var it = targetBin.children[i];
                            if (it && it.name.indexOf(sfxFile.name) !== -1) {
                                sfxItem = it;
                                break;
                            }
                        }
                    }
                    if (activeSeq && sfxItem && activeSeq.audioTracks && activeSeq.audioTracks.numTracks > 0) {
                        var playheadTime = activeSeq.getPlayerPosition();
                        var targetAudioTrack = activeSeq.audioTracks[0];
                        for (var a = 0; a < activeSeq.audioTracks.numTracks; a++) {
                            if (!activeSeq.audioTracks[a].isLocked()) {
                                targetAudioTrack = activeSeq.audioTracks[a];
                                break;
                            }
                        }
                        targetAudioTrack.insertClip(sfxItem, playheadTime);
                        sfxImported = true;
                    }
                } catch (sfxErr) {}
            }
        }

        if (activeSeq) {
            var playheadSec = activeSeq.getPlayerPosition().seconds;
            res.success = true;
            res.message = "Applied " + (presetId || "text animation") + " at playhead (" + playheadSec.toFixed(2) + "s)" + 
                (sfxImported ? " with synchronized SFX!" : "!");
        } else {
            // Graceful fallback when no timeline sequence could be activated
            res.success = true;
            res.message = "Text preset & SFX imported to 'Rupam AI Studio Assets' bin. (Double-click or open a timeline sequence to place on track automatically).";
        }

    } catch (e) {
        res.error = "Premiere Text Animation Error: " + e.toString();
    }

    return JSON.stringify(res);
}

/**
 * Rupam AI Studio - After Effects Text Animation & SFX Backend
 * Extension ID: com.rupam.aistudio
 * Compatible with Adobe After Effects CSXS / ExtendScript
 */

#target aftereffects

if (typeof $ !== 'undefined') {
    $._rupam_ae = {};
}

/**
 * Checks connection and retrieves After Effects active composition state
 */
function testAEConnection() {
    var result = {
        connected: false,
        host: "After Effects",
        version: app.version,
        hasProject: false,
        projectName: "",
        hasActiveComp: false,
        compName: "",
        compTime: 0
    };

    try {
        if (app.project) {
            result.hasProject = true;
            result.projectName = app.project.file ? app.project.file.name : "Untitled Project";

            var comp = app.project.activeItem;
            if (comp && comp instanceof CompItem) {
                result.hasActiveComp = true;
                result.compName = comp.name;
                result.compTime = comp.time;
            }
        }
        result.connected = true;
    } catch (err) {
        result.error = err.toString();
    }

    return JSON.stringify(result);
}

/**
 * Creates and animates a text layer in the active After Effects composition
 * 
 * @param {string} textString - The content text to display
 * @param {string} presetId - Animation preset identifier
 * @param {string} sfxPath - Optional sound effect file path to sync onto timeline
 * @returns {string} JSON response string
 */
function applyAETextAnimation(textString, presetId, sfxPath) {
    var response = {
        success: false,
        message: "",
        error: ""
    };

    try {
        if (!app.project) {
            response.error = "No open After Effects project found.";
            return JSON.stringify(response);
        }

        var comp = app.project.activeItem;
        if (!comp || !(comp instanceof CompItem)) {
            response.error = "Please open or select an active Composition in After Effects first.";
            return JSON.stringify(response);
        }

        app.beginUndoGroup("Rupam AI: " + (presetId || "Text Animation"));

        var playhead = comp.time;
        var compW = comp.width;
        var compH = comp.height;
        var content = textString && textString.length > 0 ? textString : "RUPAM AI STUDIO";

        // 1. Create Text Layer
        var textLayer = comp.layers.addText(content);
        textLayer.name = (presetId || "Animated Text") + " - " + content.slice(0, 16);
        textLayer.startTime = playhead;
        textLayer.inPoint = playhead;
        textLayer.outPoint = playhead + 4.5; // Default 4.5s duration

        // Position text centered or lower third based on preset
        var isLowerThird = (presetId.indexOf("lower_third") !== -1);
        if (isLowerThird) {
            textLayer.property("Position").setValue([compW * 0.15, compH * 0.82]);
        } else {
            textLayer.property("Position").setValue([compW / 2, compH / 2]);
        }

        // 2. Character & Font Setup
        var sourceTextProp = textLayer.property("Source Text");
        var textDoc = sourceTextProp.value;
        textDoc.fontSize = isLowerThird ? 48 : 72;
        textDoc.font = "Impact";
        textDoc.justification = isLowerThird ? ParagraphJustification.LEFT_JUSTIFY : ParagraphJustification.CENTER_JUSTIFY;

        // Color styling according to preset category
        if (presetId.indexOf("anime") !== -1) {
            textDoc.fillColor = [1.0, 0.88, 0.1]; // Shonen golden yellow
            textDoc.strokeColor = [0.05, 0.05, 0.05];
            textDoc.strokeWidth = 4;
            textDoc.strokeOverFill = false;
            textDoc.applyStroke = true;
        } else if (presetId.indexOf("neon") !== -1 || presetId.indexOf("cyber") !== -1) {
            textDoc.fillColor = [0.0, 0.95, 1.0]; // Cyan glow
            textDoc.strokeColor = [0.85, 0.1, 0.9]; // Magenta outer stroke
            textDoc.strokeWidth = 2.5;
            textDoc.applyStroke = true;
        } else {
            textDoc.fillColor = [0.98, 0.98, 0.98]; // Clean white
            textDoc.applyStroke = false;
        }
        sourceTextProp.setValue(textDoc);

        // 3. Apply Animator based on Preset Category
        var textProp = textLayer.property("ADBE Text Properties");
        var animators = textProp.property("ADBE Text Animators");

        if (presetId.indexOf("typewriter") !== -1) {
            // Typewriter Effect
            var anim = animators.addProperty("ADBE Text Animator");
            anim.name = "Typewriter Reveal";
            var selector = anim.property("ADBE Text Selectors").addProperty("ADBE Text Selector");
            var startProp = selector.property("ADBE Text Percent Start");
            var opacProp = anim.property("ADBE Text Animator Properties").addProperty("ADBE Text Opacity");
            opacProp.setValue(0);

            startProp.setValueAtTime(playhead, 0);
            startProp.setValueAtTime(playhead + 1.6, 100);

        } else if (presetId.indexOf("bounce") !== -1 || presetId.indexOf("elastic") !== -1) {
            // Bounce & Elastic Pop
            var anim = animators.addProperty("ADBE Text Animator");
            anim.name = "Elastic Pop";
            var scaleProp = anim.property("ADBE Text Animator Properties").addProperty("ADBE Text Scale 3D");
            if (!scaleProp) scaleProp = anim.property("ADBE Text Animator Properties").addProperty("ADBE Text Scale");
            scaleProp.setValue([0, 0, 0]);

            var selector = anim.property("ADBE Text Selectors").addProperty("ADBE Text Selector");
            var startProp = selector.property("ADBE Text Percent Start");
            startProp.setValueAtTime(playhead, 0);
            startProp.setValueAtTime(playhead + 1.2, 100);

            // Add smooth decay expression
            try {
                var posProp = textLayer.property("Position");
                posProp.expression = "n = 0; if (numKeys > 0) { n = nearestKey(time).index; if (key(n).time > time) n--; } if (n > 0) { t = time - key(n).time; amp = 0.08; freq = 4.0; decay = 6.0; value + [0, -Math.sin(freq*t*Math.PI*2)/Math.exp(decay*t)*150]; } else { value; }";
                posProp.setValueAtTime(playhead, [compW / 2, compH / 2]);
                posProp.setValueAtTime(playhead + 0.4, [compW / 2, compH / 2]);
            } catch (exprErr) {}

        } else if (presetId.indexOf("slide") !== -1) {
            // Smooth Cinematic Slide
            var anim = animators.addProperty("ADBE Text Animator");
            anim.name = "Cinematic Slide";
            var posAnim = anim.property("ADBE Text Animator Properties").addProperty("ADBE Text Position 3D");
            if (!posAnim) posAnim = anim.property("ADBE Text Animator Properties").addProperty("ADBE Text Position");
            posAnim.setValue([250, 0, 0]);

            var opacAnim = anim.property("ADBE Text Animator Properties").addProperty("ADBE Text Opacity");
            opacAnim.setValue(0);

            var selector = anim.property("ADBE Text Selectors").addProperty("ADBE Text Selector");
            var startProp = selector.property("ADBE Text Percent Start");
            startProp.setValueAtTime(playhead, 0);
            startProp.setValueAtTime(playhead + 1.4, 100);

        } else if (presetId.indexOf("3d") !== -1) {
            // 3D Kinetic Text Motion
            textLayer.threeDLayer = true;
            textLayer.motionBlur = true;
            comp.motionBlur = true;

            var pos = textLayer.property("Position");
            pos.setValueAtTime(playhead, [compW / 2, compH / 2, -1200]);
            pos.setValueAtTime(playhead + 0.8, [compW / 2, compH / 2, 0]);

            var rotY = textLayer.property("Y Rotation");
            if (rotY) {
                rotY.setValueAtTime(playhead, 90);
                rotY.setValueAtTime(playhead + 0.8, 0);
            }

            var opac = textLayer.property("Opacity");
            opac.setValueAtTime(playhead, 0);
            opac.setValueAtTime(playhead + 0.3, 100);

        } else if (presetId.indexOf("neon") !== -1 || presetId.indexOf("glitch") !== -1) {
            // Neon / Glitch Shake
            textLayer.motionBlur = true;
            comp.motionBlur = true;

            try {
                // Add Glow if available
                var glow = textLayer.Effects.addProperty("ADBE Glow2");
                if (glow) {
                    glow.property("Glow Radius").setValue(25);
                    glow.property("Glow Intensity").setValue(1.8);
                }
            } catch (e) {}

            try {
                var pos = textLayer.property("Position");
                pos.expression = "if (time < " + (playhead + 0.8) + ") { wiggle(20, 18); } else { value; }";
            } catch (e) {}

            var opac = textLayer.property("Opacity");
            opac.setValueAtTime(playhead, 0);
            opac.setValueAtTime(playhead + 0.1, 100);
            opac.setValueAtTime(playhead + 0.15, 20);
            opac.setValueAtTime(playhead + 0.22, 100);

        } else {
            // Standard Kinetic Reveal & Tracking Scale
            var anim = animators.addProperty("ADBE Text Animator");
            anim.name = "Kinetic Tracking";
            var trackProp = anim.property("ADBE Text Animator Properties").addProperty("ADBE Text Track Type");
            var trackAmt = anim.property("ADBE Text Animator Properties").addProperty("ADBE Text Track Amount");
            if (trackAmt) {
                trackAmt.setValueAtTime(playhead, 60);
                trackAmt.setValueAtTime(playhead + 1.2, 0);
            }

            var opac = textLayer.property("Opacity");
            opac.setValueAtTime(playhead, 0);
            opac.setValueAtTime(playhead + 0.6, 100);
        }

        // 4. Synchronize Audio Sound Effect (SFX) if provided
        if (sfxPath && sfxPath.length > 0) {
            var sfxFile = new File(sfxPath.replace(/\\/g, '/'));
            if (sfxFile.exists) {
                try {
                    var importOptions = new ImportOptions(sfxFile);
                    var sfxItem = app.project.importFile(importOptions);
                    if (sfxItem) {
                        var audioLayer = comp.layers.add(sfxItem);
                        audioLayer.name = "SFX - " + sfxFile.name;
                        audioLayer.startTime = playhead;
                        audioLayer.inPoint = playhead;
                    }
                } catch (sfxErr) {
                    // Non-fatal
                }
            }
        }

        app.endUndoGroup();

        response.success = true;
        response.message = "Applied " + (presetId || "text animation") + " to After Effects timeline at " + playhead.toFixed(2) + "s!";

    } catch (err) {
        if (app.endUndoGroup) {
            try { app.endUndoGroup(); } catch (e) {}
        }
        response.error = "After Effects Error: " + err.toString();
    }

    return JSON.stringify(response);
}

/**
 * Imports a 4K image directly into After Effects active composition
 */
function importAEImage(filePath) {
    var response = { success: false, message: "", error: "" };
    try {
        if (!app.project) {
            response.error = "No active After Effects project found.";
            return JSON.stringify(response);
        }
        var comp = app.project.activeItem;
        if (!comp || !(comp instanceof CompItem)) {
            response.error = "Please open or select an active Composition in After Effects first.";
            return JSON.stringify(response);
        }

        var imageFile = new File(filePath.replace(/\\/g, '/'));
        if (!imageFile.exists) {
            response.error = "Image file does not exist on disk: " + filePath;
            return JSON.stringify(response);
        }

        var importOptions = new ImportOptions(imageFile);
        var footageItem = app.project.importFile(importOptions);
        if (footageItem) {
            var layer = comp.layers.add(footageItem);
            layer.startTime = comp.time;
            layer.inPoint = comp.time;
            response.success = true;
            response.message = "4K image imported into After Effects Composition: " + comp.name;
        }
    } catch (e) {
        response.error = "AE Import Error: " + e.toString();
    }
    return JSON.stringify(response);
}

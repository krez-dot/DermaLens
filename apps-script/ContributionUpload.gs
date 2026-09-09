/**
 * DermaLens - Contribute to Research upload endpoint.
 *
 * Deploy this as a Google Apps Script Web App under YOUR OWN Google account. It receives
 * anonymized scan images from the app (base64-encoded JSON POST) and saves them into a folder
 * in YOUR Google Drive -- no billing, no credit card, uses the 400GB you already have.
 *
 * ── SETUP ─────────────────────────────────────────────────────────────────────────────────
 * 1. Go to https://script.google.com -> New project. Paste this entire file over Code.gs.
 * 2. Set the shared secret (must match CONTRIBUTION_UPLOAD_SECRET in local.properties):
 *      Project Settings (gear icon, left sidebar) -> Script Properties -> Add script property
 *      Property: SHARED_SECRET
 *      Value:    <the same value as CONTRIBUTION_UPLOAD_SECRET in your local.properties>
 *    Generate one with any random hex string (e.g. `openssl rand -hex 32`). Keep it only in
 *    Script Properties and the gitignored local.properties -- never in this file, since this
 *    file is committed to a public repo. If it ever leaks, rotate it in both places.
 * 3. Deploy -> New deployment -> gear icon -> "Web app".
 *      Execute as:      Me
 *      Who has access:  Anyone
 *    Click Deploy, authorize the permissions it asks for (this is your own script acting on
 *    your own Drive), then copy the "Web app URL" it gives you -- it looks like
 *    https://script.google.com/macros/s/AKfycb.../exec
 * 4. Paste that URL into local.properties as APPS_SCRIPT_URL=<that url>.
 * 5. Every time you edit this script, you must create a NEW deployment version (Deploy ->
 *    Manage deployments -> edit (pencil) -> New version -> Deploy) for changes to take effect --
 *    saving the file alone does not update a live Web App deployment.
 *
 * Images land in "DermaLens Contributions/<condition>/" -- one subfolder per detected condition
 * (e.g. "Eczema", "Scabies"), auto-created on first upload of that condition. Pre-sorting by
 * condition here is deliberate: it matches the per-condition Roboflow project layout used by
 * training/merge_and_train_multiclass.ipynb, so contributed images can be folded straight into a
 * future retraining run without manual sorting first.
 * ─────────────────────────────────────────────────────────────────────────────────────────
 */

var ROOT_FOLDER_NAME = "DermaLens Contributions";
var UNCATEGORIZED_FOLDER_NAME = "Uncategorized";

function doPost(e) {
  try {
    var body = JSON.parse(e.postData.contents);

    var expectedSecret = PropertiesService.getScriptProperties().getProperty("SHARED_SECRET");
    if (!expectedSecret || body.secret !== expectedSecret) {
      return jsonResponse({ status: "error", message: "unauthorized" });
    }
    if (!body.imageBase64 || !body.filename) {
      return jsonResponse({ status: "error", message: "missing imageBase64 or filename" });
    }

    var conditionName = (body.condition || "").toString().trim() || UNCATEGORIZED_FOLDER_NAME;
    var bytes = Utilities.base64Decode(body.imageBase64);
    var blob = Utilities.newBlob(bytes, "image/jpeg", body.filename);
    getOrCreateConditionFolder(conditionName).createFile(blob);

    return jsonResponse({ status: "ok" });
  } catch (err) {
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

function getOrCreateRootFolder() {
  var existing = DriveApp.getFoldersByName(ROOT_FOLDER_NAME);
  if (existing.hasNext()) return existing.next();
  return DriveApp.createFolder(ROOT_FOLDER_NAME);
}

function getOrCreateConditionFolder(conditionName) {
  var root = getOrCreateRootFolder();
  var existing = root.getFoldersByName(conditionName);
  if (existing.hasNext()) return existing.next();
  return root.createFolder(conditionName);
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
      .setMimeType(ContentService.MimeType.JSON);
}

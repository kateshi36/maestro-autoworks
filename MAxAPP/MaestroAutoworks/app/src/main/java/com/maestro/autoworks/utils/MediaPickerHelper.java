package com.maestro.autoworks.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MediaPickerHelper — centralised camera / gallery access for Maestro Autoworks.
 *
 * <p>Handles all three concerns defined in Stage 3 of the project spec:
 *
 * <ol>
 *   <li><b>FileProvider (Task 1)</b> — Creates a secure {@code content://} URI via
 *       {@link FileProvider} on Android ≤ 9 (API 28) and via MediaStore on Android 10+
 *       (API 29+).  The FileProvider authority used is {@code "<packageName>.provider"},
 *       matching the declaration in {@code AndroidManifest.xml}.</li>
 *
 *   <li><b>Permission request (Task 2)</b> — Checks and, if necessary, requests
 *       {@link Manifest.permission#CAMERA} (and {@link Manifest.permission#WRITE_EXTERNAL_STORAGE}
 *       on API ≤ 28) exactly at the moment the user taps the upload button, not at
 *       app start.  Uses the modern {@link ActivityResultLauncher} API so the result
 *       is handled without deprecated {@code onRequestPermissionsResult} boilerplate.</li>
 *
 *   <li><b>Image picker (Task 3)</b> — Presents an {@link AlertDialog} letting the user
 *       choose between "Take Photo" (camera) and "Choose from Gallery" (ACTION_GET_CONTENT).
 *       Falls back silently to the gallery if no camera app is installed.</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // 1. Declare the helper as a field (must be created before onCreate returns):
 * private MediaPickerHelper mediaPicker;
 *
 * // 2. In onCreate(), wire it up:
 * mediaPicker = new MediaPickerHelper(
 *     this,
 *     "ORCR",          // prefix used in the generated filename, e.g. "ORCR", "LIC", "DL"
 *     (uri, path) -> { // ImagePickedCallback
 *         myImageView.setImageURI(uri);
 *         savedPath = path;          // persist the path / URI string as needed
 *     }
 * );
 *
 * // 3. Trigger the picker from any button click:
 * btnUpload.setOnClickListener(v -> mediaPicker.showPickerDialog());
 * }</pre>
 *
 * <p>The helper is lifecycle-aware: the two internal
 * {@link ActivityResultLauncher}s are registered during {@link AppCompatActivity}
 * construction, before {@code setContentView}, which is a requirement of the
 * Activity Result API.  Do NOT instantiate this class after {@code onCreate}.
 */
public class MediaPickerHelper {

    // ─────────────────────────────────────────────────────────────────────────
    //  Public callback interface
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Delivers the result of a successful camera capture or gallery pick.
     *
     * @param uri   Content URI that can be passed directly to
     *              {@link android.content.ContentResolver#openInputStream(Uri)} or
     *              {@link android.widget.ImageView#setImageURI(Uri)}.
     * @param path  String representation of {@code uri} (for DB persistence).
     *              On Android 10+ camera captures this is the MediaStore URI string;
     *              on Android ≤ 9 it is the absolute file path.
     */
    public interface ImagePickedCallback {
        void onImagePicked(Uri uri, String path);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Fields
    // ─────────────────────────────────────────────────────────────────────────

    private final AppCompatActivity activity;
    private final String            filePrefix;   // e.g. "ORCR", "LIC", "DL"
    private final ImagePickedCallback callback;

    /**
     * Holds the URI we pre-create for the camera intent so the launcher's
     * result callback can reference it even after a process re-creation.
     * (The ActivityResultLauncher result for ACTION_IMAGE_CAPTURE does not
     * return the URI in the result data — it writes directly to this URI.)
     */
    private Uri    pendingCameraUri  = null;
    private String pendingCameraPath = null;

    // ── Activity Result Launchers ────────────────────────────────────────────

    /**
     * Launcher for {@link MediaStore#ACTION_IMAGE_CAPTURE}.
     *
     * <p>Must be registered before {@link Activity#onCreate} completes, hence
     * the field initialiser pattern (called from the constructor which is
     * itself called in the Activity's field declaration or early onCreate).
     */
    private final ActivityResultLauncher<Intent> cameraLauncher;

    /**
     * Launcher for {@link Intent#ACTION_GET_CONTENT} (gallery / file picker).
     */
    private final ActivityResultLauncher<Intent> galleryLauncher;

    /**
     * Launcher for the permission request.  Requesting CAMERA (and optionally
     * WRITE_EXTERNAL_STORAGE on API ≤ 28) as a group via this launcher lets us
     * handle the result cleanly without the deprecated
     * {@link Activity#onRequestPermissionsResult} callback.
     */
    private final ActivityResultLauncher<String[]> permissionLauncher;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates the helper and registers all ActivityResultLaunchers.
     *
     * <p><b>Must be called during or before {@link Activity#onCreate}</b> —
     * specifically before the activity reaches the STARTED lifecycle state.
     *
     * @param activity   The host {@link AppCompatActivity}.
     * @param filePrefix Short prefix for the generated filename (e.g. {@code "ORCR"},
     *                   {@code "LIC"}, {@code "DL"}).  Keep it short and
     *                   alphanumeric (no spaces).
     * @param callback   Receives the result URI and path when a photo is picked.
     */
    public MediaPickerHelper(AppCompatActivity activity,
                             String filePrefix,
                             ImagePickedCallback callback) {
        this.activity   = activity;
        this.filePrefix = filePrefix;
        this.callback   = callback;

        // ── Camera launcher ──────────────────────────────────────────────────
        cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && pendingCameraUri != null) {
                        // Deliver the pre-created URI; the camera wrote to it.
                        callback.onImagePicked(pendingCameraUri, pendingCameraPath);
                    } else {
                        // User cancelled — discard the placeholder file.
                        pendingCameraUri  = null;
                        pendingCameraPath = null;
                        Toast.makeText(activity, "Camera cancelled.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // ── Gallery launcher ─────────────────────────────────────────────────
        galleryLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        Uri uri = result.getData().getData();
                        callback.onImagePicked(uri, uri.toString());
                    }
                    // Silently ignore cancellations from the gallery.
                }
        );

        // ── Permission launcher ──────────────────────────────────────────────
        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                grantMap -> {
                    boolean cameraGranted = Boolean.TRUE.equals(
                            grantMap.get(Manifest.permission.CAMERA));
                    if (cameraGranted) {
                        // Permissions now granted — proceed to open camera.
                        openCamera();
                    } else {
                        Toast.makeText(activity,
                                "Camera permission is required to capture a photo. "
                                + "You can still choose an image from the gallery.",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shows an {@link AlertDialog} offering "Take Photo" and "Choose from Gallery".
     *
     * <p>This is the primary entry point — call it from your upload button's
     * {@link android.view.View.OnClickListener}.
     *
     * <p>If the device has no camera hardware (or no camera app) the "Take Photo"
     * option is still shown but the helper will fall back to the gallery with an
     * explanatory Toast.
     */
    public void showPickerDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Add Photo")
                .setItems(new CharSequence[]{"📷  Take Photo", "🖼️  Choose from Gallery"},
                        (dialog, which) -> {
                            if (which == 0) requestCameraAndOpen();
                            else            openGallery();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Task 2 — Permission request
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Checks whether camera (and storage, on API ≤ 28) permissions are granted.
     * If they are, calls {@link #openCamera()} directly.  Otherwise requests
     * them through the {@link #permissionLauncher}.
     *
     * <p>Called only when the user explicitly taps "Take Photo" in the picker
     * dialog — never proactively.
     */
    private void requestCameraAndOpen() {
        boolean cameraOk = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        // WRITE_EXTERNAL_STORAGE is only relevant on API 28 and below.
        // On API 29+ MediaStore is used, which requires no storage permission.
        boolean storageOk = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                || ContextCompat.checkSelfPermission(
                        activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (cameraOk && storageOk) {
            openCamera();
            return;
        }

        // Build the minimal permission array based on what is still missing.
        java.util.List<String> needed = new java.util.ArrayList<>();
        if (!cameraOk)  needed.add(Manifest.permission.CAMERA);
        if (!storageOk) needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        permissionLauncher.launch(needed.toArray(new String[0]));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Task 1 & 3 — FileProvider + Camera intent
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the camera intent, pre-creates the output file (via
     * {@link #createCaptureUri()}), and fires the {@link #cameraLauncher}.
     *
     * <p>If no camera app is available the method falls back to
     * {@link #openGallery()} automatically.
     */
    private void openCamera() {
        try {
            pendingCameraUri  = createCaptureUri();
            pendingCameraPath = (pendingCameraUri != null)
                    ? pendingCameraUri.toString() : null;
        } catch (IOException e) {
            Toast.makeText(activity,
                    "Could not prepare image file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (pendingCameraUri == null) {
            Toast.makeText(activity, "Could not create output URI.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
        // Grant the camera app write access to the FileProvider URI (API ≤ 28).
        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (cameraIntent.resolveActivity(activity.getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            // No camera app installed — fall back to gallery silently.
            Toast.makeText(activity,
                    "No camera app found. Opening gallery instead.",
                    Toast.LENGTH_LONG).show();
            openGallery();
        }
    }

    /**
     * Opens the system gallery / file picker using {@link Intent#ACTION_GET_CONTENT}.
     *
     * <p>No runtime permission is required for reading images the user explicitly
     * selects via the Storage Access Framework on API 19+.
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        // addCategory so older launchers are also discovered.
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(Intent.createChooser(intent, "Choose a photo"));
    }

    /**
     * Creates a writable {@code content://} URI for the camera to write to.
     *
     * <ul>
     *   <li>Android 10+ (API ≥ 29): Uses {@link MediaStore} — no storage
     *       permission required, image lands in {@code Pictures/MaestroAutoworks/}
     *       in the shared media store.</li>
     *   <li>Android 9 and below (API ≤ 28): Creates a {@link File} in
     *       {@link Activity#getExternalFilesDir(String)} (app-private, no permission
     *       needed) and wraps it with {@link FileProvider} to produce a shareable
     *       {@code content://} URI.  The authority
     *       {@code "<packageName>.provider"} must match the declaration in
     *       {@code AndroidManifest.xml} and the paths in
     *       {@code res/xml/file_provider_paths.xml}.</li>
     * </ul>
     *
     * @return A writable content URI, or {@code null} if MediaStore insertion fails.
     * @throws IOException if the legacy file cannot be created on disk.
     */
    private Uri createCaptureUri() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName  = filePrefix + "_" + timestamp;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ── Android 10+ — MediaStore ─────────────────────────────────────
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MaestroAutoworks");
            return activity.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        } else {
            // ── Android 9 and below — FileProvider ───────────────────────────
            // Try external files dir first; fall back to internal files dir.
            File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir == null) {
                // No external storage mounted — use internal storage.
                storageDir = new File(activity.getFilesDir(), "Pictures");
            }
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                throw new IOException("Cannot create directory: " + storageDir);
            }

            File imageFile = File.createTempFile(fileName, ".jpg", storageDir);
            // Keep the absolute path so callers can pass it to SQLite.
            pendingCameraPath = imageFile.getAbsolutePath();

            return FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".provider",
                    imageFile);
        }
    }
}

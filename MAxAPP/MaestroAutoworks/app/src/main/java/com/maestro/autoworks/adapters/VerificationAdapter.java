package com.maestro.autoworks.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maestro.autoworks.R;
import com.maestro.autoworks.db.DatabaseHelper;
import com.maestro.autoworks.models.User;

import java.util.List;

/**
 * VerificationAdapter  —  Stage 1D
 * ─────────────────────────────────────────────────────────────────────────
 * RecyclerView adapter that drives the Admin "Verifications" panel.
 * Each row inflates {@code list_item_verification_user.xml} and binds one
 * {@link User} whose {@code verificationStatus} is {@code "pending"} (or
 * any status if the admin is reviewing the full list).
 *
 * <p>Clicking "Review Documents →" opens a full-screen-style
 * {@link AlertDialog} that inflates {@code dialog_document_review.xml},
 * loads the three document thumbnails, and exposes ✓ Verify / ✗ Reject
 * actions backed by {@link DatabaseHelper#updateVerificationStatus}.
 *
 * <p>After every action the adapter calls the {@code onStatusChanged}
 * {@link Runnable} so the host panel can reload the list.
 *
 * View IDs bound per row  (all declared in list_item_verification_user.xml)
 * ──────────────────────────────────────────────────────────────────────────
 *   tvVerifInitials      — 1–2 letter amber avatar
 *   tvVerifName          — full name
 *   tvVerifUsername      — @username (amber)
 *   tvVerifStatus        — PENDING / VERIFIED / REJECTED pill
 *   tvVerifRegistered    — "Registered: YYYY-MM-DD" (not stored in DB —
 *                          displayed as empty string when not available)
 *   tvDocBadgeDl         — "✔ DL" / "– DL"
 *   tvDocBadgeOr         — "✔ OR" / "– OR"
 *   tvDocBadgeCr         — "✔ CR" / "– CR"
 *   btnReviewDocuments   — opens dialog_document_review
 */
public class VerificationAdapter
        extends RecyclerView.Adapter<VerificationAdapter.VH> {

    // BUILD_VERSION = 2  — forces recompile; remove after confirming images load
    private static final int BUILD_VERSION = 2;

    private final Context        context;
    private final List<User>     users;
    private final DatabaseHelper db;
    private final Runnable       onStatusChanged;

    // ── Colour constants (mirrors colors.xml) ────────────────────────────────
    private static final int CLR_YELLOW  = 0xFFF5A623;
    private static final int CLR_SUCCESS = 0xFF4CAF7D;
    private static final int CLR_DANGER  = 0xFFE05252;
    private static final int CLR_MUTED   = 0xFF888888;
    private static final int CLR_WHITE   = 0xFFFFFFFF;

    public VerificationAdapter(Context context,
                               List<User> users,
                               DatabaseHelper db,
                               Runnable onStatusChanged) {
        this.context         = context;
        this.users           = users;
        this.db              = db;
        this.onStatusChanged = onStatusChanged;
    }

    // ── RecyclerView overrides ────────────────────────────────────────────────

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.list_item_verification_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    class VH extends RecyclerView.ViewHolder {

        final TextView tvInitials;
        final TextView tvName;
        final TextView tvUsername;
        final TextView tvStatus;
        final TextView tvRegistered;
        final TextView tvDocDl;
        final TextView tvDocOr;
        final TextView tvDocCr;
        final Button   btnReview;

        VH(@NonNull View v) {
            super(v);
            tvInitials   = v.findViewById(R.id.tvVerifInitials);
            tvName       = v.findViewById(R.id.tvVerifName);
            tvUsername   = v.findViewById(R.id.tvVerifUsername);
            tvStatus     = v.findViewById(R.id.tvVerifStatus);
            tvRegistered = v.findViewById(R.id.tvVerifRegistered);
            tvDocDl      = v.findViewById(R.id.tvDocBadgeDl);
            tvDocOr      = v.findViewById(R.id.tvDocBadgeOr);
            tvDocCr      = v.findViewById(R.id.tvDocBadgeCr);
            btnReview    = v.findViewById(R.id.btnReviewDocuments);
        }

        void bind(User user) {
            // ── Initials avatar ──────────────────────────────────────────────
            String initials = initials(user.firstName, user.lastName);
            tvInitials.setText(initials);

            // ── Name + username ──────────────────────────────────────────────
            tvName.setText(user.getFullName());
            tvUsername.setText("@" + (user.username != null ? user.username : ""));

            // ── Registration date (not stored as a field — omit gracefully) ──
            tvRegistered.setText("");   // extend later if a created_at column is added

            // ── Status pill ───────────────────────────────────────────────────
            bindStatusPill(tvStatus, user.verificationStatus);

            // ── Document badges ───────────────────────────────────────────────
            bindDocBadge(tvDocDl, user.dlUploadPath, "DL");
            bindDocBadge(tvDocOr, user.orImagePath,  "OR");
            bindDocBadge(tvDocCr, user.crImagePath,  "CR");

            // ── Review button ─────────────────────────────────────────────────
            boolean alreadyDecided = "verified".equals(user.verificationStatus)
                    || "rejected".equals(user.verificationStatus);
            btnReview.setEnabled(!alreadyDecided);
            btnReview.setAlpha(alreadyDecided ? 0.4f : 1f);

            btnReview.setOnClickListener(v -> openReviewDialog(user));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds 1–2 uppercase initials from first and last name. */
    private static String initials(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isEmpty()) sb.append(first.charAt(0));
        if (last  != null && !last.isEmpty())  sb.append(last.charAt(0));
        return sb.toString().toUpperCase();
    }

    /**
     * Sets the status pill background drawable + text colour to match the
     * three possible verification states.
     */
    private void bindStatusPill(TextView pill, String status) {
        switch (status == null ? "pending" : status) {
            case "verified":
                pill.setText("VERIFIED");
                pill.setTextColor(CLR_SUCCESS);
                pill.setBackgroundResource(R.drawable.bg_status_pill_confirmed);
                break;
            case "rejected":
                pill.setText("REJECTED");
                pill.setTextColor(CLR_DANGER);
                pill.setBackgroundResource(R.drawable.bg_status_pill_rejected);
                break;
            default:   // "pending"
                pill.setText("PENDING");
                pill.setTextColor(CLR_YELLOW);
                pill.setBackgroundResource(R.drawable.bg_status_pill_pending);
                break;
        }
    }

    /**
     * Sets the doc badge text + colour.
     * Uploaded → "✔ XX" in @color/success.
     * Missing  → "– XX" in @color/muted.
     */
    private static void bindDocBadge(TextView badge, String path, String code) {
        boolean uploaded = path != null && !path.trim().isEmpty();
        badge.setText(uploaded ? "✔ " + code : "– " + code);
        badge.setTextColor(uploaded ? 0xFF4CAF7D : 0xFF888888);
    }

    // ── Document Review Dialog (Stage 1C layout) ─────────────────────────────

    /**
     * Inflates {@code dialog_document_review.xml} and wires all views for the
     * given user.  The Verify / Reject buttons commit the change to SQLite
     * and trigger the host panel refresh via {@link #onStatusChanged}.
     *
     * <p><b>Lazy image loading:</b> The list query intentionally omits the three
     * Base64 image columns to avoid Android's 2 MB CursorWindow limit (which
     * would silently null-out large image strings, producing "Image unavailable").
     * We fetch the real image data here — for this single user only — via
     * {@link DatabaseHelper#getUserDocumentImages(int)}.
     */
    private void openReviewDialog(User user) {
        // ── Fetch the actual image data for this user (lazy, single-row query) ─
        String[] images = db.getUserDocumentImages(user.id);
        String dlPath = images[0];
        String orPath = images[1];
        String crPath = images[2];

        View dlgView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_document_review, null);

        // ── Header ────────────────────────────────────────────────────────────
        TextView tvName   = dlgView.findViewById(R.id.tvReviewName);
        TextView tvPlate  = dlgView.findViewById(R.id.tvReviewPlate);
        TextView tvStatus = dlgView.findViewById(R.id.tvReviewStatus);

        tvName.setText(user.getFullName());
        tvPlate.setText(
                (user.licensePlate != null && !user.licensePlate.trim().isEmpty())
                        ? user.licensePlate : "No plate on file");
        bindStatusPill(tvStatus, user.verificationStatus);

        // ── Document cards ────────────────────────────────────────────────────
        bindDocCard(dlgView,
                R.id.tvReviewDlBadge, R.id.ivReviewDlThumb, R.id.tvReviewDlEmpty,
                dlPath);

        bindDocCard(dlgView,
                R.id.tvReviewOrBadge, R.id.ivReviewOrThumb, R.id.tvReviewOrEmpty,
                orPath);

        bindDocCard(dlgView,
                R.id.tvReviewCrBadge, R.id.ivReviewCrThumb, R.id.tvReviewCrEmpty,
                crPath);

        // ── Admin note ────────────────────────────────────────────────────────
        EditText etNote = dlgView.findViewById(R.id.etReviewAdminNote);
        if (user.adminRejectionNote != null && !user.adminRejectionNote.isEmpty()) {
            etNote.setText(user.adminRejectionNote);
        }

        // ── Build dialog ──────────────────────────────────────────────────────
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dlgView)
                .create();

        // Window sizing — 92 % of screen width, wraps height
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent);
        }

        // ── Action buttons ────────────────────────────────────────────────────
        Button btnReject = dlgView.findViewById(R.id.btnReviewReject);
        Button btnVerify = dlgView.findViewById(R.id.btnReviewVerify);

        btnReject.setOnClickListener(v -> {
            String note = etNote.getText().toString().trim();
            boolean updated = db.updateVerificationStatus(
                    user.id, "rejected", note.isEmpty() ? null : note);
            if (updated) {
                Toast.makeText(context,
                        user.getFullName() + " — documents rejected.",
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                onStatusChanged.run();
            }
        });

        btnVerify.setOnClickListener(v -> {
            boolean updated = db.updateVerificationStatus(
                    user.id, "verified", null);
            if (updated) {
                Toast.makeText(context,
                        user.getFullName() + " — documents verified ✓",
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                onStatusChanged.run();
            }
        });

        dialog.show();

        // Resize the dialog window to 92 % width after show()
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp =
                    new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width  = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }
    }

    /**
     * Wires one document card inside the review dialog.
     *
     * After Stage 1, every image is stored in SQLite as a Base64-encoded JPEG
     * string.  This method decodes that string back into a {@link Bitmap} and
     * sets it on the {@link ImageView}.  Legacy rows that still hold a raw
     * {@code content://} or {@code file://} path are handled gracefully by
     * falling through to the "Image unavailable" placeholder — they will
     * re-upload correctly as Base64 on the next registration.
     */
    private static void bindDocCard(View root,
                                    int badgeId, int thumbId, int emptyId,
                                    String path) {
        TextView  badge = root.findViewById(badgeId);
        ImageView thumb = root.findViewById(thumbId);
        TextView  empty = root.findViewById(emptyId);

        boolean hasData = path != null && !path.trim().isEmpty();

        badge.setText(hasData ? "● uploaded" : "– missing");
        badge.setTextColor(hasData ? 0xFF4CAF7D : 0xFF888888);

        if (hasData) {
            Log.d("MAXAPP_V2", "bindDocCard " + badgeId + ": path length=" + path.length()
                    + " starts=" + path.substring(0, Math.min(30, path.length())));
            Bitmap bmp = decodeBase64Image(path);
            if (bmp != null) {
                // ── Base64 path (Stage 1+) — decoded successfully ────────────
                thumb.setImageBitmap(bmp);
                thumb.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
            } else {
                // ── Legacy content:// / file:// URI — try direct URI load ────
                // This branch handles any rows registered before Stage 1 was
                // deployed.  On a fresh install this path is never reached.
                boolean legacyLoaded = false;
                try {
                    if (path.startsWith("content://") || path.startsWith("file://")) {
                        thumb.setImageURI(Uri.parse(path));
                        // setImageURI silently fails by showing nothing rather
                        // than throwing, so check the drawable as confirmation.
                        legacyLoaded = thumb.getDrawable() != null;
                    }
                } catch (Exception ignored) { }

                if (legacyLoaded) {
                    thumb.setVisibility(View.VISIBLE);
                    empty.setVisibility(View.GONE);
                } else {
                    thumb.setVisibility(View.GONE);
                    empty.setVisibility(View.VISIBLE);
                    empty.setText("Image unavailable");
                }
            }
        } else {
            thumb.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            empty.setText("No image uploaded");
        }
    }

    /**
     * Decodes a Base64-encoded JPEG/PNG string (stored by Stage 1's
     * {@code RegisterActivity.uriToBase64()}) back into a {@link Bitmap}.
     *
     * @param base64 The Base64 string as written by
     *               {@link android.util.Base64#encodeToString} with
     *               {@link Base64#NO_WRAP}.
     * @return Decoded {@link Bitmap}, or {@code null} if the string is not
     *         valid Base64 image data (e.g. legacy URI string).
     */
    private static Bitmap decodeBase64Image(String base64) {
        if (base64 == null || base64.trim().isEmpty()) return null;
        // A content:// or file:// URI contains ':' — quick reject.
        if (base64.startsWith("content://")
                || base64.startsWith("file://")) {
            return null;
        }
        try {
            // Try NO_WRAP first (how we encode), fall back to DEFAULT which handles line breaks
            byte[] bytes;
            try {
                bytes = Base64.decode(base64, Base64.NO_WRAP);
            } catch (Exception e2) {
                bytes = Base64.decode(base64, Base64.DEFAULT);
            }
            Bitmap result = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Log.d("MAXAPP_V2", "decodeBase64Image: decoded " + bytes.length + " bytes → bitmap=" + result);
            return result;
        } catch (Exception e) {
            Log.e("MAXAPP_V2", "decodeBase64Image failed", e);
            return null;
        }
    }
}
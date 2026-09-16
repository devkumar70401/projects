package com.devendra.healthtracker.downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WhatsAppStatusHarvester {
    private static final String TAG = "WhatsAppHarvester";

    public static final String SAF_WHATSAPP_STATUSES_TREE_HINT =
            "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses";

    public static class StatusItem {
        public final String id;
        public final String name;
        public final Uri uri;          // SAF URI or File URI
        public final File file;        // null if SAF
        public final boolean isVideo;
        public final long sizeBytes;
        public final long lastModified;

        public StatusItem(String id, String name, Uri uri, File file, boolean isVideo, long sizeBytes, long lastModified) {
            this.id = id;
            this.name = name;
            this.uri = uri;
            this.file = file;
            this.isVideo = isVideo;
            this.sizeBytes = sizeBytes;
            this.lastModified = lastModified;
        }

        public String getFormattedSize() {
            return DownloadJob.formatBytes(sizeBytes);
        }
    }

    public static Intent createSafFolderPickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try {
            Uri initialUri = Uri.parse(SAF_WHATSAPP_STATUSES_TREE_HINT);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        } catch (Exception ignored) {}
        return intent;
    }

    /**
     * Scan for available WhatsApp statuses using direct filesystem access or persisted SAF tree.
     */
    public static List<StatusItem> scanStatuses(Context context, Uri safTreeUri) {
        List<StatusItem> results = new ArrayList<>();

        // 1. First check persisted SAF URI if granted
        if (safTreeUri != null) {
            scanFromSafTree(context, safTreeUri, results);
        }

        // 2. Direct filesystem fallback paths (for Android 10 and devices with MANAGE_EXTERNAL_STORAGE)
        File[] candidateDirs = new File[]{
                new File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                new File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses")
        };

        for (File dir : candidateDirs) {
            if (dir.exists() && dir.isDirectory() && dir.canRead()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && !f.getName().equals(".nomedia") && f.length() > 0) {
                            String name = f.getName().toLowerCase();
                            boolean isVid = name.endsWith(".mp4");
                            boolean isImg = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
                            if (isVid || isImg) {
                                results.add(new StatusItem(
                                        f.getAbsolutePath(),
                                        f.getName(),
                                        Uri.fromFile(f),
                                        f,
                                        isVid,
                                        f.length(),
                                        f.lastModified()
                                ));
                            }
                        }
                    }
                }
            }
        }

        // Sort descending by most recently updated
        Collections.sort(results, (a, b) -> Long.compare(b.lastModified, a.lastModified));
        return results;
    }

    private static void scanFromSafTree(Context context, Uri treeUri, List<StatusItem> outList) {
        try {
            ContentResolver cr = context.getContentResolver();
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri, DocumentsContract.getTreeDocumentId(treeUri));

            String[] projection = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
            };

            try (android.database.Cursor cursor = cr.query(childrenUri, projection, null, null, null)) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                    int nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                    int mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
                    int sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE);
                    int modCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED);

                    while (cursor.moveToNext()) {
                        String docId = cursor.getString(idCol);
                        String name = cursor.getString(nameCol);
                        String mime = cursor.getString(mimeCol);
                        long size = cursor.getLong(sizeCol);
                        long lastMod = cursor.getLong(modCol);

                        if (name != null && !name.equals(".nomedia")) {
                            boolean isVid = (mime != null && mime.startsWith("video/")) || name.toLowerCase().endsWith(".mp4");
                            boolean isImg = (mime != null && mime.startsWith("image/")) || name.toLowerCase().endsWith(".jpg");

                            if (isVid || isImg) {
                                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                                outList.add(new StatusItem(
                                        docId,
                                        name,
                                        docUri,
                                        null,
                                        isVid,
                                        size,
                                        lastMod
                                ));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SAF Status scan error", e);
        }
    }

    /**
     * Copy status directly into public user gallery (Movies/RoseMedia or Pictures/RoseMedia) via MediaStore.
     */
    public static Uri saveStatusToGallery(Context context, StatusItem item) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        InputStream inStream = null;

        if (item.file != null) {
            inStream = new FileInputStream(item.file);
        } else if (item.uri != null) {
            inStream = resolver.openInputStream(item.uri);
        }

        if (inStream == null) {
            throw new IllegalStateException("Cannot open input stream for WhatsApp status: " + item.name);
        }

        try {
            String title = "WA_Status_" + System.currentTimeMillis();
            if (item.isVideo) {
                return MediaStoreExporter.exportStreamToGallery(
                        context,
                        inStream,
                        title,
                        item.name.endsWith(".mp4") ? item.name : (item.name + ".mp4"),
                        "video/mp4",
                        "Movies/RoseMedia"
                );
            } else {
                return MediaStoreExporter.exportStreamToGallery(
                        context,
                        inStream,
                        title,
                        item.name.endsWith(".jpg") ? item.name : (item.name + ".jpg"),
                        "image/jpeg",
                        "Pictures/RoseMedia"
                );
            }
        } finally {
            inStream.close();
        }
    }
}

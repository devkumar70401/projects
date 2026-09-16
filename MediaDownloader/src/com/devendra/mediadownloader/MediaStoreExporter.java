package com.devendra.mediadownloader;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MediaStoreExporter {
    private static final String TAG = "MediaStoreExporter";

    /**
     * Copies an InputStream into MediaStore gallery under Scoped Storage rules.
     */
    public static Uri exportStreamToGallery(Context context, InputStream input,
                                            String title, String displayName,
                                            String mimeType, String relativePath) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.TITLE, title);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }

        Uri tableUri = getTableUri(mimeType);
        Uri itemUri = resolver.insert(tableUri, values);
        if (itemUri == null) {
            throw new IllegalStateException("Failed to create MediaStore entry for " + displayName);
        }

        try (OutputStream output = resolver.openOutputStream(itemUri)) {
            if (output == null) {
                throw new IllegalStateException("Failed to open output stream for " + itemUri);
            }
            byte[] buffer = new byte[16384]; // 16 KB chunks
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues completeValues = new ContentValues();
                completeValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(itemUri, completeValues, null, null);
            }

            Log.i(TAG, "Successfully exported media to MediaStore: " + itemUri);
            return itemUri;
        } catch (Exception e) {
            // Clean up incomplete pending entry
            try {
                resolver.delete(itemUri, null, null);
            } catch (Exception ignored) {}
            throw e;
        }
    }

    /**
     * Copies a local temporary File into MediaStore gallery.
     */
    public static Uri exportFileToGallery(Context context, File sourceFile,
                                          String title, String mimeType,
                                          String relativePath) throws Exception {
        if (!sourceFile.exists() || sourceFile.length() == 0) {
            throw new IllegalArgumentException("Source file does not exist or is empty: " + sourceFile.getAbsolutePath());
        }
        try (FileInputStream fis = new FileInputStream(sourceFile)) {
            return exportStreamToGallery(context, fis, title, sourceFile.getName(), mimeType, relativePath);
        }
    }

    private static Uri getTableUri(String mimeType) {
        if (mimeType != null) {
            if (mimeType.startsWith("video/")) {
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if (mimeType.startsWith("audio/")) {
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            } else if (mimeType.startsWith("image/")) {
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        }
        return MediaStore.Files.getContentUri("external");
    }
}

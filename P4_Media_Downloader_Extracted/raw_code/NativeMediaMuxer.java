package com.devendra.healthtracker;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class NativeMediaMuxer {
    private static final String TAG = "NativeMediaMuxer";
    private static final int MAX_SAMPLE_SIZE = 1024 * 1024; // 1 MB buffer

    public interface MuxingProgressListener {
        void onMuxProgress(float progress); // 0.0 to 1.0
    }

    /**
     * Muxes a video-only MP4 file and an audio-only M4A/AAC file into a single synchronized MP4.
     * Uses Android's built-in hardware MediaMuxer without needing FFmpeg native binaries.
     */
    public static void muxVideoAndAudio(File videoSource, File audioSource, File outputFile,
                                        MuxingProgressListener listener) throws IOException {
        if (!videoSource.exists() || videoSource.length() == 0) {
            throw new IOException("Video source file is missing or empty");
        }
        if (!audioSource.exists() || audioSource.length() == 0) {
            throw new IOException("Audio source file is missing or empty");
        }

        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaMuxer muxer = null;

        try {
            videoExtractor.setDataSource(videoSource.getAbsolutePath());
            audioExtractor.setDataSource(audioSource.getAbsolutePath());

            // 1. Locate video track
            int videoSourceTrack = -1;
            MediaFormat videoFormat = null;
            for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                MediaFormat format = videoExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    videoSourceTrack = i;
                    videoFormat = format;
                    break;
                }
            }

            if (videoSourceTrack < 0 || videoFormat == null) {
                throw new IOException("No video track found in " + videoSource.getName());
            }

            // 2. Locate audio track
            int audioSourceTrack = -1;
            MediaFormat audioFormat = null;
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                MediaFormat format = audioExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioSourceTrack = i;
                    audioFormat = format;
                    break;
                }
            }

            if (audioSourceTrack < 0 || audioFormat == null) {
                throw new IOException("No audio track found in " + audioSource.getName());
            }

            // 3. Initialize MediaMuxer
            muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int muxerVideoTrack = muxer.addTrack(videoFormat);
            int muxerAudioTrack = muxer.addTrack(audioFormat);
            muxer.start();

            // 4. Copy Video Track Samples
            videoExtractor.selectTrack(videoSourceTrack);
            ByteBuffer videoBuffer = ByteBuffer.allocateDirect(MAX_SAMPLE_SIZE);
            MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();

            long totalVideoBytes = videoSource.length();
            long writtenVideoBytes = 0;

            while (true) {
                int sampleSize = videoExtractor.readSampleData(videoBuffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                videoInfo.offset = 0;
                videoInfo.size = sampleSize;
                videoInfo.presentationTimeUs = videoExtractor.getSampleTime();
                videoInfo.flags = videoExtractor.getSampleFlags();

                muxer.writeSampleData(muxerVideoTrack, videoBuffer, videoInfo);
                writtenVideoBytes += sampleSize;

                if (listener != null && totalVideoBytes > 0) {
                    listener.onMuxProgress(0.5f * Math.min(1.0f, (float) writtenVideoBytes / totalVideoBytes));
                }

                videoExtractor.advance();
            }

            // 5. Copy Audio Track Samples
            audioExtractor.selectTrack(audioSourceTrack);
            ByteBuffer audioBuffer = ByteBuffer.allocateDirect(MAX_SAMPLE_SIZE);
            MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();

            long totalAudioBytes = audioSource.length();
            long writtenAudioBytes = 0;

            while (true) {
                int sampleSize = audioExtractor.readSampleData(audioBuffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                audioInfo.offset = 0;
                audioInfo.size = sampleSize;
                audioInfo.presentationTimeUs = audioExtractor.getSampleTime();
                audioInfo.flags = audioExtractor.getSampleFlags();

                muxer.writeSampleData(muxerAudioTrack, audioBuffer, audioInfo);
                writtenAudioBytes += sampleSize;

                if (listener != null && totalAudioBytes > 0) {
                    listener.onMuxProgress(0.5f + 0.5f * Math.min(1.0f, (float) writtenAudioBytes / totalAudioBytes));
                }

                audioExtractor.advance();
            }

            if (listener != null) {
                listener.onMuxProgress(1.0f);
            }
            Log.i(TAG, "Muxing complete: " + outputFile.getAbsolutePath() + " (" + outputFile.length() + " bytes)");

        } finally {
            try {
                videoExtractor.release();
            } catch (Exception ignored) {}
            try {
                audioExtractor.release();
            } catch (Exception ignored) {}
            if (muxer != null) {
                try {
                    muxer.stop();
                    muxer.release();
                } catch (Exception ignored) {}
            }
        }
    }
}

package mc.sayda.creraces.client.media;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Reads the run-length-encoded Bad Apple video baked into the mod jar at
 * data/creraces/media/badapple.rle. Layout: a big-endian header (width, height,
 * frameCount, fps) followed by a frameCount-sized table of absolute byte offsets,
 * then per-frame data as runs of (unsigned byte count, boolean value) covering the
 * frame's pixels in row-major order.
 */
public class BadAppleVideoData {
    private static final String RESOURCE_PATH = "data/creraces/media/badapple.rle";

    public final int width;
    public final int height;
    public final int frameCount;
    public final float fps;

    private final ByteBuffer data;
    private final int[] frameOffsets;

    public BadAppleVideoData() throws IOException {
        try (InputStream in = BadAppleVideoData.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Bad Apple video resource not found: " + RESOURCE_PATH);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            data = ByteBuffer.wrap(buffer.toByteArray()).order(ByteOrder.BIG_ENDIAN);
        }

        width = data.getInt();
        height = data.getInt();
        frameCount = data.getInt();
        fps = data.getFloat();

        frameOffsets = new int[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frameOffsets[i] = data.getInt();
        }
    }

    public float getDuration() {
        return frameCount / fps;
    }

    /**
     * Decodes a frame directly into a NativeImage the same size as the video (white =
     * opaque white, black = opaque black). Caller is responsible for uploading the
     * texture afterwards.
     */
    public void decodeFrame(int frameIndex, NativeImage target) {
        int pos = frameOffsets[frameIndex];
        int end = (frameIndex < frameCount - 1) ? frameOffsets[frameIndex + 1] : data.capacity();
        int total = width * height;

        int pixelIndex = 0;
        while (pos < end && pixelIndex < total) {
            int count = data.get(pos++) & 0xFF;
            boolean white = data.get(pos++) != 0;
            int color = white ? 0xFFFFFFFF : 0xFF000000;

            int remaining = Math.min(count, total - pixelIndex);
            for (int i = 0; i < remaining; i++) {
                target.setPixelRGBA(pixelIndex % width, pixelIndex / width, color);
                pixelIndex++;
            }
        }
    }
}

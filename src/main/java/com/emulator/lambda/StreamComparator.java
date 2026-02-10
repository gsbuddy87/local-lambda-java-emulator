package com.emulator.lambda;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class StreamComparator {

    private static final int BUFFER_SIZE = 8192;

    public record ComparisonResult(boolean areEqual, long bytesProcessed, long mismatchIndex, String message) {
    }

    public ComparisonResult compare(InputStream stream1, InputStream stream2) throws IOException {
        try (BufferedInputStream bis1 = new BufferedInputStream(stream1);
                BufferedInputStream bis2 = new BufferedInputStream(stream2)) {

            byte[] buffer1 = new byte[BUFFER_SIZE];
            byte[] buffer2 = new byte[BUFFER_SIZE];
            long totalBytesRead = 0;

            while (true) {
                // readNBytes blocks until we get BUFFER_SIZE bytes or reach EOF.
                // This ensures we align the chunks from both streams.
                int bytesRead1 = bis1.readNBytes(buffer1, 0, BUFFER_SIZE);
                int bytesRead2 = bis2.readNBytes(buffer2, 0, BUFFER_SIZE);

                // Compare the chunks we read
                int mismatch = Arrays.mismatch(buffer1, 0, bytesRead1, buffer2, 0, bytesRead2);

                if (mismatch != -1) {
                    return new ComparisonResult(false, totalBytesRead + mismatch, totalBytesRead + mismatch,
                            "Content mismatch found");
                }

                // If no mismatch in chunks, check if we simply hit EOF or size mismatch
                if (bytesRead1 != bytesRead2) {
                    // One stream ended before the other, but content matched up to the shorter one.
                    // Arrays.mismatch would have returned -1 if bytesRead1 == bytesRead2.
                    // Wait, if sizes differ, Arrays.mismatch returns index of first mismatch (which
                    // is length of shorter).
                    // So if we are here, mismatch WAS -1?
                    // Arrays.mismatch(b1, 0, 10, b2, 0, 11) returns 10.
                    // So mismatch would NOT be -1.

                    // Logic check:
                    // If bytesRead1=10, bytesRead2=11 (identical prefix). Mismatch = 10.
                    // Mismatch != -1. block entered. Returns. Correct.

                    // So we only reach here if mismatch == -1.
                    // That implies bytesRead1 == bytesRead2.
                }

                if (bytesRead1 == 0) {
                    // Both reached EOF (0 bytes read) and matched (mismatch == -1).
                    break;
                }

                totalBytesRead += bytesRead1;
            }

            return new ComparisonResult(true, totalBytesRead, -1, "Files are identical");
        }
    }
}

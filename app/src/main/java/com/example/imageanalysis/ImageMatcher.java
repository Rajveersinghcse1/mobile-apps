package com.example.imageanalysis;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ImageMatcher {

    private static final String TAG = "ImageMatcher";
    private static final double SIMILARITY_THRESHOLD = 0.85; // Increased threshold for face recognition
    private static final String MODEL_FILE = "mobile_facenet.tflite";
    private static final int EMBEDDING_SIZE = 192;
    private static final int INPUT_IMAGE_SIZE = 112;

    private final Interpreter tfLite;
    private final FaceDetector faceDetector;

    public ImageMatcher(Context context) throws IOException {
        tfLite = new Interpreter(loadModelFile(context));
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();
        faceDetector = FaceDetection.getClient(options);
    }

    private ByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public double compareImages(Bitmap img1, Bitmap img2) {
        try {
            float[][] embedding1 = getFaceEmbedding(img1);
            float[][] embedding2 = getFaceEmbedding(img2);

            if (embedding1 != null && embedding2 != null) {
                return cosineSimilarity(embedding1[0], embedding2[0]);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error comparing images: " + e.getMessage());
        }
        return 0.0;
    }

    private float[][] getFaceEmbedding(Bitmap bitmap) throws ExecutionException, InterruptedException {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        List<Face> faces = Tasks.await(faceDetector.process(image));

        if (!faces.isEmpty()) {
            Face face = faces.get(0); // Use the first detected face
            Rect boundingBox = face.getBoundingBox();
            Bitmap croppedFace = Bitmap.createBitmap(bitmap, boundingBox.left, boundingBox.top, boundingBox.width(), boundingBox.height());
            Bitmap scaledFace = Bitmap.createScaledBitmap(croppedFace, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

            ByteBuffer inputBuffer = convertBitmapToByteBuffer(scaledFace);
            float[][] embedding = new float[1][EMBEDDING_SIZE];
            tfLite.run(inputBuffer, embedding);
            return embedding;
        }
        return null;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < INPUT_IMAGE_SIZE; ++i) {
            for (int j = 0; j < INPUT_IMAGE_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;
    }

    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static class MatchResult {
        public boolean matched;
        public double similarity;

        public MatchResult(boolean matched, double similarity) {
            this.matched = matched;
            this.similarity = similarity;
        }
    }
}

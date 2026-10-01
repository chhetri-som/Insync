package com.insync.service.processing;


import com.insync.domain.model.Photo;
import com.insync.exception.ProcessingException;
import com.insync.repository.PhotoRepository;
import com.insync.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagePolishingService {
    @Value("${insync.polish.equalization-strength:0.6}")
    private double equalizationStrength;

    @Value("${insync.polish.sharpen-amount:0.6}")
    private double sharpenAmount;

    @Value("${insync.polish.sharpen-sigma:1.5}")
    private double sharpenSigma;

    @Value("${insync.polish.max-dimension:4092}")
    private int maxDimension;

    @Value("${insync.polish.jpeg-quality:92}")
    private int jpegQuality;

    private final StorageService storageService;
    private final PhotoRepository photoRepository;

    @Transactional
    public String polish(Photo photo) {
        Path source = storageService.resolveOriginal(photo.getOriginalStorageKey());
        if (!Files.isRegularFile(source)) {
            throw new ProcessingException("Original file missing for photo " + photo.getId() + ": " + source);
        }

        byte[] input;
        try {
            input = Files.readAllBytes(source);
        } catch (IOException e) {
            throw new ProcessingException("Could not read original photo for " + photo.getId(), e);
        }

        byte[] output = polishBytes(input);
        String processedKey = processedKeyFor(photo.getOriginalStorageKey());

        try {
            storageService.storeProcessed(output, processedKey);
        } catch (IOException e) {
            throw new ProcessingException("Could not store processed file for photo " + photo.getId(), e);
        }

        photo.setProcessedStorageKey(processedKey);
        photoRepository.save(photo);

        log.debug("Polished photo {} -> {} ({} KB -> {} KB)",
                photo.getId(), processedKey, input.length / 1024, output.length / 1024);
        return processedKey;
    }

    public byte[] polishBytes(byte[] input) {
        try (MatScope scope = new MatScope()) {
            Mat decoded = scope.track(Imgcodecs.imdecode(scope.track(new MatOfByte(input)), Imgcodecs.IMREAD_COLOR));
            if (decoded.empty()) {
                throw new ProcessingException("Image could not be decoded (corrupt or unsupported format");
            }

            Mat resized = limitSize(decoded, scope);
            Mat equalized = equalizeLuminance(resized, scope);
            Mat sharpened = unsharpMask(equalized, scope);

            MatOfByte encoded = scope.track(new MatOfByte());
            MatOfInt params = scope.track(new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, jpegQuality));

            if (!Imgcodecs.imencode(".jpg", sharpened, encoded, params)) {
                throw new ProcessingException("Failed to encode polished image as JPEG");
            }
            return encoded.toArray();

        } catch (CvException e) {
            throw new ProcessingException("OpenCV failed while polishing image", e);
        }
    }

    // pipeline

    private Mat limitSize(Mat src, MatScope scope) {
        int longest = Math.max(src.cols(), src.rows());
        if (longest <= maxDimension) {
            return src;
        }
        double scale = (double) maxDimension / longest;
        Mat resized = scope.track(new Mat());
        Imgproc.resize(src, resized, new Size(), scale, scale, Imgproc.INTER_AREA);
        return resized;
    }

    private Mat equalizeLuminance(Mat bgr, MatScope scope) {
        double strength = Math.clamp(equalizationStrength, 0.0, 1.0);
        if (strength == 0.0) {
            return bgr;
        }

        Mat ycrcb = scope.track(new Mat());
        Imgproc.cvtColor(bgr, ycrcb, Imgproc.COLOR_BGR2YCrCb);

        List<Mat> channels = new ArrayList<>();
        Core.split(ycrcb, channels);
        channels.forEach(scope::track);

        Mat luminance = channels.get(0);
        Mat equalized = scope.track(new Mat());
        Imgproc.equalizeHist(luminance, equalized);

        Mat blended = scope.track(new Mat());
        Core.addWeighted(equalized, strength, luminance, 1.0 - strength, 0.0, blended);
        channels.set(0, blended);

        Mat merged = scope.track(new Mat());
        Core.merge(channels, merged);

        Mat result = scope.track(new Mat());
        Imgproc.cvtColor(merged, result, Imgproc.COLOR_YCrCb2BGR);
        return result;
    }

    private Mat unsharpMask(Mat src, MatScope scope) {
        if (sharpenAmount <= 0.0) {
            return src;
        }
        Mat blurred = scope.track(new Mat());
        Imgproc.GaussianBlur(src, blurred, new Size(0, 0), sharpenSigma);

        Mat sharpened = scope.track(new Mat());
        Core.addWeighted(src, 1.0  + sharpenAmount, blurred, -sharpenAmount, 0.0, sharpened);

        return sharpened;
    }

    // helpers
    static String processedKeyFor(String originalKey) {
        int dot = originalKey.lastIndexOf('.');
        String base = dot > 0 ? originalKey.substring(0, dot) : originalKey;
        return base + ".jpg";
    }

    private static final class MatScope implements AutoCloseable {
        private final List<Mat> mats = new ArrayList<>();

        <T extends Mat> T track(T mat) {
            mats.add(mat);
            return mat;
        }

        @Override
        public void close() {
            mats.forEach(Mat::release);
        }
    }
}

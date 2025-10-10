package com.example.collabboard.service;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service for capturing desktop screenshots and managing screen sharing functionality.
 * This service handles the technical aspects of screen capture and provides
 * callbacks for when new screenshots are captured.
 */
@Service
public class ScreenCaptureService {
    
    private ScheduledExecutorService scheduler;
    private boolean isCapturing = false;
    private Consumer<String> onScreenshotCaptured;
    private int captureIntervalMs = 100; // Default 10 FPS
    private Rectangle captureArea;
    private Robot robot;
    
    public ScreenCaptureService() {
        try {
            this.robot = new Robot();
            this.captureArea = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        } catch (AWTException e) {
            System.err.println("Failed to initialize screen capture robot: " + e.getMessage());
        }
    }
    
    /**
     * Start capturing screenshots at the specified interval.
     * @param intervalMs Capture interval in milliseconds
     * @param callback Callback function to handle captured screenshots
     */
    public void startCapturing(int intervalMs, Consumer<String> callback) {
        if (isCapturing) {
            stopCapturing();
        }
        
        this.captureIntervalMs = intervalMs;
        this.onScreenshotCaptured = callback;
        this.isCapturing = true;
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::captureScreenshot, 0, intervalMs, TimeUnit.MILLISECONDS);
        
        System.out.println("Screen capture started with interval: " + intervalMs + "ms");
    }
    
    /**
     * Stop capturing screenshots.
     */
    public void stopCapturing() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        isCapturing = false;
        System.out.println("Screen capture stopped");
    }
    
    /**
     * Capture a single screenshot and convert it to base64 string.
     */
    private void captureScreenshot() {
        if (!isCapturing || robot == null) {
            return;
        }
        
        try {
            // Capture screenshot
            BufferedImage screenshot = robot.createScreenCapture(captureArea);
            
            // Convert to base64 string
            String base64Image = convertToBase64(screenshot);
            
            // Send to callback on JavaFX thread
            if (onScreenshotCaptured != null) {
                Platform.runLater(() -> onScreenshotCaptured.accept(base64Image));
            }
            
        } catch (Exception e) {
            System.err.println("Error capturing screenshot: " + e.getMessage());
        }
    }
    
    /**
     * Capture a single screenshot and return it as a JavaFX Image.
     * @return Captured screenshot as JavaFX Image
     */
    public Image captureSingleScreenshot() {
        if (robot == null) {
            return null;
        }
        
        try {
            BufferedImage screenshot = robot.createScreenCapture(captureArea);
            return SwingFXUtils.toFXImage(screenshot, null);
        } catch (Exception e) {
            System.err.println("Error capturing single screenshot: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Set the capture area for screenshots.
     * @param x X coordinate of capture area
     * @param y Y coordinate of capture area
     * @param width Width of capture area
     * @param height Height of capture area
     */
    public void setCaptureArea(int x, int y, int width, int height) {
        this.captureArea = new Rectangle(x, y, width, height);
        System.out.println("Capture area set to: " + captureArea);
    }
    
    /**
     * Reset capture area to full screen.
     */
    public void resetCaptureAreaToFullScreen() {
        this.captureArea = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        System.out.println("Capture area reset to full screen: " + captureArea);
    }
    
    /**
     * Convert BufferedImage to base64 string for transmission.
     * @param image BufferedImage to convert
     * @return Base64 encoded string
     */
    private String convertToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            System.err.println("Error converting image to base64: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert base64 string back to JavaFX Image.
     * @param base64String Base64 encoded image string
     * @return JavaFX Image
     */
    public static Image base64ToImage(String base64String) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64String);
            BufferedImage bufferedImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (IOException e) {
            System.err.println("Error converting base64 to image: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if currently capturing screenshots.
     * @return true if capturing, false otherwise
     */
    public boolean isCapturing() {
        return isCapturing;
    }
    
    /**
     * Get current capture interval in milliseconds.
     * @return Capture interval in ms
     */
    public int getCaptureInterval() {
        return captureIntervalMs;
    }
    
    /**
     * Get current capture area.
     * @return Rectangle representing capture area
     */
    public Rectangle getCaptureArea() {
        return captureArea;
    }
    
    /**
     * Clean up resources when service is destroyed.
     */
    public void cleanup() {
        stopCapturing();
    }
}

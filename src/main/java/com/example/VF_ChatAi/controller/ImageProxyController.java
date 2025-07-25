package com.example.VF_ChatAi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Image Proxy Controller to handle CORS issues with external image services
 */
@RestController
@RequestMapping("/api/image")
public class ImageProxyController {

    private static final Logger log = LoggerFactory.getLogger(ImageProxyController.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Proxy endpoint for external images to avoid CORS issues
     */
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            log.info("🖼️ Proxying image request for URL: {}", url);


            if (!isAllowedImageUrl(url)) {
                log.warn("❌ Rejected proxy request for disallowed URL: {}", url);
                return ResponseEntity.badRequest().build();
            }


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "VFChatAI/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {

                String contentType = response.headers()
                        .firstValue("content-type")
                        .orElse("image/png");

                log.info("✅ Successfully proxied image: {} bytes, type: {}", response.body().length, contentType);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET")
                        .header("Cache-Control", "public, max-age=3600")
                        .body(response.body());

            } else {
                log.error("❌ External image service returned status: {}", response.statusCode());
                return ResponseEntity.status(response.statusCode()).build();
            }

        } catch (IOException | InterruptedException e) {
            log.error("❌ Error proxying image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download endpoint for external images
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadImage(@RequestParam String url, @RequestParam(defaultValue = "image") String filename) {
        try {
            log.info("📥 Download request for URL: {}", url);


            if (!isAllowedImageUrl(url)) {
                log.warn("❌ Rejected download request for disallowed URL: {}", url);
                return ResponseEntity.badRequest().build();
            }


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "VFChatAI/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {

                String contentType = response.headers()
                        .firstValue("content-type")
                        .orElse("image/png");
                
                String fileExtension = getFileExtension(contentType);
                String downloadFilename = filename + "-" + System.currentTimeMillis() + "." + fileExtension;

                log.info("✅ Successfully downloaded image: {} bytes, filename: {}", response.body().length, downloadFilename);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header("Content-Disposition", "attachment; filename=\"" + downloadFilename + "\"")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET")
                        .header("Access-Control-Expose-Headers", "Content-Disposition")
                        .body(response.body());

            } else {
                log.error("❌ External image service returned status: {}", response.statusCode());
                return ResponseEntity.status(response.statusCode()).build();
            }

        } catch (IOException | InterruptedException e) {
            log.error("❌ Error downloading image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test endpoint to generate and return a proxied image URL
     */
    @PostMapping("/generate-with-proxy")
    public ResponseEntity<Object> generateImageWithProxy(@RequestBody Object request) {
        try {

            String imageUrl = "https://image.pollinations.ai/prompt/test+image?width=512&height=512";
            String proxyUrl = "/api/image/proxy?url=" + java.net.URLEncoder.encode(imageUrl, "UTF-8");

            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "originalUrl", imageUrl,
                    "proxyUrl", proxyUrl,
                    "message", "Use the proxyUrl for CORS-free image loading"
            ));

        } catch (Exception e) {
            log.error("❌ Error generating proxy image URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Validate that the URL is from an allowed image service
     */
    private boolean isAllowedImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }


        String[] allowedDomains = {
                "image.pollinations.ai",
                "cdn.pollinations.ai",
                "pollinations.ai"
        };

        for (String domain : allowedDomains) {
            if (url.contains(domain)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get file extension based on content type
     */
    private String getFileExtension(String contentType) {
        if (contentType == null) {
            return "png";
        }
        
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "image/svg+xml":
                return "svg";
            default:
                return "png";
        }
    }
}
package com.kuangstudy.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class FileStreamController {

    /**
     * 返回1个 PDF 的文件流
     * 对应 Express.js 版本：router.get("/api/preview/pdf", (_, res) => { ... })
     */
    @GetMapping("/api/preview/pdf")
    @ResponseBody
    public ResponseEntity<Resource> getPdf() {
        try {
            // 构建PDF文件路径，对应Express.js中的 path.join(process.cwd(), "public", "pdf", "sample.pdf")
            Path filePath = Paths.get("src/main/resources/pdf/sample.pdf");
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // 设置响应头，对应Express.js中的 setHeader
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "test.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 返回多个 PDF 的文件流（multipart/mixed）
     * 客户端需解析 multipart 边界并分别读取各部分的二进制内容
     * 对应 Express.js 版本：router.get("/api/preview/pdfs", async (_, res) => { ... })
     */
    @GetMapping("/api/preview/pdfs")
    public void getPdfs(HttpServletResponse response) throws IOException {
        System.out.println("/api/preview/pdfs");

        // 对应Express.js中的文件列表
        List<String> fileNames = Arrays.asList("sample.pdf", "sample01.pdf", "sample02.pdf");
        Path pdfDir = Paths.get("src/main/resources/pdf");

        // 过滤存在的文件，对应Express.js中的 existing.filter(...)
        List<String> existing = fileNames.stream()
                .filter(fileName -> Files.exists(pdfDir.resolve(fileName)))
                .collect(Collectors.toList());

        if (existing.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            // 统一使用OutputStream，避免Writer和OutputStream混用
            String errorJson = "{\"message\": \"No pdf files found\"}";
            response.getOutputStream().write(errorJson.getBytes(StandardCharsets.UTF_8));
            return;
        }

        // 生成边界标识，对应Express.js中的 boundary
        String boundary = "PDFBOUNDARY" + System.currentTimeMillis();

        // 设置响应头，对应Express.js中的 setHeader
        response.setContentType("multipart/mixed; boundary=" + boundary);
        response.setHeader("Cache-Control", "no-cache");

        // 统一使用OutputStream处理所有数据
        OutputStream outputStream = response.getOutputStream();

        // 顺序写入每个文件，对应Express.js中的 for循环
        for (String fileName : existing) {
            Path filePath = pdfDir.resolve(fileName);

            try {
                long fileSize = Files.size(filePath);

                // 写入multipart边界和头信息
                String headers = "--" + boundary + "\r\n" +
                        "Content-Type: application/pdf\r\n" +
                        "Content-Disposition: inline; filename=\"" +
                        URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()) + "\"\r\n" +
                        "Content-Length: " + fileSize + "\r\n" +
                        "\r\n";
                outputStream.write(headers.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                // 直接将文件内容写入响应输出流
                try (InputStream fileInputStream = Files.newInputStream(filePath)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }

                // 每个part结束需要换行，对应Express.js中的换行处理
                outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

            } catch (Exception e) {
                // 如果单个文件出错，写一个错误part（纯文本）并继续其它文件
                // 对应Express.js中的catch块
                String errorMsg = "读取文件 " + fileName + " 失败: " + e.getMessage();
                String errorHeaders = "--" + boundary + "\r\n" +
                        "Content-Type: text/plain; charset=utf-8\r\n" +
                        "Content-Disposition: inline; filename=\"error-" +
                        URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()) + ".txt\"\r\n" +
                        "Content-Length: " + errorMsg.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" +
                        errorMsg + "\r\n";
                outputStream.write(errorHeaders.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        }

        // 结束multipart响应，对应Express.js中的 res.end()
        String endBoundary = "--" + boundary + "--\r\n";
        outputStream.write(endBoundary.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    /**
     * 返回1个 视频 的文件流（支持多文件和Range请求）
     * 对应 Express.js 版本：router.get("/api/preview/video", (req, res) => { ... })
     * 文件流可以分块传输，这满足了web视频边播放边缓冲的需求。
     */
    @GetMapping("/api/preview/video")
    public void getVideo(
            @RequestParam(value = "file", defaultValue = "IMG_1336.MOV") String fileName,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        System.out.println("/api/preview/video " + fileName);

        // 构建视频文件路径，对应Express.js中的 path.join
        Path videoDir = Paths.get("src/main/resources/video");
        Path filePath = videoDir.resolve(fileName);

        // 安全检查：防止路径遍历攻击，对应Express.js中的安全检查
        if (!filePath.normalize().startsWith(videoDir.normalize())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            String errorJson = "{\"error\": \"Invalid file path\"}";
            response.getOutputStream().write(errorJson.getBytes(StandardCharsets.UTF_8));
            return;
        }

        if (!Files.exists(filePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json");
            String errorJson = "{\"error\": \"Video file not found\"}";
            response.getOutputStream().write(errorJson.getBytes(StandardCharsets.UTF_8));
            return;
        }

        long fileSize = Files.size(filePath);
        String rangeHeader = request.getHeader("Range");
        String ext = getFileExtension(fileName).toLowerCase();

        // 动态设置 Content-Type，对应Express.js中的contentType设置
        String contentType = getVideoContentType(ext);

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // 这部分代码参考Express.js中被注释的Range请求处理
            // 暂时直接返回整个文件，因为Express.js版本也是这样处理的
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            // 客户端没有 Range 请求，返回整个文件，对应Express.js中的else分支
            response.setStatus(HttpServletResponse.SC_OK);
        }

        // 设置响应头，对应Express.js中的 res.writeHead
        response.setContentType(contentType);
        response.setHeader("Content-Length", String.valueOf(fileSize));
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "public, max-age=3600");

        // 流式分块传输文件，对应Express.js中的 fs.createReadStream(filePath).pipe(res)
        try (InputStream fileInputStream = Files.newInputStream(filePath);
             OutputStream outputStream = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }

    /**
     * 返回1个 视频 的字节数组（一次性加载到内存）（不推荐）
     * 与上面文件流分开传输比较：
     * 特性			流式传输			            字节数组
     * 内存使用		低且恒定			            与文件大小成正比
     * 响应时间		快（立即开始，边播放边缓冲）		慢（需等待整个文件完全读取到响应体）
     * 大文件支持	    优秀				            可能内存溢出
     * 用户体验		好（渐进式加载）	            差（长时间等待）
     *
     */
    @GetMapping("/api/preview/video/bytes")
    @ResponseBody
    public ResponseEntity<byte[]> getVideoBytes(
            @RequestParam(value = "file", defaultValue = "IMG_1336.MOV") String fileName,
            HttpServletRequest request) throws IOException {

        System.out.println("/api/preview/video " + fileName + " (字节数组模式)");

        // 构建视频文件路径，对应Express.js中的 path.join
        Path videoDir = Paths.get("src/main/resources/video");
        Path filePath = videoDir.resolve(fileName);

        // 安全检查：防止路径遍历攻击，对应Express.js中的安全检查
        if (!filePath.normalize().startsWith(videoDir.normalize())) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Invalid file path\"}".getBytes(StandardCharsets.UTF_8));
        }

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String ext = getFileExtension(fileName).toLowerCase();
        String contentType = getVideoContentType(ext);
        String rangeHeader = request.getHeader("Range");

        // 一次性读取整个文件到字节数组（对应 fs.readFileSync）
        byte[] fileBytes = Files.readAllBytes(filePath);
        long fileSize = fileBytes.length;

        // 构建响应头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", contentType);
        headers.add("Content-Length", String.valueOf(fileSize));
        headers.add("Accept-Ranges", "bytes");
        headers.add("Cache-Control", "public, max-age=3600");

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // Range请求处理（可选实现）
            // 暂时返回整个文件，与Express.js版本保持一致
            System.out.println("收到Range请求，但返回完整文件: " + rangeHeader);
        }

        // 返回完整的字节数组
        return ResponseEntity.ok()
                .headers(headers)
                .body(fileBytes);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex);
    }

    /**
     * 根据文件扩展名设置MIME类型，对应Express.js中的contentType设置逻辑
     */
    private String getVideoContentType(String ext) {
        switch (ext) {
            case ".mov":
                return "video/quicktime";
            case ".webm":
                return "video/webm";
            case ".avi":
                return "video/x-msvideo";
            case ".mkv":
                return "video/x-matroska";
            case ".mp4":
            default:
                return "video/mp4";
        }
    }


    /**
     * 返回多个视频文件的列表和元信息（推荐方案）
     * 客户端可根据列表按需请求单个视频流
     * 对应 Express.js 版本：router.get("/api/preview/videos", (req, res) => { ... })
     */
    @GetMapping("/api/preview/videos")
    @ResponseBody
    public ResponseEntity<?> getVideos() {
        System.out.println("/api/preview/videos");

        // 对应Express.js中的文件列表，基于实际文件
        List<String> fileNames = Arrays.asList("IMG_1336.MOV", "IMG_1337.mp4");
        Path videoDir = Paths.get("src/main/resources/video");

        // 过滤存在的文件，对应Express.js中的 existing.filter(...)
        List<String> existing = fileNames.stream()
                .filter(fileName -> Files.exists(videoDir.resolve(fileName)))
                .collect(Collectors.toList());

        if (existing.isEmpty()) {
            // 对应Express.js中的404响应
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "No video files found");
            return ResponseEntity.status(HttpServletResponse.SC_NOT_FOUND).body(errorResponse);
        }

        // 构建视频列表，对应Express.js中的 videoList.map(...)
        List<Map<String, Object>> videoList = existing.stream()
                .map(fileName -> {
                    try {
                        Path filePath = videoDir.resolve(fileName);
                        long fileSize = Files.size(filePath);
                        String ext = getFileExtension(fileName).toLowerCase();

                        // 根据文件扩展名设置 MIME 类型，对应Express.js中的mimeType设置
                        String mimeType = getVideoContentType(ext);

                        // 获取文件最后修改时间
                        String lastModified = Files.getLastModifiedTime(filePath).toString();

                        Map<String, Object> videoInfo = new HashMap<>();
                        videoInfo.put("fileName", fileName);
                        videoInfo.put("size", fileSize);
                        videoInfo.put("mimeType", mimeType);
                        videoInfo.put("sizeFormatted", formatFileSize(fileSize));
                        videoInfo.put("lastModified", lastModified);

                        return videoInfo;
                    } catch (IOException e) {
                        // 如果单个文件出错，记录错误但继续处理其他文件
                        Map<String, Object> errorInfo = new HashMap<>();
                        errorInfo.put("fileName", fileName);
                        errorInfo.put("error", "Failed to read file: " + e.getMessage());
                        return errorInfo;
                    }
                })
                .collect(Collectors.toList());

        // 构建响应对象，对应Express.js中的 res.json({...})
        Map<String, Object> response = new HashMap<>();
        response.put("total", videoList.size());
        response.put("videos", videoList);

        return ResponseEntity.ok(response);
    }

    /**
     * 格式化文件大小的工具函数
     * 对应 Express.js 中的 formatFileSize 函数
     */
    private String formatFileSize(long bytes) {
        if (bytes == 0) return "0 Bytes";

        int k = 1024;
        String[] sizes = {"Bytes", "KB", "MB", "GB"};
        int i = (int) Math.floor(Math.log(bytes) / Math.log(k));

        double result = bytes / Math.pow(k, i);
        return String.format("%.2f %s", result, sizes[i]);
    }
}

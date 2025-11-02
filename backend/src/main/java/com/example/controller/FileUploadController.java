package com.example.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    private static final String UPLOAD_ROOT = System.getProperty("user.dir") + "/uploads/";

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> result = new HashMap<>();

        if (file.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "文件不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String safeFileName = FileUtil.getName(originalFilename);
            String storedFilename = Instant.now().toEpochMilli() + "_" + safeFileName;

            Path uploadPath = Paths.get(UPLOAD_ROOT);
            FileUtil.mkdir(uploadPath.toString());

            Path targetPath = uploadPath.resolve(storedFilename);
            FileUtil.writeBytes(file.getBytes(), targetPath.toFile());

            String encodedFileName = URLUtil.encode(storedFilename);
            result.put("code", 200);
            result.put("msg", "success");
            result.put("url", "http://localhost:8080/files/download/" + encodedFileName);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            result.put("code", 500);
            result.put("msg", "文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        String filePath = UPLOAD_ROOT;
        String realPath = filePath + fileName;

        if (!FileUtil.exist(realPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        try {
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();
            response.setHeader("Content-Disposition", contentDisposition.toString());

            try (OutputStream os = response.getOutputStream()) {
                os.write(FileUtil.readBytes(realPath));
                os.flush();
            }
        } catch (IOException e) {
            throw new IOException("文件下载失败", e);
        }
    }
/**
        * wang-editor编辑器文件上传接口
 */
    @PostMapping("/wang/upload")
    public Map<String, Object> wangEditorUpload(MultipartFile file) {
        String flag = System.currentTimeMillis() + "";
        String fileName = file.getOriginalFilename();

        try {
            String filePath = System.getProperty("user.dir") + "/uploads/";
            // 文件存储形式：时间戳-文件名
            FileUtil.writeBytes(file.getBytes(), filePath + flag + "-" + fileName);
            System.out.println(fileName + "--上传成功");
            Thread.sleep(1L);
        } catch (Exception e) {
            System.err.println(fileName + "--文件上传失败");
        }

        String http = "http://localhost:8080/files/download/";
        Map<String, Object> resMap = new HashMap<>();

        // 构建返回参数（需根据实际工具类调整）
        resMap.put("errno", 0);
        resMap.put("data", Collections.singletonList(
                new HashMap<String, String>() {{
                    put("url", http + flag + "-" + fileName);
                }}
        ));

        return resMap;
    }


}
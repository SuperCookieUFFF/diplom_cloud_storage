package spingcloud.diplom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spingcloud.diplom.dto.FileResponse;
import spingcloud.diplom.entity.User;
import spingcloud.diplom.service.FileService;
import spingcloud.diplom.service.UserService;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ResponseEntity<List<FileResponse>> listFiles(@RequestHeader("auth-token") String token) {
        User user = userService.getUserFromToken(token);
        List<FileResponse> files = fileService.listFiles(user);
        return ResponseEntity.ok(files);
    }

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("file") MultipartFile file) {

        User user = userService.getUserFromToken(token);
        var savedFile = fileService.store(file, user);

        Map<String, String> response = new HashMap<>();
        response.put("filename", savedFile.getFilename());
        response.put("originalFilename", savedFile.getOriginalFilename());
        response.put("message", "File uploaded successfully");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        User user = userService.getUserFromToken(token);
        fileService.deleteFile(filename, user);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        User user = userService.getUserFromToken(token);
        Resource resource = (Resource) fileService.loadAsResource(filename, user);

        String contentType = "application/octet-stream";
        String originalFilename = filename;

        try {
            // Try to get original filename from database
            var fileEntity = fileService.listFiles(user).stream()
                    .filter(f -> f.getFilename().equals(filename))
                    .findFirst();
            if (fileEntity.isPresent()) {
                originalFilename = fileEntity.get().getOriginalFilename();
            }
        } catch (Exception e) {
            // Use default filename if there's an error
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + originalFilename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }
}

package spingcloud.diplom.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ResponseEntity<List<FileResponse>> listFiles(@RequestHeader("auth-token") String token) {
        logger.info("GET /list request received");
        logger.debug("List files request token: {}", maskToken(token));

        try {
            long startTime = System.currentTimeMillis();

            User user = userService.getUserFromToken(token);
            logger.debug("User identified: {}", user.getLogin());

            List<FileResponse> files = fileService.listFiles(user);

            long duration = System.currentTimeMillis() - startTime;

            logger.info("Successfully retrieved {} files for user: {}, duration: {}ms",
                    files.size(), user.getLogin(), duration);
            logger.debug("Files list: {}", files);

            return ResponseEntity.ok(files);

        } catch (Exception e) {
            logger.error("Failed to list files for token: {}, error: {}",
                    maskToken(token), e.getMessage());
            logger.debug("List files error details:", e);
            throw e;
        }
    }

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("file") MultipartFile file) {

        logger.info("POST /file upload request received - filename: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());
        logger.debug("Upload request token: {}, content type: {}",
                maskToken(token), file.getContentType());

        try {
            long startTime = System.currentTimeMillis();

            User user = userService.getUserFromToken(token);
            logger.debug("Uploading file for user: {}", user.getLogin());

            var savedFile = fileService.store(file, user);

            long duration = System.currentTimeMillis() - startTime;

            Map<String, String> response = new HashMap<>();
            response.put("filename", savedFile.getFilename());
            response.put("originalFilename", savedFile.getOriginalFilename());
            response.put("message", "File uploaded successfully");

            logger.info("File uploaded successfully - user: {}, original: {}, stored as: {}, size: {} bytes, duration: {}ms",
                    user.getLogin(),
                    savedFile.getOriginalFilename(),
                    savedFile.getFilename(),
                    file.getSize(),
                    duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("File upload failed - filename: {}, token: {}, error: {}",
                    file.getOriginalFilename(), maskToken(token), e.getMessage());
            logger.debug("Upload error details:", e);
            throw e;
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        logger.info("DELETE /file request received - filename: {}", filename);
        logger.debug("Delete request token: {}, filename: {}", maskToken(token), filename);

        try {
            long startTime = System.currentTimeMillis();

            User user = userService.getUserFromToken(token);
            logger.debug("Deleting file for user: {}, filename: {}", user.getLogin(), filename);

            fileService.deleteFile(filename, user);

            long duration = System.currentTimeMillis() - startTime;

            logger.info("File deleted successfully - user: {}, filename: {}, duration: {}ms",
                    user.getLogin(), filename, duration);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("File deletion failed - filename: {}, user token: {}, error: {}",
                    filename, maskToken(token), e.getMessage());
            logger.debug("Delete error details:", e);
            throw e;
        }
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        logger.info("GET /file download request received - filename: {}", filename);
        logger.debug("Download request token: {}, filename: {}", maskToken(token), filename);

        try {
            long startTime = System.currentTimeMillis();

            User user = userService.getUserFromToken(token);
            logger.debug("Downloading file for user: {}, filename: {}", user.getLogin(), filename);

            Resource resource = (Resource) fileService.loadAsResource(filename, user);

            String contentType = "application/octet-stream";
            String originalFilename = filename;


            try {
                var fileEntity = fileService.listFiles(user).stream()
                        .filter(f -> f.getFilename().equals(filename))
                        .findFirst();
                if (fileEntity.isPresent()) {
                    originalFilename = fileEntity.get().getOriginalFilename();
                    logger.debug("Resolved original filename: {} for stored filename: {}",
                            originalFilename, filename);
                }
            } catch (Exception e) {
                logger.warn("Could not resolve original filename for: {}, using default", filename);
                logger.debug("Filename resolution error:", e);
            }

            long duration = System.currentTimeMillis() - startTime;

            logger.info("File download prepared - user: {}, filename: {}, original: {}, duration: {}ms",
                    user.getLogin(), filename, originalFilename, duration);
            logger.debug("Download headers - Content-Disposition: attachment; filename=\"{}\", Content-Type: {}",
                    originalFilename, contentType);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + originalFilename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);

        } catch (Exception e) {
            logger.error("File download failed - filename: {}, user token: {}, error: {}",
                    filename, maskToken(token), e.getMessage());
            logger.debug("Download error details:", e);
            throw e;
        }
    }


    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
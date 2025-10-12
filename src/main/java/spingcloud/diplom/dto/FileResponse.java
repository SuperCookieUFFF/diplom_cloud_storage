package spingcloud.diplom.dto;

import java.time.LocalDateTime;

public class FileResponse {
    private String filename;
    private String originalFilename;
    private Long size;
    private LocalDateTime uploadedAt;

    public FileResponse() {}

    public FileResponse(String filename, String originalFilename, Long size, LocalDateTime uploadedAt) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }


    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}

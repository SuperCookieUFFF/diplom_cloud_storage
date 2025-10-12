package spingcloud.diplom.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import spingcloud.diplom.dto.FileResponse;
import spingcloud.diplom.entity.FileEntity;
import spingcloud.diplom.entity.User;
import spingcloud.diplom.repository.FileRepository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    @Value("${app.file-storage.location}")
    private String storageLocation;

    @Autowired
    private FileRepository fileRepository;

    private Path fileStoragePath;

    @PostConstruct
    public void init() {
        this.fileStoragePath = Paths.get(storageLocation).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize file storage", e);
        }
    }

    public FileEntity store(MultipartFile file, User user) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (originalFilename.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence: " + originalFilename);
        }

        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file: " + originalFilename);
        }

        String filename = generateFilename(originalFilename);
        Path targetLocation = this.fileStoragePath.resolve(filename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFilename(filename);
            fileEntity.setOriginalFilename(originalFilename);
            fileEntity.setSize(file.getSize());
            fileEntity.setContentType(file.getContentType());
            fileEntity.setUser(user);

            return fileRepository.save(fileEntity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    public Resource loadAsResource(String filename, User user) {
        FileEntity fileEntity = fileRepository.findByFilenameAndUser(filename, user)
                .orElseThrow(() -> new RuntimeException("File not found: " + filename));

        try {
            Path filePath = this.fileStoragePath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    public List<FileResponse> listFiles(User user) {
        return fileRepository.findByUserOrderByUploadedAtDesc(user).stream()
                .map(this::mapToFileResponse)
                .collect(Collectors.toList());
    }

    public void deleteFile(String filename, User user) {
        FileEntity fileEntity = fileRepository.findByFilenameAndUser(filename, user)
                .orElseThrow(() -> new RuntimeException("File not found: " + filename));

        try {
            Path filePath = this.fileStoragePath.resolve(filename);
            Files.deleteIfExists(filePath);
            fileRepository.delete(fileEntity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + filename, e);
        }
    }

    private String generateFilename(String originalFilename) {
        return UUID.randomUUID().toString() + "_" + originalFilename;
    }

    private FileResponse mapToFileResponse(FileEntity fileEntity) {
        return new FileResponse(
                fileEntity.getFilename(),
                fileEntity.getOriginalFilename(),
                fileEntity.getSize(),
                fileEntity.getUploadedAt()
        );
    }
}
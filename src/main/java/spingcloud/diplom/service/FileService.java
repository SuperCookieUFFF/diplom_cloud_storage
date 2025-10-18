package spingcloud.diplom.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import spingcloud.diplom.exception.FileNotFoundException;
import spingcloud.diplom.exception.FileStorageException;
import spingcloud.diplom.exception.InvalidFileException;
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

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Value("${app.file-storage.location}")
    private String storageLocation;

    @Autowired
    private FileRepository fileRepository;

    private Path fileStoragePath;

    @PostConstruct
    public void init() {
        this.fileStoragePath = Paths.get(storageLocation).toAbsolutePath().normalize();

        logger.info("Initializing file storage at location: {}", this.fileStoragePath);

        try {
            Files.createDirectories(this.fileStoragePath);
            logger.info("Successfully created file storage directory: {}", this.fileStoragePath);
        } catch (IOException e) {
            logger.error("Failed to initialize file storage directory: {}", this.fileStoragePath, e);
            throw new FileStorageException("Could not initialize file storage directory: " + this.fileStoragePath, e);
        }
    }

    public FileEntity store(MultipartFile file, User user) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        logger.info("Starting file upload - user: {}, filename: {}, size: {} bytes",
                user.getLogin(), originalFilename, file.getSize());

        if (originalFilename.contains("..")) {
            logger.warn("Security violation - filename contains invalid path sequence: {}, user: {}",
                    originalFilename, user.getLogin());
            throw new InvalidFileException("Filename contains invalid path sequence: " + originalFilename);
        }

        if (file.isEmpty()) {
            logger.warn("Attempt to upload empty file: {}, user: {}", originalFilename, user.getLogin());
            throw new InvalidFileException("Cannot store empty file: " + originalFilename);
        }

        String filename = generateFilename(originalFilename);
        Path targetLocation = this.fileStoragePath.resolve(filename);

        logger.debug("Generated unique filename: {} for original: {}", filename, originalFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("File successfully stored on disk: {}", targetLocation);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFilename(filename);
            fileEntity.setOriginalFilename(originalFilename);
            fileEntity.setSize(file.getSize());
            fileEntity.setContentType(file.getContentType());
            fileEntity.setUser(user);

            FileEntity savedEntity = fileRepository.save(fileEntity);
            logger.info("File successfully saved to database - id: {}, filename: {}, user: {}",
                    savedEntity.getId(), filename, user.getLogin());

            return savedEntity;
        } catch (IOException e) {
            logger.error("Failed to store file: {}, user: {}", originalFilename, user.getLogin(), e);
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }
    }

    public Resource loadAsResource(String filename, User user) {
        logger.debug("Attempting to load file: {} for user: {}", filename, user.getLogin());

        FileEntity fileEntity = fileRepository.findByFilenameAndUser(filename, user)
                .orElseThrow(() -> {
                    logger.warn("File not found - filename: {}, user: {}", filename, user.getLogin());
                    return new FileNotFoundException("File not found with filename: " + filename + " for current user");
                });

        try {
            Path filePath = this.fileStoragePath.resolve(filename).normalize();
            logger.debug("Resolved file path: {}", filePath);

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                logger.info("File successfully loaded: {} for user: {}", filename, user.getLogin());
                return resource;
            } else {
                logger.error("File exists but is not readable: {}, user: {}", filename, user.getLogin());
                throw new FileStorageException("File exists but is not readable: " + filename);
            }
        } catch (MalformedURLException e) {
            logger.error("Invalid file path: {}, user: {}", filename, user.getLogin(), e);
            throw new FileStorageException("Invalid file path: " + filename, e);
        }
    }

    public List<FileResponse> listFiles(User user) {
        logger.debug("Listing files for user: {}", user.getLogin());

        List<FileResponse> files = fileRepository.findByUserOrderByUploadedAtDesc(user).stream()
                .map(this::mapToFileResponse)
                .collect(Collectors.toList());

        logger.info("Retrieved {} files for user: {}", files.size(), user.getLogin());
        return files;
    }

    public void deleteFile(String filename, User user) {
        logger.info("Attempting to delete file: {} for user: {}", filename, user.getLogin());

        FileEntity fileEntity = fileRepository.findByFilenameAndUser(filename, user)
                .orElseThrow(() -> {
                    logger.warn("File not found for deletion - filename: {}, user: {}", filename, user.getLogin());
                    return new FileNotFoundException("File not found with filename: " + filename + " for current user");
                });

        try {
            Path filePath = this.fileStoragePath.resolve(filename);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                fileRepository.delete(fileEntity);
                logger.info("File successfully deleted - filename: {}, user: {}", filename, user.getLogin());
            } else {
                logger.warn("File not found on disk during deletion - filename: {}, user: {}", filename, user.getLogin());
            }
        } catch (IOException e) {
            logger.error("Failed to delete file from storage: {}, user: {}", filename, user.getLogin(), e);
            throw new FileStorageException("Failed to delete file from storage: " + filename, e);
        }
    }

    private String generateFilename(String originalFilename) {
        String generatedName = UUID.randomUUID().toString() + "_" + originalFilename;
        logger.trace("Generated filename: {} for original: {}", generatedName, originalFilename);
        return generatedName;
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
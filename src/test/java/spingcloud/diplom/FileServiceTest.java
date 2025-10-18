package spingcloud.diplom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import spingcloud.diplom.entity.FileEntity;
import spingcloud.diplom.entity.User;
import spingcloud.diplom.repository.FileRepository;
import spingcloud.diplom.service.FileService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    private User testUser;

    @BeforeEach
    void setUp() throws IOException {
        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");

        // Очищаем тестовую директорию перед каждым тестом
        Path testDir = Path.of("./test-uploads");
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .filter(path -> !path.equals(testDir))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }


        ReflectionTestUtils.setField(fileService, "storageLocation", "./test-uploads");
        fileService.init();
    }

    @Test
    void testStoreFile() {
        // given
        MultipartFile file = new MockMultipartFile(
                "test.txt",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );


        FileEntity savedFileEntity = new FileEntity();
        savedFileEntity.setId(1L);
        savedFileEntity.setFilename("uuid_test.txt");
        savedFileEntity.setOriginalFilename("test.txt");
        savedFileEntity.setSize(11L);
        savedFileEntity.setContentType("text/plain");
        savedFileEntity.setUser(testUser);

        when(fileRepository.save(any(FileEntity.class))).thenReturn(savedFileEntity);


        FileEntity result = fileService.store(file, testUser);


        assertNotNull(result, "Saved file entity should not be null");
        assertEquals(1L, result.getId());
        assertEquals("test.txt", result.getOriginalFilename());
        verify(fileRepository, times(1)).save(any(FileEntity.class));
    }
}
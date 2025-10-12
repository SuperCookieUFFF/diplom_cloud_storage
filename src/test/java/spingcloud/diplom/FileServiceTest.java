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
import spingcloud.diplom.entity.User;
import spingcloud.diplom.repository.FileRepository;
import springcloud.diplom.service.FileService;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");

        // Инициализируем fileStoragePath для тестов
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

        // when
        fileService.store(file, testUser);

        // then
        verify(fileRepository, times(1)).save(any());
    }
}

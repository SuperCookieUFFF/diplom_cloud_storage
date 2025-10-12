package spingcloud.diplom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spingcloud.diplom.entity.FileEntity;
import spingcloud.diplom.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUserOrderByUploadedAtDesc(User user);
    Optional<FileEntity> findByFilenameAndUser(String filename, User user);
    boolean existsByFilenameAndUser(String filename, User user);
    void deleteByFilenameAndUser(String filename, User user);
}

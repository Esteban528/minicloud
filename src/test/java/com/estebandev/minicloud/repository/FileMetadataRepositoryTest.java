package com.estebandev.minicloud.repository;

import com.estebandev.minicloud.entity.FileMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FileMetadataRepositoryTest {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Test
    void testSaveFileMetadata() {
        // Arrange
        FileMetadata metadata = FileMetadata.builder()
                .uuid("123e4567-e89b-12d3-a456-426614174000")
                .key("author")
                .value("John Doe")
                .build();

        // Act
        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);

        // Assert
        assertThat(savedMetadata).isNotNull();
        assertThat(savedMetadata.getId()).isPositive();
        assertThat(savedMetadata.getUuid()).isEqualTo(metadata.getUuid());
        assertThat(savedMetadata.getKey()).isEqualTo(metadata.getKey());
        assertThat(savedMetadata.getValue()).isEqualTo(metadata.getValue());
    }

    @Test
    void testFindByUuid() {
        // Arrange
        String uuid = "123e4567-e89b-12d3-a456-426614174001";
        fileMetadataRepository.saveAll(List.of(
                FileMetadata.builder().uuid(uuid).key("type").value("PDF").build(),
                FileMetadata.builder().uuid(uuid).key("size").value("15MB").build(),
                FileMetadata.builder().uuid("other-uuid").key("security").value("encrypted").build()));

        // Act
        List<FileMetadata> results = fileMetadataRepository.findByUuid(uuid);

        // Assert
        assertThat(results)
                .hasSize(2)
                .allMatch(m -> m.getUuid().equals(uuid));
    }

    @Test
    void testUpdateUuid() {
        // Arrange
        String oldUuid = "old-uuid";
        String newUuid = "new-uuid";

        fileMetadataRepository.saveAllAndFlush(List.of(
                FileMetadata.builder().uuid(oldUuid).key("status").value("migrating").build(),
                FileMetadata.builder().uuid(oldUuid).key("version").value("2.0").build()));

        // Act
        int updatedCount = fileMetadataRepository.updateUuid(oldUuid, newUuid);

        // Assert
        assertThat(updatedCount).isEqualTo(2);
        assertThat(fileMetadataRepository.findByUuid(oldUuid)).isEmpty();
        assertThat(fileMetadataRepository.findByUuid(newUuid))
                .hasSize(2)
                .allSatisfy(m -> {
                    assertThat(m.getUuid()).isEqualTo(newUuid);
                    assertThat(m.getKey()).isIn("status", "version");
                });
    }

    @Test
    void testDeleteByUuid() {
        // Arrange
        String uuidToDelete = "delete-uuid";
        fileMetadataRepository.saveAll(List.of(
                FileMetadata.builder().uuid(uuidToDelete).key("temp").value("true").build(),
                FileMetadata.builder().uuid(uuidToDelete).key("expires").value("24h").build()));

        // Act
        int deletedCount = fileMetadataRepository.deleteByUuid(uuidToDelete);

        // Assert
        assertThat(deletedCount).isEqualTo(2);
        assertThat(fileMetadataRepository.findByUuid(uuidToDelete)).isEmpty();
    }

    @Test
    void testDeleteById() {
        // Arrange
        FileMetadata metadata = fileMetadataRepository.save(
                FileMetadata.builder()
                        .uuid("delete-by-id-uuid")
                        .key("priority")
                        .value("high")
                        .build());

        // Act
        fileMetadataRepository.deleteById(metadata.getId());

        // Assert
        assertThat(fileMetadataRepository.findById(metadata.getId())).isEmpty();
    }

    @Test
    void testComplexOperationsFlow() {
        // Initial state
        assertThat(fileMetadataRepository.count()).isZero();

        // Create multiple entries
        List<FileMetadata> initialData = List.of(
                FileMetadata.builder().uuid("uuid-a").key("type").value("text").build(),
                FileMetadata.builder().uuid("uuid-a").key("size").value("2KB").build(),
                FileMetadata.builder().uuid("uuid-b").key("security").value("public").build());
        fileMetadataRepository.saveAll(initialData);
        assertThat(fileMetadataRepository.count()).isEqualTo(3);

        // Update UUIDs
        int updated = fileMetadataRepository.updateUuid("uuid-a", "uuid-archived-a");
        assertThat(updated).isEqualTo(2);

        // Verify update
        assertThat(fileMetadataRepository.findByUuid("uuid-archived-a")).hasSize(2);
        assertThat(fileMetadataRepository.findByUuid("uuid-a")).isEmpty();

        // Delete remaining
        int deleted = fileMetadataRepository.deleteByUuid("uuid-b");
        assertThat(deleted).isEqualTo(1);
        assertThat(fileMetadataRepository.count()).isEqualTo(2);
    }
}

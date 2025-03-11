package com.estebandev.minicloud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.estebandev.minicloud.entity.FileMetadata;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByUuid(String uuid);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE FileMetadata fm SET fm.uuid = :new_uuid WHERE fm.uuid = :old_uuid")
    int updateUuid(@Param("old_uuid") String oldUUID, @Param("new_uuid") String newUUID);

    @Modifying
    @Transactional
    @Query("DELETE FROM FileMetadata fm WHERE fm.uuid = :uuid")
    int deleteByUuid(@Param("uuid") String uuid);

    Optional<FileMetadata> findByUuidAndKey(String uuid, String key);
}

package com.estebandev.minicloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.entity.UserMetadata;
import java.util.List;

public interface UserMetadataRepository extends JpaRepository<UserMetadata, Long>{
    List<UserMetadata> findByKey(String key);
    List<UserMetadata> findByKeyContaining(String contain);
    List<UserMetadata> findByUserAndKeyContaining(User user, String contain);
}

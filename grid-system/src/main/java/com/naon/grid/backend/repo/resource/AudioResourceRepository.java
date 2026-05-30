package com.naon.grid.backend.repo.resource;

import com.naon.grid.backend.domain.resource.AudioResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioResourceRepository extends JpaRepository<AudioResource, Long>, JpaSpecificationExecutor<AudioResource> {

    List<AudioResource> findByIdInAndStatus(List<Long> ids, Integer status);
}

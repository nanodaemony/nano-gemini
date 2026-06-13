package com.naon.grid.backend.repo.charradical;

import com.naon.grid.backend.domain.charradical.CharRadical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CharRadicalRepository extends JpaRepository<CharRadical, Long>,
        JpaSpecificationExecutor<CharRadical> {
}

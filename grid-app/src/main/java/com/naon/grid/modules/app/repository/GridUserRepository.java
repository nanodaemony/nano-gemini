package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * APP用户数据访问层
 */
@Repository
public interface GridUserRepository extends JpaRepository<GridUser, Long>, JpaSpecificationExecutor<GridUser> {

    /**
     * 根据用户名查询用户
     */
    Optional<GridUser> findByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    Optional<GridUser> findByPhone(String phone);

    /**
     * 根据用户名判断是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据手机号判断是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据邮箱查询用户
     */
    Optional<GridUser> findByEmail(String email);

    /**
     * 根据邮箱判断是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据推荐码查询用户
     */
    Optional<GridUser> findByReferralCode(String referralCode);

    /**
     * 根据推荐码判断是否存在
     */
    boolean existsByReferralCode(String referralCode);

    /**
     * 根据机构ID和角色查询用户
     */
    List<GridUser> findByOrgIdAndOrgRole(Integer orgId, String orgRole);
}

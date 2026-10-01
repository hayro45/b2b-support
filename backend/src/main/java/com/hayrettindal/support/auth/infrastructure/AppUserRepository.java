package com.hayrettindal.support.auth.infrastructure;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import com.hayrettindal.support.auth.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByEmailIgnoreCase(String email);

    List<AppUserEntity> findByOrganizationIdAndActiveTrueAndRoleInOrderByFullNameAsc(UUID organizationId, List<UserRole> roles);
}

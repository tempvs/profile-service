package club.tempvs.profile.dao;

import club.tempvs.profile.domain.Profile;
import club.tempvs.profile.domain.Profile.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    List<Profile> findAllByTypeAndUserIdAndIsActiveTrue(Type type, Long userId);

    int countByTypeAndUserIdAndIsActiveTrue(Type type, Long userId);

    Optional<Profile> findByAliasAndIsActiveTrue(String alias);

    boolean existsByAliasAndIsActiveTrue(String alias);

    boolean existsByAliasAndIdNotAndIsActiveTrue(String alias, Long id);
}

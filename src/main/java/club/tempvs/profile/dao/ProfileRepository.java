package club.tempvs.profile.dao;

import club.tempvs.profile.domain.Profile;
import club.tempvs.profile.domain.Profile.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    List<Profile> findAllByTypeAndUserId(Type type, Long userId);

    int countByTypeAndUserId(Type type, Long userId);

    Optional<Profile> findByAlias(String alias);

    boolean existsByAlias(String alias);

    boolean existsByAliasAndIdNot(String alias, Long id);
}

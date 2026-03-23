package club.tempvs.profile.service.impl;

import club.tempvs.profile.component.ProfileValidator;
import club.tempvs.profile.component.UserHolder;
import club.tempvs.profile.dao.ProfileRepository;
import club.tempvs.profile.domain.Profile;
import club.tempvs.profile.domain.Profile.Type;
import club.tempvs.profile.dto.ImageDto;
import club.tempvs.profile.service.ImageService;
import club.tempvs.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final int MAX_CLUB_PROFILE_COUNT = 10;
    private static final String PROFILE_ENTITY_IDENTIFIER = "profile";
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{2,39}$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    );

    private final UserHolder userHolder;
    private final ProfileValidator profileValidator;
    private final ProfileRepository profileRepository;
    private final ImageService imageService;

    @Override
    public Profile createUserProfile(Profile profile) {
        profileValidator.validateUserProfile(profile);
        Long userId = userHolder.getUserId();

        if(!findUserProfileByUserId(userId).isEmpty()) {
            throw new IllegalStateException(String.format("User with id %d already has user profile", userId));
        }

        profile.setType(Type.USER);
        profile.setUserId(userId);
        profile.setIsActive(Boolean.TRUE);
        profile.setAlias(normalizeAndValidateAlias(profile.getAlias(), null));

        return save(profile);
    }

    @Override
    public Profile createClubProfile(Profile profile) {
        Long userId = userHolder.getUserId();

        if(countClubProfilesByUserId(userId) >= MAX_CLUB_PROFILE_COUNT) {
            throw new IllegalStateException(String.format("User with id %d has too many club profiles", userId));
        }

        profile.setType(Type.CLUB);
        profile.setUserId(userId);
        profile.setIsActive(Boolean.TRUE);
        profile.setAlias(normalizeAndValidateAlias(profile.getAlias(), null));

        return save(profile);
    }

    @Override
    public Profile get(String aliasOrId) {
        return resolveProfile(aliasOrId);
    }

    @Override
    public Profile getUserProfile() {
        Long userId = userHolder.getUserId();
        return findUserProfileByUserId(userId)
                .stream()
                .findAny()
                .get();
    }

    @Override
    public Profile getUserProfile(Long userId) {
        return findUserProfileByUserId(userId)
                .stream()
                .findAny()
                .get();
    }

    @Override
    public Profile update(Long id, Profile profile) {
        Profile persistentProfile = fetchProfile(id);
        Long currentUserId = userHolder.getUserId();

        if (!persistentProfile.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        validateUpdate(profile, persistentProfile.getType());
        String normalizedAlias = normalizeAndValidateAlias(profile.getAlias(), persistentProfile.getId());
        applyUpdates(persistentProfile, profile);
        persistentProfile.setAlias(normalizedAlias);
        return save(persistentProfile);
    }

    @Override
    public void deleteClubProfile(Long id) {
        Profile persistentProfile = fetchProfile(id);
        Long currentUserId = userHolder.getUserId();

        if (persistentProfile.getType() != Type.CLUB) {
            throw new IllegalArgumentException("Only club profiles can be deleted");
        }

        if (!persistentProfile.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        persistentProfile.setIsActive(Boolean.FALSE);
        save(persistentProfile);
    }

    @Override
    public void uploadAvatar(Long profileId, ImageDto imageDto) {
        Long currentUserId = userHolder.getUserId();
        Long userId = profileRepository.findById(profileId)
                .map(Profile::getUserId)
                .get();

        if (!userId.equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        imageDto.setBelongsTo(PROFILE_ENTITY_IDENTIFIER);
        imageDto.setEntityId(profileId);
        imageService.store(imageDto);
    }

    @Override
    public List<Profile> getClubProfiles(Long userId) {
        return profileRepository.findAllByTypeAndUserIdAndIsActiveTrue(Type.CLUB, userId);
    }

    private Profile fetchProfile(Long id) {
        return profileRepository.findById(id)
                .filter(profile -> Boolean.TRUE.equals(profile.getIsActive()))
                .get();
    }

    private Profile resolveProfile(String aliasOrId) {
        String candidate = aliasOrId == null ? "" : aliasOrId.trim();

        if (!candidate.isEmpty()) {
            Optional<Profile> aliasProfile = profileRepository.findByAliasAndIsActiveTrue(candidate.toLowerCase(Locale.ROOT));

            if (aliasProfile.isPresent()) {
                return aliasProfile.get();
            }
        }

        try {
            return fetchProfile(Long.valueOf(candidate));
        } catch (NumberFormatException e) {
            throw new NoSuchElementException("Profile not found");
        }
    }

    private Profile save(Profile profile) {
        return profileRepository.save(profile);
    }

    private List<Profile> findUserProfileByUserId(Long userId) {
        return profileRepository.findAllByTypeAndUserIdAndIsActiveTrue(Type.USER, userId);
    }

    private int countClubProfilesByUserId(Long userId) {
        return profileRepository.countByTypeAndUserIdAndIsActiveTrue(Type.CLUB, userId);
    }

    private void validateUpdate(Profile profile, Type type) {
        if (!StringUtils.hasText(profile.getFirstName())) {
            throw new IllegalArgumentException("First name can not be blank");
        }

        if (!StringUtils.hasText(profile.getLastName())) {
            throw new IllegalArgumentException("Last name can not be blank");
        }

        if (type == Type.CLUB && profile.getPeriod() == null) {
            throw new IllegalArgumentException("Period can not be blank");
        }
    }

    private String normalizeAndValidateAlias(String alias, Long profileId) {
        String normalizedAlias = normalizeAlias(alias);
        if (normalizedAlias == null) {
            return null;
        }

        validateAlias(normalizedAlias);

        boolean aliasExists = profileId == null
                ? profileRepository.existsByAliasAndIsActiveTrue(normalizedAlias)
                : profileRepository.existsByAliasAndIdNotAndIsActiveTrue(normalizedAlias, profileId);

        if (aliasExists) {
            throw new IllegalArgumentException("Alias is already in use");
        }

        return normalizedAlias;
    }

    private String normalizeAlias(String alias) {
        if (!StringUtils.hasText(alias)) {
            return null;
        }

        return alias.trim().toLowerCase(Locale.ROOT);
    }

    private void validateAlias(String alias) {
        if (UUID_PATTERN.matcher(alias).matches()) {
            throw new IllegalArgumentException("Alias can not be a UUID");
        }

        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            throw new IllegalArgumentException(
                    "Alias must start with a letter and contain only lowercase letters, digits, or hyphens"
            );
        }
    }

    private void applyUpdates(Profile target, Profile source) {
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setNickName(source.getNickName());
        target.setProfileEmail(source.getProfileEmail());
        target.setLocation(source.getLocation());

        if (target.getType() == Type.CLUB) {
            target.setPeriod(source.getPeriod());
        }
    }
}

package club.tempvs.profile.service;

import club.tempvs.profile.domain.Profile;
import club.tempvs.profile.dto.ImageDto;

import java.util.List;

public interface ProfileService {

    Profile createUserProfile(Profile profile);

    Profile createClubProfile(Profile profile);

    Profile get(String aliasOrId);

    Profile getUserProfile();

    Profile getUserProfile(Long userId);

    Profile update(Long id, Profile profile);

    void deleteClubProfile(Long id);

    List<Profile> getClubProfiles(Long userId);

    void uploadAvatar(Long profileId, ImageDto imageDto);
}

package club.tempvs.profile.service

import club.tempvs.profile.component.ProfileValidator
import club.tempvs.profile.component.UserHolder
import club.tempvs.profile.dao.ProfileRepository
import club.tempvs.profile.domain.Profile
import club.tempvs.profile.domain.Profile.Type
import club.tempvs.profile.dto.ImageDto
import club.tempvs.profile.service.impl.ProfileServiceImpl
import org.springframework.security.access.AccessDeniedException
import spock.lang.Specification
import spock.lang.Subject

class ProfileServiceSpec extends Specification {

    def userHolder = Mock(UserHolder)
    def profileValidator = Mock(ProfileValidator)
    def profileRepository = Mock(ProfileRepository)
    def imageService = Mock(ImageService)

    @Subject
    ProfileService profileService = new ProfileServiceImpl(userHolder, profileValidator, profileRepository, imageService)

    def profile = Mock(Profile)
    def imageDto = Mock(ImageDto)

    def "create club profile"() {
        given:
        Long userId = 1L

        when:
        Profile result = profileService.createClubProfile(profile)

        then:
        1 * userHolder.userId >> userId
        1 * profileRepository.countByTypeAndUserIdAndIsActiveTrue(Type.CLUB, userId) >> 9
        1 * profile.setType(Type.CLUB)
        1 * profile.setUserId(userId)
        1 * profile.setIsActive(Boolean.TRUE)
        1 * profile.getAlias() >> null
        1 * profile.setAlias(null)
        1 * profileRepository.save(profile) >> profile
        0 * _

        and:
        result == profile
    }

    def "create user profile"() {
        given:
        Long userId = 1L

        when:
        Profile result = profileService.createUserProfile(profile)

        then:
        1 * profileValidator.validateUserProfile(profile)
        1 * userHolder.userId >> userId
        1 * profileRepository.findAllByTypeAndUserIdAndIsActiveTrue(Type.USER, userId) >> []
        1 * profile.setType(Type.USER)
        1 * profile.setUserId(userId)
        1 * profile.setIsActive(Boolean.TRUE)
        1 * profile.getAlias() >> null
        1 * profile.setAlias(null)
        1 * profileRepository.save(profile) >> profile
        0 * _

        and:
        result == profile
    }

    def "create a user profile duplicate"() {
        given:
        Long userId = 1L

        when:
        profileService.createUserProfile(profile)

        then:
        1 * profileValidator.validateUserProfile(profile)
        1 * userHolder.userId >> userId
        1 * profileRepository.findAllByTypeAndUserIdAndIsActiveTrue(Type.USER, userId) >> [profile]
        0 * _

        and:
        Exception exception = thrown IllegalStateException
        exception.message == 'User with id 1 already has user profile'
    }

    def "create 11th club profile"() {
        given:
        Long userId = 1L

        when:
        profileService.createClubProfile(profile)

        then:
        1 * userHolder.userId >> userId
        1 * profileRepository.countByTypeAndUserIdAndIsActiveTrue(Type.CLUB, userId) >> 10
        0 * _

        and:
        Exception exception = thrown IllegalStateException
        exception.message == 'User with id 1 has too many club profiles'
    }

    def "get profile"() {
        given:
        String id = '1'

        when:
        Profile result = profileService.get(id)

        then:
        1 * profileRepository.findByAliasAndIsActiveTrue('1') >> Optional.empty()
        1 * profileRepository.findById(1L) >> Optional.of(profile)
        1 * profile.isActive >> true
        0 * _

        and:
        result == profile
    }

    def "get profile by alias first"() {
        when:
        Profile result = profileService.get('albvs')

        then:
        1 * profileRepository.findByAliasAndIsActiveTrue('albvs') >> Optional.of(profile)
        0 * _

        and:
        result == profile
    }

    def "get missing profile"() {
        given:
        String id = '1'

        when:
        profileService.get(id)

        then:
        1 * profileRepository.findByAliasAndIsActiveTrue('1') >> Optional.empty()
        1 * profileRepository.findById(1L) >> Optional.empty()
        0 * _

        and:
        Exception exception = thrown NoSuchElementException
        exception.message == 'No value present'
    }

    def "get missing alias profile"() {
        when:
        profileService.get('missing-alias')

        then:
        1 * profileRepository.findByAliasAndIsActiveTrue('missing-alias') >> Optional.empty()
        0 * _

        and:
        Exception exception = thrown NoSuchElementException
        exception.message == 'Profile not found'
    }

    def "get user profile"() {
        given:
        Long userId = 1L

        when:
        Profile result = profileService.userProfile

        then:
        1 * userHolder.userId >> userId
        1 * profileRepository.findAllByTypeAndUserIdAndIsActiveTrue(Type.USER, userId) >> [profile]
        0 * _

        and:
        result == profile
    }

    def "get missing user profile"() {
        given:
        Long userId = 1L

        when:
        profileService.userProfile

        then:
        1 * userHolder.userId >> userId
        1 * profileRepository.findAllByTypeAndUserIdAndIsActiveTrue(Type.USER, userId) >> []
        0 * _

        and:
        Exception exception = thrown NoSuchElementException
        exception.message == 'No value present'
    }

    def "get user profile by user id"() {
        given:
        Long userId = 1L

        when:
        Profile result = profileService.getUserProfile(userId)

        then:
        1 * profileRepository.findAllByTypeAndUserIdAndIsActiveTrue(Type.USER, userId) >> [profile]
        0 * _

        and:
        result == profile
    }

    def "update profile"() {
        given:
        Long profileId = 1L
        Long userId = 2L
        def persistentProfile = new Profile(id: profileId, userId: userId, type: Type.USER, isActive: true)
        def update = new Profile(
                firstName: 'Updated',
                lastName: 'User',
                nickName: 'Nick',
                profileEmail: 'user@email.com',
                location: 'Somewhere',
                alias: 'Alias'
        )

        when:
        Profile result = profileService.update(profileId, update)

        then:
        1 * profileRepository.findById(profileId) >> Optional.of(persistentProfile)
        1 * userHolder.userId >> userId
        1 * profileRepository.existsByAliasAndIdNotAndIsActiveTrue('alias', profileId) >> false
        1 * profileRepository.save(persistentProfile) >> persistentProfile
        0 * _

        and:
        persistentProfile.firstName == 'Updated'
        persistentProfile.lastName == 'User'
        persistentProfile.nickName == 'Nick'
        persistentProfile.profileEmail == 'user@email.com'
        persistentProfile.location == 'Somewhere'
        persistentProfile.alias == 'alias'
        result == persistentProfile
    }

    def "update profile for wrong user"() {
        given:
        Long profileId = 1L
        Long currentUserId = 2L
        def persistentProfile = new Profile(userId: 3L, type: Type.USER, isActive: true)
        def update = new Profile(firstName: 'Updated', lastName: 'User')

        when:
        profileService.update(profileId, update)

        then:
        1 * profileRepository.findById(profileId) >> Optional.of(persistentProfile)
        1 * userHolder.userId >> currentUserId
        0 * _

        and:
        Exception exception = thrown AccessDeniedException
        exception.message == 'Access denied'
    }

    def "update profile rejects numeric alias"() {
        given:
        Long profileId = 1L
        Long userId = 2L
        def persistentProfile = new Profile(userId: userId, type: Type.USER, isActive: true)
        def update = new Profile(firstName: 'Updated', lastName: 'User', alias: '12345')

        when:
        profileService.update(profileId, update)

        then:
        1 * profileRepository.findById(profileId) >> Optional.of(persistentProfile)
        1 * userHolder.userId >> userId
        0 * _

        and:
        Exception exception = thrown IllegalArgumentException
        exception.message == 'Alias must start with a letter and contain only lowercase letters, digits, or hyphens'
    }

    def "update profile rejects duplicate alias"() {
        given:
        Long profileId = 1L
        Long userId = 2L
        def persistentProfile = new Profile(id: profileId, userId: userId, type: Type.USER, isActive: true)
        def update = new Profile(firstName: 'Updated', lastName: 'User', alias: 'alias')

        when:
        profileService.update(profileId, update)

        then:
        1 * profileRepository.findById(profileId) >> Optional.of(persistentProfile)
        1 * userHolder.userId >> userId
        1 * profileRepository.existsByAliasAndIdNotAndIsActiveTrue('alias', profileId) >> true
        0 * _

        and:
        Exception exception = thrown IllegalArgumentException
        exception.message == 'Alias is already in use'
    }

    def "get club profiles for user"() {
        given:
        Long userId = 1L

        when:
        List<Profile> result = profileService.getClubProfiles(userId)

        then:
        1 * profileRepository.findAllByTypeAndUserIdAndIsActiveTrue(Type.CLUB, userId) >> [profile]
        0 * _

        and:
        result == [profile]
    }

    def "delete club profile"() {
        given:
        Long profileId = 1L
        Long userId = 2L
        def persistentProfile = new Profile(id: profileId, userId: userId, type: Type.CLUB, isActive: true)

        when:
        profileService.deleteClubProfile(profileId)

        then:
        1 * profileRepository.findById(profileId) >> Optional.of(persistentProfile)
        1 * userHolder.userId >> userId
        1 * profileRepository.save(persistentProfile) >> persistentProfile
        0 * _

        and:
        persistentProfile.isActive == false
    }

    def "delete user profile is forbidden"() {
        given:
        Long profileId = 1L
        Long userId = 2L
        def persistentProfile = new Profile(id: profileId, userId: userId, type: Type.USER, isActive: true)

        when:
        profileService.deleteClubProfile(profileId)

        then:
        1 * profileRepository.findById(profileId) >> Optional.of(persistentProfile)
        1 * userHolder.userId >> userId
        0 * _

        and:
        Exception exception = thrown IllegalArgumentException
        exception.message == 'Only club profiles can be deleted'
    }

    def "add avatar"() {
        given:
        Long profileId = 1L
        Long userId = 2L

        when:
        profileService.uploadAvatar(profileId, imageDto)

        then:
        1 * userHolder.userId >> userId
        1 * profileRepository.findById(profileId) >> Optional.of(profile)
        1 * profile.userId >> userId
        1 * imageDto.setBelongsTo('profile')
        1 * imageDto.setEntityId(profileId)
        1 * imageService.store(imageDto)
        0 * _
    }

    def "add avatar for wrong user"() {
        given:
        Long profileId = 1L
        Long userId = 2L
        Long profileUserId = 3L

        when:
        profileService.uploadAvatar(profileId, imageDto)

        then:
        1 * userHolder.userId >> userId
        1 * profileRepository.findById(profileId) >> Optional.of(profile)
        1 * profile.userId >> profileUserId
        0 * _

        and:
        Exception exception = thrown AccessDeniedException
        exception.message == 'Access denied'
    }
}

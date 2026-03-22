package club.tempvs.profile.controller

import club.tempvs.profile.dao.ProfileRepository
import club.tempvs.profile.domain.Profile
import club.tempvs.profile.domain.Profile.Type
import club.tempvs.profile.dto.UserInfoDto
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.ResourceUtils
import spock.lang.Specification
import tools.jackson.databind.json.JsonMapper

import java.nio.file.Files

import static org.hamcrest.Matchers.is
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        replace = AutoConfigureEmbeddedDatabase.Replace.ANY
)
@ActiveProfiles("test")
class ProfileControllerIntegrationSpec extends Specification {

    private static final String USER_INFO_HEADER = "User-Info"
    private static final String AUTHORIZATION_HEADER = "Authorization"
    private static final String TOKEN = "df41895b9f26094d0b1d39b7bdd9849e" //security_token as MD5

    @Autowired
    private MockMvc mvc
    @Autowired
    private JsonMapper jsonMapper
    @Autowired
    private ProfileRepository profileRepository

    def "create user profile"() {
        given:
        File createProfileFile = ResourceUtils.getFile("classpath:profile/create-user-profile.json")
        String createProfileJson = new String(Files.readAllBytes(createProfileFile.toPath()))
        String userInfoValue = buildUserInfoValue(1L)

        expect:
        mvc.perform(post("/user-profile")
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(createProfileJson)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("firstName", is("first name")))
                    .andExpect(jsonPath("lastName", is("last name")))
                    .andExpect(jsonPath("nickName", is("nickname")))
                    .andExpect(jsonPath("profileEmail", is("user@email.com")))
                    .andExpect(jsonPath("location", is("Earth")))
                    .andExpect(jsonPath("alias", is("user-alias")))
                    .andExpect(jsonPath("type", is("USER")))

    }

    def "create club profile"() {
        given:
        File createProfileFile = ResourceUtils.getFile("classpath:profile/create-club-profile.json")
        String createProfileJson = new String(Files.readAllBytes(createProfileFile.toPath()))
        String userInfoValue = buildUserInfoValue(1L)

        expect:
        mvc.perform(post("/club-profile")
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(createProfileJson)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("firstName", is("first name")))
                .andExpect(jsonPath("lastName", is("last name")))
                .andExpect(jsonPath("type", is("CLUB")))
                .andExpect(jsonPath("period", is("ANTIQUITY")))

    }

    def "create user profile being unauthenticated"() {
        given:
        File createProfileFile = ResourceUtils.getFile("classpath:profile/create-user-profile.json")
        String createProfileJson = new String(Files.readAllBytes(createProfileFile.toPath()))

        expect:
        mvc.perform(post("/user-profile")
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(createProfileJson)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isUnauthorized())
    }

    def "create second user profile"() {
        given:
        File createProfileFile = ResourceUtils.getFile("classpath:profile/create-user-profile.json")
        String createProfileJson = new String(Files.readAllBytes(createProfileFile.toPath()))
        String userInfoValue = buildUserInfoValue(1L)

        and:
        Profile profile = new Profile(firstName: 'firstName', lastName: 'lastName', userId: 1L, type: Type.USER)
        profileRepository.save(profile)

        expect:
        mvc.perform(post("/user-profile")
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(createProfileJson)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isConflict())
    }

    def "get profile for user type"() {
        given:
        Long userId = 1L
        String firstName = "firstName"
        String lastName = "lastName"

        and:
        Profile profile = new Profile(firstName: firstName, lastName: lastName, userId: userId, type: Type.USER)
        profileRepository.save(profile)

        expect:
        mvc.perform(get("/profile/" + profile.id)
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("firstName", is(firstName)))
                    .andExpect(jsonPath("lastName", is(lastName)))
                    .andExpect(jsonPath("userId", is(userId.toInteger())))
                    .andExpect(jsonPath("type", is("USER")))
    }

    def "get profile by alias"() {
        given:
        Long userId = 1L
        Profile profile = new Profile(
                firstName: 'firstName',
                lastName: 'lastName',
                userId: userId,
                alias: 'albvs',
                type: Type.USER
        )
        profileRepository.save(profile)

        expect:
        mvc.perform(get('/profile/albvs')
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath('id', is(profile.id.intValue())))
                .andExpect(jsonPath('alias', is('albvs')))
    }

    def "get user profile"() {
        given:
        Long userId = 1L
        String firstName = "firstName"
        String lastName = "lastName"
        String userInfoValue = buildUserInfoValue(userId)

        and:
        Profile profile = new Profile(firstName: firstName, lastName: lastName, userId: userId, type: Type.USER)
        profileRepository.save(profile)

        expect:
        mvc.perform(get("/profile")
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("firstName", is(firstName)))
                    .andExpect(jsonPath("lastName", is(lastName)))
                    .andExpect(jsonPath("userId", is(userId.toInteger())))
                    .andExpect(jsonPath("type", is("USER")))
    }

    def "get user profile being unauthenticated"() {
        expect:
        mvc.perform(get("/profile")
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isUnauthorized())
    }

    def "get user profile for missing one"() {
        given:
        String userInfoValue = buildUserInfoValue(1L)

        expect:
        mvc.perform(get("/profile")
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isNotFound())
    }

    def "update user profile"() {
        given:
        Long userId = 1L
        String userInfoValue = buildUserInfoValue(userId)
        Profile profile = new Profile(firstName: 'firstName', lastName: 'lastName', userId: userId, type: Type.USER)
        profileRepository.save(profile)
        String payload = """
        {
          "firstName": "updated",
          "lastName": "profile",
          "nickName": "nick",
          "profileEmail": "updated@email.com",
          "location": "Earth",
          "alias": "alias"
        }
        """

        expect:
        mvc.perform(put("/profile/" + profile.id)
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(payload)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("firstName", is("updated")))
                .andExpect(jsonPath("lastName", is("profile")))
                .andExpect(jsonPath("nickName", is("nick")))
                .andExpect(jsonPath("profileEmail", is("updated@email.com")))
                .andExpect(jsonPath("location", is("Earth")))
                .andExpect(jsonPath("alias", is("alias")))
    }

    def "update user profile with numeric alias"() {
        given:
        Long userId = 1L
        String userInfoValue = buildUserInfoValue(userId)
        Profile profile = new Profile(firstName: 'firstName', lastName: 'lastName', userId: userId, type: Type.USER)
        profileRepository.save(profile)
        String payload = """
        {
          "firstName": "updated",
          "lastName": "profile",
          "alias": "12345"
        }
        """

        expect:
        mvc.perform(put("/profile/" + profile.id)
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(payload)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isBadRequest())
    }

    def "update user profile with duplicate alias"() {
        given:
        Long userId = 1L
        String userInfoValue = buildUserInfoValue(userId)
        profileRepository.save(new Profile(firstName: 'other', lastName: 'user', userId: 2L, alias: 'taken-alias', type: Type.USER))
        Profile profile = new Profile(firstName: 'firstName', lastName: 'lastName', userId: userId, type: Type.USER)
        profileRepository.save(profile)
        String payload = """
        {
          "firstName": "updated",
          "lastName": "profile",
          "alias": "taken-alias"
        }
        """

        expect:
        mvc.perform(put("/profile/" + profile.id)
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(payload)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isBadRequest())
    }

    def "get club profiles"() {
        given:
        Long userId = 1L
        String firstName1 = "firstName1"
        String lastName1 = "lastName1"
        String firstName2 = "firstName2"
        String lastName2 = "lastName2"

        and:
        Profile profile1 = new Profile(firstName: firstName1, lastName: lastName1, userId: userId, type: Type.CLUB)
        Profile profile2 = new Profile(firstName: firstName2, lastName: lastName2, userId: userId, type: Type.CLUB)
        profileRepository.save(profile1)
        profileRepository.save(profile2)

        expect:
        mvc.perform(get("/club-profile?userId=" + userId)
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath('$[0]firstName', is(firstName1)))
                    .andExpect(jsonPath('$[0]lastName', is(lastName1)))
                    .andExpect(jsonPath('$[0]userId', is(userId.toInteger())))
                    .andExpect(jsonPath('$[0]type', is('CLUB')))
                    .andExpect(jsonPath('$[1]firstName', is(firstName2)))
                    .andExpect(jsonPath('$[1]lastName', is(lastName2)))
                    .andExpect(jsonPath('$[1]userId', is(userId.toInteger())))
                    .andExpect(jsonPath('$[1]type', is('CLUB')))
    }

    def "upload avatar"() {
        given:
        Long userId = 1L
        File avatarFile = ResourceUtils.getFile('classpath:profile/upload-avatar.json')
        String avatarFileJson = new String(Files.readAllBytes(avatarFile.toPath()))
        String userInfoValue = buildUserInfoValue(userId)

        and:
        Profile profile = new Profile(firstName: 'firstName', lastName: 'lastName', userId: userId, type: Type.USER)
        profileRepository.save(profile)

        expect:
        mvc.perform(post('/profile/' + profile.id + '/avatar')
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(avatarFileJson)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                    .andExpect(status().isOk())
    }

    def "upload avatar for wrong profile"() {
        given:
        File avatarFile = ResourceUtils.getFile('classpath:profile/upload-avatar.json')
        String avatarFileJson = new String(Files.readAllBytes(avatarFile.toPath()))
        String userInfoValue = buildUserInfoValue(1L)

        and:
        Profile profile = new Profile(firstName: 'firstName', lastName: 'lastName', userId: 2L, type: Type.USER)
        profileRepository.save(profile)

        expect:
        mvc.perform(post('/profile/' + profile.id + '/avatar')
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(avatarFileJson)
                .header(USER_INFO_HEADER, userInfoValue)
                .header(AUTHORIZATION_HEADER, TOKEN))
                .andExpect(status().isForbidden())
    }

    private String buildUserInfoValue(Long id) throws Exception {
        UserInfoDto userInfoDto = new UserInfoDto(userId: id, lang: Locale.ENGLISH.language)
        jsonMapper.writeValueAsString(userInfoDto)
    }
}

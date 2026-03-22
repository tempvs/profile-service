package club.tempvs.profile.component

import club.tempvs.profile.domain.Profile
import club.tempvs.profile.domain.Profile.Period
import club.tempvs.profile.domain.Profile.Type
import club.tempvs.profile.dto.ErrorsDto
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import spock.lang.Specification
import spock.lang.Subject
import tools.jackson.databind.json.JsonMapper

class ProfileValidatorSpec extends Specification {

    def messageSource = Mock(MessageSource)
    def jsonMapper = Mock(JsonMapper)

    @Subject
    ProfileValidator profileValidator = new ProfileValidator(messageSource, jsonMapper)

    def "validate user profile"() {
        given:
        Profile profile = new Profile(firstName: 'john', lastName: 'doe', period: Period.ANTIQUITY, type: Type.USER)

        expect:
        profileValidator.validateUserProfile(profile)
    }

    def "validate user profile with missing first name"() {
        given:
        String messageKey = 'profile.firstName.blank.error'
        String failureMessage = 'failure message'
        String serializedErrorsDto = '{errors...}'
        ErrorsDto errorsDto = new ErrorsDto()
        errorsDto.addError('firstName', failureMessage)
        Locale locale = LocaleContextHolder.locale
        Profile profile = new Profile(lastName: 'doe', period: Period.ANTIQUITY, type: Type.USER)

        when:
        profileValidator.validateUserProfile(profile)

        then:
        1 * messageSource.getMessage(messageKey, null, messageKey, locale) >> failureMessage
        1 * jsonMapper.writeValueAsString(errorsDto) >> serializedErrorsDto
        0 * _

        and:
        Exception exception = thrown IllegalArgumentException
        exception.message == serializedErrorsDto
    }

    def "validate user profile with missing last name"() {
        given:
        String messageKey = 'profile.lastName.blank.error'
        String failureMessage = 'failure message'
        String serializedErrorsDto = '{errors...}'
        ErrorsDto errorsDto = new ErrorsDto()
        errorsDto.addError('lastName', failureMessage)
        Locale locale = LocaleContextHolder.locale
        Profile profile = new Profile(firstName: 'john', period: Period.ANTIQUITY, type: Type.USER)

        when:
        profileValidator.validateUserProfile(profile)

        then:
        1 * messageSource.getMessage(messageKey, null, messageKey, locale) >> failureMessage
        1 * jsonMapper.writeValueAsString(errorsDto) >> serializedErrorsDto
        0 * _

        and:
        Exception exception = thrown IllegalArgumentException
        exception.message == serializedErrorsDto
    }
}

package club.tempvs.profile.dto;

import club.tempvs.profile.domain.Profile.Type;
import club.tempvs.profile.domain.Profile.Period;
import club.tempvs.profile.dto.validation.Scope.Create;
import lombok.Data;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@Data
public class ProfileDto {

    @Null(groups = {Create.UserProfile.class, Create.ClubProfile.class})
    private Long id;
    @Null(groups = {Create.UserProfile.class, Create.ClubProfile.class})
    private Long userId;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @Null(groups = Create.UserProfile.class)
    private String nickName;
    @Null(groups = Create.UserProfile.class)
    private String profileEmail;
    @Null(groups = Create.UserProfile.class)
    private String location;
    @Null(groups = Create.UserProfile.class)
    private String alias;
    @Null(groups = {Create.UserProfile.class, Create.ClubProfile.class})
    private Boolean isActive;
    @Null(groups = Create.UserProfile.class)
    @NotNull(groups = Create.ClubProfile.class)
    private Period period;
    @Null(groups = {Create.UserProfile.class, Create.ClubProfile.class})
    private List<PassportDto> passports;
    @Null(groups = {Create.UserProfile.class, Create.ClubProfile.class})
    private Type type;
    @Null(groups = {Create.UserProfile.class, Create.ClubProfile.class})
    private Instant createdDate;
}

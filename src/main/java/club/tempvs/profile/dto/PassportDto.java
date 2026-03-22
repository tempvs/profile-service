package club.tempvs.profile.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

@Data
public class PassportDto {

    private Long id;
    @NotBlank
    private String name;
    private String description;
    private List<Long> items;
    private Instant createdDate;
}

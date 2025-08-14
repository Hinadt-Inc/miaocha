package com.hinadt.miaocha.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** Request DTO to update alert recipients for a Logstash process. */
@Data
@Schema(description = "Update alert recipients for a Logstash process")
public class AlertRecipientsUpdateRequestDTO {

    @Schema(description = "List of recipient emails")
    @NotNull(message = "Recipients list cannot be null") private List<String> alertRecipients;
}

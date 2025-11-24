package org.goodee.startup_BE.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendPayloadDTO {

    private String content;
    private List<AttachmentFileResponseDTO> attachments;
}

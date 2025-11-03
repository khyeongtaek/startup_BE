package org.goodee.startup_BE.mail.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.mail.entity.Mail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmlServiceImpl implements EmlService{
	private final JavaMailSender mailSender;           // MimeMessage 생성용 (SMTP 설정 없어도 OK)
	@Value("${file.storage.root}") private String root; // 첨부가 쓰는 동일 루트 재사용
	
	@Override
	public String generate(Mail mail,
	                       List<String> to, List<String> cc, List<String> bcc,
	                       List<AttachmentFileResponseDTO> attachments,
	                       Employee sender) {
		try {
			// 1) 메시지 뼈대
			var msg = mailSender.createMimeMessage();
			var helper = new org.springframework.mail.javamail.MimeMessageHelper(msg, true, "UTF-8");
			
			// From / To / Cc / Bcc
			helper.setFrom(new jakarta.mail.internet.InternetAddress(
				sender.getEmail(), sender.getName(), java.nio.charset.StandardCharsets.UTF_8.name()
			));
			if (to  != null && !to.isEmpty())  helper.setTo(to.toArray(String[]::new));
			if (cc  != null && !cc.isEmpty())  helper.setCc(cc.toArray(String[]::new));
			if (bcc != null && !bcc.isEmpty()) helper.setBcc(bcc.toArray(String[]::new));
			
			// 제목/본문/날짜
			helper.setSubject(mail.getTitle());
			helper.setText(mail.getContent() == null ? "" : mail.getContent(), true); // HTML=true
			var sentDate = java.util.Date.from(mail.getSendAt()
				                                   .atZone(java.time.ZoneId.systemDefault()).toInstant());
			msg.setSentDate(sentDate);
			
			// 2) 첨부
			if (attachments != null) {
				for (var a : attachments) {
					var abs = java.nio.file.Paths.get(root).resolve(a.getStoragePath()).normalize().toFile();
					var display = jakarta.mail.internet.MimeUtility.encodeText(
						a.getOriginalName(), "UTF-8", "B"
					);
					helper.addAttachment(display, new org.springframework.core.io.FileSystemResource(abs));
				}
			}
			
			// 3) 파일로 저장(.eml)
			msg.saveChanges(); // 헤더 finalize
			String relPath = buildEmlRelPath(mail.getMailId()); // mail-eml/yyyy/MM/dd/{mailId}.eml
			var outPath = java.nio.file.Paths.get(root).resolve(relPath);
			java.nio.file.Files.createDirectories(outPath.getParent());
			try (var os = java.nio.file.Files.newOutputStream(outPath)) {
				msg.writeTo(os);
			}
			return relPath;
			
		} catch (Exception e) {
			throw new IllegalStateException("EML 생성 실패", e);
		}
	}
	
	private String buildEmlRelPath(Long mailId) {
		var d = java.time.LocalDate.now();
		return String.format("mail-eml/%d/%02d/%02d/%d.eml",
			d.getYear(), d.getMonthValue(), d.getDayOfMonth(), mailId == null ? 0L : mailId);
	}
}

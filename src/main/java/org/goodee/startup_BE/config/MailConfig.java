package org.goodee.startup_BE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {
	@Bean
	public JavaMailSender javaMailSender() {
		JavaMailSenderImpl s = new JavaMailSenderImpl();
		s.setDefaultEncoding("UTF-8");
		// SMTP 붙일 때 host/port/user/pass 설정 예정
		return s;
	}
}

package com.mechergui.cafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
public class CafeManagementSystemApplication {

	public static void main(String[] args) {
	ConfigurableApplicationContext context= SpringApplication.run(CafeManagementSystemApplication.class, args);
		//Elien a =new Elien();
		Elien a=context.getBean(Elien.class);
		a.show();
//		Elien a1=context.getBean(Elien.class);
//		a1.show();

	}

}

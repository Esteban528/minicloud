package com.estebandev.minicloud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.estebandev.minicloud.config.CustomErrorController;

@SpringBootTest
class MinicloudApplicationTests {
    @MockitoBean
    CustomErrorController customErrorController;

	@Test
	void contextLoads() {
	}

}

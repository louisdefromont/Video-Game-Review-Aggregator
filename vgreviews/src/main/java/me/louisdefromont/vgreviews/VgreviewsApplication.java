package me.louisdefromont.vgreviews;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.transaction.Transactional;

@SpringBootApplication
public class VgreviewsApplication implements CommandLineRunner{
	@Autowired
	private AdjustedScoreService averageScoreService;

	public static void main(String[] args) {
		SpringApplication.run(VgreviewsApplication.class, args);
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		averageScoreService.init();
	}

}

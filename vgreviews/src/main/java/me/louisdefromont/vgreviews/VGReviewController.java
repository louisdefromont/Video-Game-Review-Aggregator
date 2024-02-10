package me.louisdefromont.vgreviews;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.louisdefromont.vgreviews.service.OpenCriticScraperService;
import me.louisdefromont.vgreviews.service.SteamDBScraperService;
import me.louisdefromont.vgreviews.service.SteamStoreScraperService;


@RestController
@RequestMapping("/api/vgreviews")
public class VGReviewController {
	@Autowired
	private OpenCriticScraperService openCriticScraperService;
	@Autowired
	private SteamDBScraperService steamDBScraperService;
	@Autowired
	private SteamStoreScraperService steamStoreScraperService;

	@GetMapping(path = "/scrape/opencritic")
	public VideoGame scrapeOpenCritic(String title) {
		return openCriticScraperService.scrape(title);
	}

	@GetMapping(path = "/scrape/steamdb")
	public VideoGame scrapeSteamDB(String title) {
		return steamDBScraperService.scrape(title);
	}

	@GetMapping(path = "/scrape/steamstore")
	public VideoGame scrapeSteamStore(String title) {
		return steamStoreScraperService.scrape(title);
	}
}

package me.louisdefromont.vgreviews;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import me.louisdefromont.vgreviews.service.GlitchWaveScraperService;
import me.louisdefromont.vgreviews.service.OpenCriticScraperService;
import me.louisdefromont.vgreviews.service.SteamDBScraperService;
import me.louisdefromont.vgreviews.service.SteamStoreScraperService;
import me.louisdefromont.vgreviews.service.VideoGameScraperService;

@RestController
@RequestMapping("/api/")
@CrossOrigin(origins = "http://localhost:3000")
public class VGReviewController {
	@Autowired
	private VideoGameRepository videoGameRepository;

	@Autowired
	private VideoGameScraperService videoGameScraperService;
	@Autowired
	private OpenCriticScraperService openCriticScraperService;
	@Autowired
	private SteamDBScraperService steamDBScraperService;
	@Autowired
	private SteamStoreScraperService steamStoreScraperService;
	@Autowired
	private GlitchWaveScraperService glitchWaveScraperService;

	@Autowired
	private AdjustedScoreService adjustedScoreService;

	@GetMapping(path = "/scrape/opencritic")
	public VideoGame scrapeOpenCritic(String title) {
		return openCriticScraperService.scrape(title);
	}

	@GetMapping(path = "/scrape/opencritic/last90")
	public List<VideoGame> last90Releases() {
		return videoGameScraperService.last90Releases();
	}

	@GetMapping(path = "/scrape/steamdb")
	public VideoGame scrapeSteamDB(String title) {
		return steamDBScraperService.scrape(title);
	}

	@GetMapping(path = "/scrape/steamstore")
	public VideoGame scrapeSteamStore(String title) {
		return steamStoreScraperService.scrape(title);
	}

	@GetMapping(path = "/scrape/glitchwave")
	public VideoGame scrapeGlitchWave(String title) {
		return glitchWaveScraperService.scrape(title);
	}

	@GetMapping(path = "/search")
	public List<VideoGame> search(String query) {
		return videoGameRepository.findByTitleContainingIgnoreCase(query);
	}

	@GetMapping(path = "/adjusted/all")
	public List<AdjustedGameScore> allGamesByAdjustedScore() {
		List<AdjustedGameScore> adjustedGameScores = new ArrayList<>();
		Iterable<VideoGame> videoGames = videoGameRepository.findAll();
		for (VideoGame videoGame : videoGames) {
			AdjustedGameScore adjustedGameScore = new AdjustedGameScore();
			adjustedGameScore.setAdjustedScore(adjustedScoreService.calculateAdjustedScore(videoGame));
			adjustedGameScore.setVideoGame(videoGame);
			adjustedGameScores.add(adjustedGameScore);
		}
		adjustedGameScores.sort((a, b) -> Double.compare(b.getAdjustedScore(), a.getAdjustedScore()));
		return adjustedGameScores;
	}

	@GetMapping(path = "/adjusted/search")
	public List<AdjustedGameScore> searchByAdjustedScoreAndGenre(String query, @RequestParam(required = false) String genre) {
		List<AdjustedGameScore> adjustedGameScores = new ArrayList<>();
		List<VideoGame> videoGames;
		if (genre == null) {
			videoGames = videoGameRepository.findByTitleContainingIgnoreCase(query);
		} else {
			videoGames = videoGameRepository.findByTitleContainingIgnoreCaseAndGenreContainingIgnoreCase(query,
				genre);
		}
		for (VideoGame videoGame : videoGames) {
			AdjustedGameScore adjustedGameScore = new AdjustedGameScore();
			adjustedGameScore.setAdjustedScore(adjustedScoreService.calculateAdjustedScore(videoGame));
			adjustedGameScore.setVideoGame(videoGame);
			adjustedGameScores.add(adjustedGameScore);
		}
		adjustedGameScores.sort((a, b) -> Double.compare(b.getAdjustedScore(), a.getAdjustedScore()));
		return adjustedGameScores;
	}
}

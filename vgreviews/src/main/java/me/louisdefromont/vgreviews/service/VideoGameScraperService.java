package me.louisdefromont.vgreviews.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import me.louisdefromont.vgreviews.ReviewSource;
import me.louisdefromont.vgreviews.VideoGame;
import me.louisdefromont.vgreviews.VideoGameRepository;

@Service
public class VideoGameScraperService {
	@Autowired
	private VideoGameRepository videoGameRepository;
	@Autowired
	private OpenCriticScraperService openCriticScraperService;
	@Autowired
	private SteamStoreScraperService steamStoreScraperService;
	@Autowired
	private MetaCriticScraperService metaCriticScraperService;
	@Autowired
	private GlitchWaveScraperService glitchWaveScraperService;

	public List<VideoGame> last90Releases() {
		List<VideoGame> videoGames = new ArrayList<>();
		List<String> last90Releases = openCriticScraperService.last90Releases();
		List<CompletableFuture<VideoGame>> videoGameFutures = new ArrayList<>();
		for (String title : last90Releases) {
			videoGameFutures.add(scrapeTitle(title));
		}
		for (Future<VideoGame> videoGameFuture : videoGameFutures) {
			try {
				videoGames.add(videoGameFuture.get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return videoGames;
	}

	@Async("taskExecutor")
	public CompletableFuture<VideoGame> scrapeTitle(String title) {
		VideoGame videoGame = videoGameRepository.findByTitle(title);
		if (videoGame != null) {
			return CompletableFuture.completedFuture(videoGame);
		}
		VideoGame openCriticVideoGame = openCriticScraperService.scrape(title);
		VideoGame steamVideoGame = steamStoreScraperService.scrape(title);
		VideoGame metaCriticVideoGame = metaCriticScraperService.scrape(title);
		// VideoGame glitchWaveVideoGame = glitchWaveScraperService.scrape(title);
		VideoGame combinedVideoGame = combineVideoGameInfo(openCriticVideoGame, steamVideoGame);
		combinedVideoGame = combineVideoGameInfo(combinedVideoGame, metaCriticVideoGame);
		// combinedVideoGame = combineVideoGameInfo(combinedVideoGame, glitchWaveVideoGame);
		videoGameRepository.save(combinedVideoGame);
		return CompletableFuture.completedFuture(combinedVideoGame);
	}

	private VideoGame combineVideoGameInfo(VideoGame videoGame1, VideoGame videoGame2) {
		if (videoGame1 == null) {
			return videoGame2;
		} else if (videoGame2 == null) {
			return videoGame1;
		}

		VideoGame combinedVideoGame = new VideoGame();
		if (videoGame1.getTitle() != null) {
			combinedVideoGame.setTitle(videoGame1.getTitle());
		} else {
			combinedVideoGame.setTitle(videoGame2.getTitle());
		}
		if (videoGame1.getPlatforms() != null) {
			combinedVideoGame.setPlatforms(videoGame1.getPlatforms());
		}
		if (videoGame2.getPlatforms() != null) {
			for (String platform : videoGame2.getPlatforms().split(", ")) {
				if (!combinedVideoGame.getPlatforms().contains(platform)) {
					combinedVideoGame.setPlatforms(combinedVideoGame.getPlatforms() + ", " + platform);
				}
			}
		}
		if (videoGame1.getReleaseDate() != null) {
			combinedVideoGame.setReleaseDate(videoGame1.getReleaseDate());
		} else {
			combinedVideoGame.setReleaseDate(videoGame2.getReleaseDate());
		}
		if (videoGame1.getPublishers() != null) {
			combinedVideoGame.setPublishers(videoGame1.getPublishers());
		} else {
			combinedVideoGame.setPublishers(videoGame2.getPublishers());
		}
		if (videoGame1.getGenres() != null) {
			combinedVideoGame.setGenres(videoGame1.getGenres());
		} else {
			combinedVideoGame.setGenres(videoGame2.getGenres());
		}

		List<ReviewSource> reviews = new ArrayList<>();
		if (videoGame1.getReviews() != null) {
			reviews.addAll(videoGame1.getReviews());
		}
		if (videoGame2.getReviews() != null) {
			for (ReviewSource reviewSource : videoGame2.getReviews()) {
				Boolean found = false;
				for (ReviewSource review : reviews) {
					if (review.getSourceURL().equals(reviewSource.getSourceURL())) {
						found = true;
						if (reviewSource.getNumberOfReviews() > review.getNumberOfReviews()) {
							reviews.remove(review);
							reviews.add(reviewSource);
							break;
						}
					}
				}
				if (!found) {
					reviews.add(reviewSource);
				}
			}
		}
		combinedVideoGame.setReviews(reviews);

		if (videoGame1.getId() != null) {
			combinedVideoGame.setId(videoGame1.getId());
		} else {
			combinedVideoGame.setId(videoGame2.getId());
		}
		return combinedVideoGame;
	}
}

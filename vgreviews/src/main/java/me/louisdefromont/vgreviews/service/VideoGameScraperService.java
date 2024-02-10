package me.louisdefromont.vgreviews.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

	public List<VideoGame> last90Releases() {
		List<VideoGame> videoGames = new ArrayList<>();
		List<String> last90Releases = openCriticScraperService.last90Releases();
		for (String source : last90Releases) {
			VideoGame videoGame = videoGameRepository.findByReviewSource(source);
			if (videoGame == null) {
				VideoGame openCriticVideoGame = openCriticScraperService.scrapeFromSource(source);
				VideoGame steamVideoGame = steamStoreScraperService.scrape(openCriticVideoGame);
				VideoGame metaCriticVideoGame = metaCriticScraperService.scrape(openCriticVideoGame.getTitle());
				VideoGame combinedVideoGame = combineVideoGameInfo(openCriticVideoGame, steamVideoGame);
				combinedVideoGame = combineVideoGameInfo(combinedVideoGame, metaCriticVideoGame);
				videoGameRepository.save(combinedVideoGame);
				videoGames.add(combinedVideoGame);
			} else {
				videoGames.add(videoGame);
			}
		}

		return videoGames;
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
			reviews.addAll(videoGame2.getReviews());
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

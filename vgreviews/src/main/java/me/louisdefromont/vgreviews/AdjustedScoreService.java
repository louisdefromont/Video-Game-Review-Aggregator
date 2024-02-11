package me.louisdefromont.vgreviews;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Service
public class AdjustedScoreService {
	@Autowired
	private VideoGameRepository videoGameRepository;

	public double metaCriticAverageCriticScore;
	public double metaCriticAverageUserScore;
	public double openCriticAverageScore;
	public double steamAverageScore;
	public double glitchWaveAverageScore;
	public double overallAverageScore;

	public void init() {
		Iterable<VideoGame> videoGames = videoGameRepository.findAll();
		double metaCriticAverageCriticScore = 0;
		double metaCriticAverageUserScore = 0;
		double openCriticAverageScore = 0;
		double steamAverageScore = 0;
		double overallAverageScore = 0;
		if (videoGames == null || !videoGames.iterator().hasNext()) {
			return;
		}

		double metaCriticCriticScoreSum = 0;
		double metaCriticUserScoreSum = 0;
		double openCriticScoreSum = 0;
		double steamScoreSum = 0;
		double glitchWaveScoreSum = 0;

		double metaCriticCriticScoreCount = 0;
		double metaCriticUserScoreCount = 0;
		double openCriticScoreCount = 0;
		double steamScoreCount = 0;
		double glitchWaveScoreCount = 0;

		for (VideoGame videoGame : videoGames) {
			for (ReviewSource review : videoGame.getReviews()) {
				if (review.getSourceName().equals("MetaCriticCritic")) {
					metaCriticCriticScoreSum += review.getAverageScore();
					metaCriticCriticScoreCount++;
				} else if (review.getSourceName().equals("MetaCriticUser")) {
					metaCriticUserScoreSum += review.getAverageScore();
					metaCriticUserScoreCount++;
				} else if (review.getSourceName().equals("OpenCritic")) {
					openCriticScoreSum += review.getAverageScore();
					openCriticScoreCount++;
				} else if (review.getSourceName().equals("Steam")) {
					steamScoreSum += review.getAverageScore();
					steamScoreCount++;
				} else if (review.getSourceName().equals("GlitchWave")) {
					glitchWaveScoreSum += review.getAverageScore();
					glitchWaveScoreCount++;
				}
			}

		}

		metaCriticAverageCriticScore = metaCriticCriticScoreSum / metaCriticCriticScoreCount;
		metaCriticAverageUserScore = metaCriticUserScoreSum / metaCriticUserScoreCount;
		openCriticAverageScore = openCriticScoreSum / openCriticScoreCount;
		steamAverageScore = steamScoreSum / steamScoreCount;
		glitchWaveAverageScore = glitchWaveScoreSum / glitchWaveScoreCount;
		overallAverageScore = (metaCriticAverageCriticScore + metaCriticAverageUserScore + openCriticAverageScore
				+ steamAverageScore + glitchWaveAverageScore) / 5.0;
	}

	public double calculateAdjustedScore(VideoGame videoGame) {
		double userScoreSum = 0;
		double criticScoreSum = 0;
		double userScoreCount = 0;
		double criticScoreCount = 0;

		for (ReviewSource review : videoGame.getReviews()) {
			if (review.getSourceName().equals("MetaCriticCritic")) {
				criticScoreSum += ((review.getAverageScore() - metaCriticAverageCriticScore) + overallAverageScore) * review.getNumberOfReviews();
				criticScoreCount += review.getNumberOfReviews();
			} else if (review.getSourceName().equals("MetaCriticUser")) {
				userScoreSum += ((review.getAverageScore() - metaCriticAverageUserScore) + overallAverageScore) * review.getNumberOfReviews();
				userScoreCount += review.getNumberOfReviews();
			} else if (review.getSourceName().equals("OpenCritic")) {
				criticScoreSum += ((review.getAverageScore() - openCriticAverageScore) + overallAverageScore) * review.getNumberOfReviews();
				criticScoreCount += review.getNumberOfReviews();
			} else if (review.getSourceName().equals("Steam")) {
				userScoreSum += ((review.getAverageScore() - steamAverageScore) + overallAverageScore) * review.getNumberOfReviews();
				userScoreCount += review.getNumberOfReviews();
			} else if (review.getSourceName().equals("GlitchWave")) {
				userScoreSum += ((review.getAverageScore() - glitchWaveAverageScore) + overallAverageScore) * review.getNumberOfReviews();
				userScoreCount += review.getNumberOfReviews();
			}
		}

		if (userScoreCount == 0 || criticScoreCount == 0) {
			return 0;
		}

		double userScore = userScoreSum / userScoreCount;
		double criticScore = criticScoreSum / criticScoreCount;
		return (userScore + criticScore) / 2.0;
	}
}

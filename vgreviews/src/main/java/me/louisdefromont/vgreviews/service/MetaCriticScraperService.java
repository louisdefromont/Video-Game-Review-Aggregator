package me.louisdefromont.vgreviews.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import me.louisdefromont.vgreviews.ReviewSource;
import me.louisdefromont.vgreviews.VideoGame;

@Service
public class MetaCriticScraperService {
	public VideoGame scrape(String title) {
		HttpResponse<String> response = Unirest.get(
				"https://internal-prod.apigee.fandom.net/v1/xapi/finder/metacritic/autosuggest/"
						+ title.replace(" ", "%20") + "?apiKey=1MOZgmNFxvmljaQR1X9KAij9Mo4xAY3u")
				.header("authority", "internal-prod.apigee.fandom.net")
				.header("accept", "application/json, text/plain, */*")
				.header("accept-language", "en-US,en;q=0.9,fr;q=0.8")
				.header("cache-control", "no-cache")
				.header("origin", "https://www.metacritic.com")
				.header("pragma", "no-cache")
				.header("referer", "https://www.metacritic.com/")
				.header("sec-ch-ua", "\"Not A(Brand\";v=\"99\", \"Google Chrome\";v=\"121\", \"Chromium\";v=\"121\"")
				.header("sec-ch-ua-mobile", "?0")
				.header("sec-ch-ua-platform", "\"Windows\"")
				.header("sec-fetch-dest", "empty")
				.header("sec-fetch-mode", "cors")
				.header("sec-fetch-site", "cross-site")
				.header("user-agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
				.asString();
		try {
			JSONArray items = new JSONObject(response.getBody()).getJSONObject("data").getJSONArray("items");
			for (int i = 0; i < items.length(); i++) {
				JSONObject item = items.getJSONObject(i);
				if (item.get("type").equals("game-title")) {
					if (item.getString("title").equalsIgnoreCase(title)) {
						return scrapeFromSource("https://www.metacritic.com/game/" + item.getString("slug"));
					}
				}
			}
		} catch (JSONException e) {
			System.out.println("Error searching for " + title + " on Metacritic");
		}

		return null;
	}

	public VideoGame scrapeFromSource(String url) {
		try {
			Document doc = Jsoup.connect(url).get();
			String title = doc.select("div.c-productHero_title div").text().trim();
			String date = doc.select("div.g-text-xsmall .u-text-uppercase").text().trim();
			LocalDate releaseDate;
			try {
				releaseDate = LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"));
			} catch (Exception e) {
				releaseDate = null;
			}
			VideoGame videoGame = new VideoGame();
			videoGame.setTitle(title);
			videoGame.setReleaseDate(releaseDate);

			List<ReviewSource> reviews = new ArrayList<>();

			try {
				double criticAverageScore = Double
						.parseDouble(
								doc.selectFirst(
										".g-inner-spacing-bottom-medium div.c-productScoreInfo_scoreNumber span")
										.text());
				double criticReviewCount = Double
						.parseDouble(
								doc.selectFirst(".g-inner-spacing-bottom-medium .c-productScoreInfo_reviewsTotal span")
										.text().split(" ")[2]);
				ReviewSource criticReviewSource = new ReviewSource();
				criticReviewSource.setAverageScore(criticAverageScore);
				criticReviewSource.setNumberOfReviews(criticReviewCount);
				criticReviewSource.setSourceURL("https://www.metacritic.com"
						+ doc.selectFirst(".g-inner-spacing-bottom-medium .c-productScoreInfo_reviewsTotal a")
								.attr("href"));
				criticReviewSource.setSourceName("MetaCriticCritic");
				criticReviewSource.setScrapeDate(LocalDate.now());
				reviews.add(criticReviewSource);
			} catch (NumberFormatException | NullPointerException e) {
			}

			try {
				double userAverageScore = 10.0 * Double
						.parseDouble(doc.selectFirst(
								".c-productScoreInfo.u-clearfix:not(.g-inner-spacing-bottom-medium) div.c-productScoreInfo_scoreNumber span")
								.text());
				double userReviewCount = Double
						.parseDouble(doc.selectFirst(
								".c-productScoreInfo.u-clearfix:not(.g-inner-spacing-bottom-medium) .c-productScoreInfo_reviewsTotal span")
								.text().split(" ")[2]);
				ReviewSource userReviewSource = new ReviewSource();
				userReviewSource.setAverageScore(userAverageScore);
				userReviewSource.setNumberOfReviews(userReviewCount);
				userReviewSource.setSourceURL("https://www.metacritic.com"
						+ doc.selectFirst(
								".c-productScoreInfo.u-clearfix:not(.g-inner-spacing-bottom-medium) .c-productScoreInfo_reviewsTotal a")
								.attr("href"));
				userReviewSource.setSourceName("MetaCriticUser");
				userReviewSource.setScrapeDate(LocalDate.now());
				reviews.add(userReviewSource);
			} catch (NumberFormatException | NullPointerException e) {
			}

			videoGame.setReviews(reviews);

			return videoGame;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}

package me.louisdefromont.vgreviews.service;

import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import me.louisdefromont.vgreviews.ReviewSource;
import me.louisdefromont.vgreviews.VideoGame;

@Service
public class GlitchWaveScraperService {
	public VideoGame scrape(String title) {
		HttpResponse<String> response = Unirest.get("https://api.sonemic.com/1/search?q=" + title.replace(" ", "%20") + "&t=games")
				.header("authority", "api.sonemic.com")
				.header("accept", "application/json, text/javascript, */*; q=0.01")
				.header("accept-language", "en-US,en;q=0.9,fr;q=0.8")
				.header("cache-control", "no-cache")
				.header("origin", "https://glitchwave.com")
				.header("pragma", "no-cache")
				.header("referer", "https://glitchwave.com/")
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
			JSONArray results = new JSONObject(response.getBody()).getJSONArray("results");
			for (int i = 0; i < results.length(); i++) {
				JSONObject result = results.getJSONObject(i);
				if (result.getString("type").equals("game")) {
					if (result.getJSONArray("name").getString(0).trim().equalsIgnoreCase(title)) {
						return scrapeFromSource("https://glitchwave.com" + result.getString("url"));
					}
				}
			}
		} catch (NullPointerException e) {
		}

		return null;
	}

	public VideoGame scrapeFromSource(String url) {
		try {
			Document doc = Jsoup.connect(url).get();
			String title = doc.selectFirst("h1.page_object_header_title").text().trim();
			VideoGame videoGame = new VideoGame();
			videoGame.setTitle(title);

			try {
				double averageScore = 20.0
						* Double.parseDouble(doc.selectFirst(".rating_number.rating_number_game").text().trim());
				double numberOfReviews = Double.parseDouble(
						doc.selectFirst(".rating_card.scale_game div.rating_card_description:not(#rating_card_charts)")
								.text().split(" ")[0].replace(",", ""));
				ReviewSource reviewSource = new ReviewSource();
				reviewSource.setAverageScore(averageScore);
				reviewSource.setNumberOfReviews(numberOfReviews);
				reviewSource.setSourceURL(url);
				reviewSource.setSourceName("GlitchWave");
				reviewSource.setScrapeDate(java.time.LocalDate.now());
				videoGame.setReviews(List.of(reviewSource));
			} catch (NumberFormatException | NullPointerException e) {
			}

			return videoGame;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}

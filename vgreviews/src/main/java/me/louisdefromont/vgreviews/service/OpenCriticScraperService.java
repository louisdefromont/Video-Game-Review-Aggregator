package me.louisdefromont.vgreviews.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import me.louisdefromont.vgreviews.ReviewSource;
import me.louisdefromont.vgreviews.VideoGame;

@Service
public class OpenCriticScraperService {

	public List<String> last90Releases() {
		List<String> videoGameTitles = new ArrayList<>();
		Document doc;
		Elements games = new Elements();
		int page = 1;
		do {
			try {
				System.out.println("Collecting links from page " + page + "...");
				doc = Jsoup.connect("https://opencritic.com/browse/all/last90/date?page=" + page).get();
				games = doc.select(".game-name a");
				for (Element game : games) {
					videoGameTitles.add(game.text());
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			page++;
		} while (games.size() > 0);
		return videoGameTitles;
	}

	public VideoGame scrape(VideoGame videoGame) {
		for (ReviewSource reviewSource : videoGame.getReviews()) {
			if (reviewSource.getSourceURL().contains("opencritic")) {
				return scrapeFromSource(reviewSource.getSourceURL());
			}
		}
		return scrape(videoGame.getTitle());
	}

	public VideoGame scrape(String title) {
		HttpResponse<String> response = Unirest
				.get("https://api.opencritic.com/api/meta/search?criteria=" + title.replace(" ", "%20"))
				.header("authority", "api.opencritic.com")
				.header("accept", "application/json, text/plain, */*")
				.header("accept-language", "en-US,en;q=0.9,fr;q=0.8")
				.header("cache-control", "no-cache")
				.header("origin", "https://opencritic.com")
				.header("pragma", "no-cache")
				.header("referer", "https://opencritic.com/")
				.header("sec-ch-ua", "\"Not A(Brand\";v=\"99\", \"Google Chrome\";v=\"121\", \"Chromium\";v=\"121\"")
				.header("sec-ch-ua-mobile", "?0")
				.header("sec-ch-ua-platform", "\"Windows\"")
				.header("sec-fetch-dest", "empty")
				.header("sec-fetch-mode", "cors")
				.header("sec-fetch-site", "same-site")
				.header("user-agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
				.asString();
		JSONArray games = new JSONArray(response.getBody());
		JSONObject game = games.getJSONObject(0);
		long id = game.getLong("id");
		String name = game.getString("name");

		return scrape(id, name);
	}

	public VideoGame scrapeFromSource(String url) {
		String[] parts = url.split("/");
		long id = Long.parseLong(parts[parts.length - 2]);
		String name = parts[parts.length - 1];
		return scrape(id, name);
	}

	public VideoGame scrape(long id, String name) {
		String url = "https://opencritic.com/game/" + id + "/" + name.replace(" ", "-");
		try {
			Document doc = Jsoup.connect(url).get();
			String title = doc.selectFirst("h1").text();
			Element platformsElement = doc.selectFirst("div.platforms");
			LocalDate releaseDate = LocalDate.parse(platformsElement.text().split(" - ")[0],
					java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"));
			String platforms = platformsElement.text().split(" - ")[1];
			Element publishersElement = doc.selectFirst("div.companies");
			String publishers = publishersElement.text();
			VideoGame videoGame = new VideoGame();
			videoGame.setTitle(title);
			videoGame.setPlatforms(platforms);
			videoGame.setReleaseDate(releaseDate);
			videoGame.setPublishers(publishers);

			try {
				double averageScore = Double.parseDouble(doc.selectFirst("div.game-scores .inner-orb").text());
				double criticCount = Double.parseDouble(doc.selectFirst(".text-right.my-1").text().split(" ")[2]);
				ReviewSource reviewSource = new ReviewSource();
				reviewSource.setSourceURL(url);
				reviewSource.setSourceName("OpenCritic");
				reviewSource.setScrapeDate(LocalDate.now());
				reviewSource.setAverageScore(averageScore);
				reviewSource.setNumberOfReviews(criticCount);
				videoGame.setReviews(List.of(reviewSource));
			} catch (NullPointerException e) {
			}

			return videoGame;
		} catch (IOException e) {
			System.out.println("Timeout Error scraping " + url);
		}

		return null;
	}
}

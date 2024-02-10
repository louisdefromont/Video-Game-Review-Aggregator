package me.louisdefromont.vgreviews.service;

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
import me.louisdefromont.vgreviews.ReviewSource;
import me.louisdefromont.vgreviews.VideoGame;

@Service
public class SteamStoreScraperService {
	public VideoGame scrape(VideoGame videoGame) {
		if (videoGame.getReviews() != null) {
			for (ReviewSource reviewSource : videoGame.getReviews()) {
				if (reviewSource.getSource().contains("steampowered")) {
					return scrapeFromSource(reviewSource.getSource());
				}
			}
		}
		return scrape(videoGame.getTitle());
	}

	public VideoGame scrape(String title) {
		HttpResponse<String> response = Unirest.get(
				"https://store.steampowered.com/search/suggest?term=" + title.replace(" ", "%2B")
						+ "&f=games&cc=US&realm=1&l=english&v=22291786&use_store_query=1&use_search_spellcheck=1")
				.header("Accept", "*/*")
				.header("Accept-Language", "en-US,en;q=0.9,fr;q=0.8")
				.header("Cache-Control", "no-cache")
				.header("Connection", "keep-alive")
				.header("Pragma", "no-cache")
				.header("Referer", "https://store.steampowered.com/")
				.header("Sec-Fetch-Dest", "empty")
				.header("Sec-Fetch-Mode", "cors")
				.header("Sec-Fetch-Site", "same-origin")
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
				.header("X-Requested-With", "XMLHttpRequest")
				.header("sec-ch-ua", "\"Not A(Brand\";v=\"99\", \"Google Chrome\";v=\"121\", \"Chromium\";v=\"121\"")
				.header("sec-ch-ua-mobile", "?0")
				.header("sec-ch-ua-platform", "\"Windows\"")
				.asString();
		Document doc = Jsoup.parse(response.getBody());
		try {
			long id = Long.parseLong(doc.select("a").first().attr("data-ds-appid"));
			return scrape(id);
		} catch (NullPointerException e) {
			// System.out.println("Cannot find appid for " + title);
		} catch (NumberFormatException e) {
			System.out.println("Error parsing appid from " + title);
		}
		return null;
	}

	public VideoGame scrapeFromSource(String url) {
		String[] parts = url.split("/");
		long id = Long.parseLong(parts[parts.length - 1]);
		return scrape(id);
	}

	public VideoGame scrape(long id) {
		String url = "https://store.steampowered.com/app/" + id;
		try {
			Map<String, String> cookies = new HashMap<>();
			cookies.put("birthtime", "502261201");
			cookies.put("wants_mature_content", "1");
			cookies.put("lastagecheckage", "1-0-1986");
			Document doc = Jsoup.connect(url).cookies(cookies).get();
			String name = doc.select("div#appHubAppName").first().text();
			String genres = "";
			Elements appTags = doc.select(".app_tag:not(.add_button)");
			for (int i = 0; i < appTags.size(); i++) {
				genres += appTags.get(i).text();
				if (i < appTags.size() - 1) {
					genres += ", ";
				}
			}
			VideoGame videoGame = new VideoGame();
			videoGame.setTitle(name);
			videoGame.setGenres(genres);
			videoGame.setPlatforms("PC");

			double averageScore;
			double numberOfReviews;
			try {
				averageScore = Double.parseDouble(
						doc.select("div.user_reviews_summary_row").get(1).attr("data-tooltip-html").split("% ")[0]);
				numberOfReviews = Double.parseDouble(
						doc.select("div.user_reviews_summary_row").get(1).attr("data-tooltip-html").split(" ")[3]
								.replace(",", ""));
			} catch (NumberFormatException e) {
				try {
					averageScore = Double.parseDouble(
							doc.select("div.user_reviews_summary_row").get(0).attr("data-tooltip-html").split("% ")[0]);
					numberOfReviews = Double.parseDouble(
							doc.select("div.user_reviews_summary_row").get(0).attr("data-tooltip-html").split(" ")[3]
									.replace(",", ""));
				} catch (NumberFormatException e2) {
					return videoGame;
				}
			}
			ReviewSource reviewSource = new ReviewSource();
			reviewSource.setSource(url);
			reviewSource.setScrapeDate(java.time.LocalDate.now());
			reviewSource.setAverageScore(averageScore);
			reviewSource.setNumberOfReviews(numberOfReviews);
			videoGame.setReviews(List.of(reviewSource));

			return videoGame;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

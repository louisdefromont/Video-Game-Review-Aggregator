package me.louisdefromont.vgreviews.service;

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
public class SteamDBScraperService {
	public VideoGame scrape(String title) {
		HttpResponse<String> response = Unirest.get(
				"https://94he6yatei-dsn.algolia.net/1/indexes/steamdb/?x-algolia-agent=SteamDB%2BAutocompletion&x-algolia-application-id=94HE6YATEI&x-algolia-api-key=338033d8e504a8a3f98452c637d713b9&hitsPerPage=15&attributesToSnippet=null&attributesToHighlight=name&attributesToRetrieve=objectID%2ClastUpdated&query="
						+ title.replace(" ", "%2B"))
				.header("Accept", "*/*")
				.header("Accept-Language", "en-US,en;q=0.9,fr;q=0.8")
				.header("Cache-Control", "no-cache")
				.header("Connection", "keep-alive")
				.header("Origin", "https://steamdb.info")
				.header("Pragma", "no-cache")
				.header("Referer", "https://steamdb.info/")
				.header("Sec-Fetch-Dest", "empty")
				.header("Sec-Fetch-Mode", "cors")
				.header("Sec-Fetch-Site", "cross-site")
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
				.header("sec-ch-ua", "\"Not A(Brand\";v=\"99\", \"Google Chrome\";v=\"121\", \"Chromium\";v=\"121\"")
				.header("sec-ch-ua-mobile", "?0")
				.header("sec-ch-ua-platform", "\"Windows\"")
				.asString();
		JSONArray hits = new JSONObject(response.getBody()).getJSONArray("hits");
		long id = hits.getJSONObject(0).getLong("objectID");

		return scrape(id);
	}

	public VideoGame scrape(long id) {
		String url = "https://steamdb.info/app/" + id;
		try {
			Document doc = Jsoup.connect(url).get();
			String name = doc.select("h1").first().text();
			List<String> storeTags = doc.select(".store-tags a").eachText();
			String genres = String.join(", ", storeTags);
			VideoGame videoGame = new VideoGame();
			videoGame.setTitle(name);
			videoGame.setGenres(genres);

			double averageScore = Double.parseDouble(doc.select("[itemprop=\"ratingValue\"]").first().attr("content"));
			double numberOfReviews = Double.parseDouble(doc.select("[itemprop=\"reviewCount\"]").first().attr("content"));
			ReviewSource reviewSource = new ReviewSource();
			reviewSource.setSourceURL(url);
			reviewSource.setScrapeDate(java.time.LocalDate.now());
			reviewSource.setAverageScore(averageScore);
			reviewSource.setNumberOfReviews(numberOfReviews);
			videoGame.setReviews(List.of(reviewSource));

			return videoGame;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return null;
	}
}

package com.tfc.word.auto.collect.study;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.tfc.word.auto.collect.config.Configuration;
import com.tfc.word.auto.collect.repository.entity.FetchOrig;
import com.tfc.word.auto.collect.study.service.WordAnalyzerService;

public class Robot implements Runnable {
	private String startUrl;
	private WordAnalyzerService wordService;

	public Robot(String startUrl) {
		this.startUrl = startUrl;
	}

	public void run() {
		parse(startUrl);
		// 抓取链接中的内容，然后将抓取的内容交给WordAnalyzerService进行处理。再抓取其中的一个，其过程就像蜘蛛。
	}

	private void parse(String fetchUrl) {
		try {
			FetchOrig fo = Configuration.repo.getOrgi(fetchUrl);
			if (fo != null) {
				// 已经存在过，则表示已经分析过。
				return;
			}
			Document doc = Jsoup.connect(fetchUrl).get();
			String text = doc.text();
			wordService.analyzer(text);
			Configuration.repo.saveOrgi(fetchUrl);
			Elements eles = doc.getElementsByTag("a");
			if (eles != null && eles.size() > 0) {
				for (int i = 0; i < eles.size(); i++) {
					Element ele = eles.get(i);
					String url = StringUtils.trim(ele.attr("href"));
					if (StringUtils.isNotEmpty(url)) {
						parse(url);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

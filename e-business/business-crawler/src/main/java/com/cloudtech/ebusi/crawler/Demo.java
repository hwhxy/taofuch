package com.cloudtech.ebusi.crawler;

import net.vidageek.crawler.PageCrawler;
import net.vidageek.crawler.component.DefaultLinkNormalizer;

import com.cloudtech.ebusi.crawler.parser.HcParser;

public class Demo {
	public static void main(String[] args) {
		// String baseUrl =
		// "http://search.china.alibaba.com/selloffer/--1046622.html?showStyle=img&sortType=booked&descendOrder=true&filt=y&lessThanQuantityBegin=true&cleanCookie=false";
		String baseUrl = "http://list.b2b.hc360.com/company/kw/%CE%E5%BD%F0.html";
		PageCrawler crawler = new PageCrawler(baseUrl, new WebDownloader(), new DefaultLinkNormalizer(baseUrl));
		// crawler.crawl(new WebListVisitor(baseUrl, new AliParser(null)));
		crawler.crawl(new WebListVisitor(baseUrl, new HcParser(null)));
	}
}

package com.scoop.crawler.weibo.parser;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.scoop.crawler.weibo.entity.WeiboComment;
import com.scoop.crawler.weibo.entity.WeiboPersonInfo;
import com.scoop.crawler.weibo.repository.DataSource;
import com.scoop.crawler.weibo.repository.mysql.Weibo;
import com.scoop.crawler.weibo.request.ExploreRequest;
import com.scoop.crawler.weibo.request.failed.FailedHandler;
import com.scoop.crawler.weibo.util.Logger;

/**
 * 微博评论解析器。
 * 
 * @author taofucheng
 * 
 */
public class CommentParser extends Parser {
	private String preContent = "";
	private int tryTimes = 0;

	public CommentParser(DataSource dataSource, FailedHandler handler) {
		super(dataSource, handler);
	}

	public void fetchWeiboComments(WebDriver driver, Weibo w, DefaultHttpClient client) {
		if (driver == null) {
			return;
		}
		try {
			// 打开微博页面，抓取其评论信息。
			driver.navigate().to(w.getUrl());
			Thread.sleep(2000);
			Logger.log("解析微博[" + w + "]的评论信息……");
			Elements eles = getComments(driver);
			if (eles == null || eles.isEmpty()) {
				Logger.log("当前微博没有评论信息！");
			}
			// 获取所有评论信息，并进行循环处理。
			int cnt = 0;
			while (eles != null && eles.size() > 0) {
				Element tmp = null;
				for (int i = 0; i < eles.size(); i++) {
					Logger.log("解析其中一条评论信息……");
					tmp = eles.get(i);
					if (tmp != null) {
						// 获取对应的评论者主页URL。
						try {
							String userInfoUrl = parseToUserUrl(tmp);
							WeiboPersonInfo person = new WeiboPersonInfo(userInfoUrl, client);
							person.setHandler(getHandler());
							WeiboComment comment = new WeiboComment(tmp);
							comment.setWeiboId(w.getWeiboId());
							comment.setPerson(person);
							dataSource.saveComment(comment);
							cnt++;
						} catch (Exception e) {
							Logger.log("当前评论解析失败！错误信息：" + e);
							e.printStackTrace();
						}
						Logger.log("当前评论解析完毕！");
					} else {
						Logger.log("当前评论为空，解析下一条！");
					}
				}
				// 加载下一页评论，并进行分析
				eles = loadNextPage(driver);
			}
			afterSave(w, cnt);
		} catch (Exception e) {
			System.err.println("解析微博[" + w + "]的评论失败！" + e);
			e.printStackTrace();
		}
		Logger.log("微博[" + w + "]的评论信息解析完毕！");
	}

	/**
	 * 当前微博所有评论保存完之后的操作
	 * 
	 * @param w
	 * @param cnt
	 *            当前抓取的总共的评论数
	 */
	protected void afterSave(Weibo w, long cnt) {
		if (StringUtils.isNotBlank(w.getWeiboId()) && cnt > w.getCommentNum()) {
			Weibo update = new Weibo();
			update.setWeiboId(w.getWeiboId());
			update.setCommentNum(cnt);
			dataSource.mergeWeibo(update);
		}
		Logger.log("当前微博评论解析完毕！共解析出：" + cnt + "个，记录的微博评论的总数：" + w.getCommentNum() + "个");
	}

	/**
	 * 获取Driver页面中的每个评论信息。
	 * 
	 * @param driver
	 * @return
	 */
	private Elements getComments(WebDriver driver) {
		String html = ExploreRequest.getPageHtml(driver);
		// html = cut(html, detailStart);//获取是就是已经解析好的内容！
		Elements eles = Jsoup.parse(html).getElementsByAttributeValue("class", "comment_lists");
		if (eles != null) {
			return eles.select("dd");
		} else {
			return null;
		}
	}

	/**
	 * 获取下一页的信息
	 * 
	 * @param client
	 * @return
	 */
	private Elements loadNextPage(WebDriver driver) {
		try {
			WebElement ele = driver.findElement(By.className("comment_lists"));
			if (ele == null) {
				return null;
			}
			ele = ele.findElement(By.className("W_pages_minibtn"));
			if (ele == null) {
				return null;
			}
			ele = ele.findElement(By.linkText("下一页"));
			if (ele == null) {
				return null;
			}
			ele.click();
			Thread.sleep(2000);// 等待两秒，等数据加载完毕！
			String html = ExploreRequest.getPageHtml(driver);
			if (html.equals(preContent) && tryTimes < 3) {
				// 如果此次获取的内容与上一次一样，说明翻页失败，重新刷新一下页面再翻页
				driver.navigate().refresh();
				++tryTimes;
				return loadNextPage(driver);
			} else {
				preContent = html;
				tryTimes = 0;
			}
		} catch (Throwable e) {
			return null;
		}
		return getComments(driver);
	}

	/**
	 * 获取个人信息的URL。
	 * 
	 * @param tmp
	 * @param weiboUrl
	 * @return
	 */
	protected String parseToUserUrl(Element tmp) {
		Elements eles = tmp.getElementsByTag("a");
		String userInfoUrl = eles.get(0).attr("usercard");
		userInfoUrl = userInfoUrl.startsWith("id=") ? userInfoUrl.substring(3) : userInfoUrl;
		userInfoUrl = "http://weibo.com/" + userInfoUrl + "/info";
		return userInfoUrl;
	}

	/**
	 * 评论内容
	 * 
	 * @param tmp
	 * @return
	 */
	protected String parseToMsg(Elements tmp) {
		boolean hasContent = false;// 是否是评论内容。
		String comment = "";// 评论的内容
		List<TextNode> nl = tmp.first().textNodes();
		for (int i = 0; i < nl.size(); i++) {
			TextNode n = nl.get(i);
			String t = StringUtils.trim(n.text());
			if (StringUtils.isBlank(t)) {
				continue;
			}
			if (hasContent) {
				// 如果已经是评论，则这个文本内容时，表示结束了！
				comment += t;
				break;
			} else {
				// 如果还没有找到评论内容，且这是文本信息，即：首次发现文本信息，则说明这是评论内容的开始！
				hasContent = true;
				t = t.startsWith("：") ? t.substring(1) : t;// 去除评论者后面的冒号
				comment += t;
			}
		}
		return comment;
	}
}

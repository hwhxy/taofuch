package com.scoop.crawler.weibo.runnable;

import org.apache.http.impl.client.DefaultHttpClient;
import org.openqa.selenium.WebDriver;

import com.scoop.crawler.weibo.entity.LogonInfo;
import com.scoop.crawler.weibo.parser.httpclient.FansParserHttpclient;
import com.scoop.crawler.weibo.parser.httpclient.FollowParserHttpclient;
import com.scoop.crawler.weibo.repository.DataSource;
import com.scoop.crawler.weibo.repository.mysql.User;
import com.scoop.crawler.weibo.request.ExploreRequest;
import com.scoop.crawler.weibo.request.SinaWeiboRequest;
import com.scoop.crawler.weibo.request.failed.FailedHandler;
import com.scoop.crawler.weibo.util.Logger;
import com.scoop.crawler.weibo.util.ThreadUtils;

/**
 * 微博用户的关系信息，如粉丝、关注等。
 * 
 * @author taofucheng
 * 
 */
public class WeiboUserRelationRunnable extends Thread implements Runnable {
	protected DataSource dataSource;
	protected FailedHandler handler;
	private long preTime = System.currentTimeMillis();

	public WeiboUserRelationRunnable(DataSource dataSource, FailedHandler handler) {
		this.dataSource = dataSource;
		this.handler = handler;
	}

	public void run() {
		DefaultHttpClient client = ThreadUtils.allocateHttpClient();
		FansParserHttpclient fansp = new FansParserHttpclient(dataSource, handler);
		FollowParserHttpclient followP = new FollowParserHttpclient(dataSource, handler);
		Logger.log("开始解析所有用户粉丝与关注信息……");
		try {
			WebDriver driver = ExploreRequest.getDriver("http://weibo.com/");
			if (driver == null) {
				Logger.log("浏览器打开失败！停止运行！");
				return;
			}
			// 循环获取用户信息
			for (User u = dataSource.getOneUnfetchedUser(); u != null; u = dataSource.getOneUnfetchedUser()) {
				try {
					if (System.currentTimeMillis() - preTime >= LogonInfo.DRIVER_INTERVAL) {
						driver.quit();
						driver = ExploreRequest.getDriver("http://weibo.com/");
						if (driver == null) {
							Logger.log("浏览器打开失败！停止运行！");
							return;
						} else {
							SinaWeiboRequest.setCookieToClient(client, driver);
						}
						preTime = System.currentTimeMillis();
					}
					fansp.fetchFans(u, driver, client);
					followP.fetchFollows(u, driver, client);
				} catch (Exception e) {
					System.err.println("解析用户信息[" + u + "]出错！");
					e.printStackTrace();
				}
			}
			if (driver != null) {
				driver.quit();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		ThreadUtils.freeThread();
		ThreadUtils.finishUserRelation();
		Logger.log("所有用户粉丝与关注信息解析完毕！");
	}
}

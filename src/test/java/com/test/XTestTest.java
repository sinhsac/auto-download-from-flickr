package com.test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.test.MyDriver.TypeForDownload;
import com.test.WriterCSV.Types;

public class XTestTest {
	public static Logger log = LoggerFactory.getLogger(XTestTest.class);
	
	private static String baseUrl = "https://flickr.com/photos/";
	
	@Test
	public void firstTestCommonTesting() {
		MyDriver.loginIntoFlick(TypeForDownload.URL);

		for(String strUserId: MyDriver.userIds()) {
			WriterCSV userImageInfo = WriterCSV.getInstance(strUserId, Types.IMG_INFO);
			Set<String> links = new LinkedHashSet<>();
			processForUser(strUserId, links, userImageInfo);
		}
	}



	public void processForUser(String userId, Set<String> links, WriterCSV userImageInfo) {
		System.out.println("\n\n[INFO] \t Process for user " + userId);
		String basePage = baseUrl + userId + "/";
		processForPage(userId, 1, -1, basePage, links, userImageInfo);
		Integer totalPages = calcTotalPage();

		if (totalPages > 1) {
			for (int page = 2; page <= totalPages; page++) {
				processForPage(userId, page, totalPages, basePage, links, userImageInfo);
			}
		}
		
		System.out.println(String.format("found %s images link", links.size()));
		
		System.out.println("\n[INFO] \t for user " + userId + " ====> done");
	}

	private void processForPage(String userId, int page, int totalPage, String basePage, Set<String> links, WriterCSV userImageInfo) {
		MyDriver.getDriver(TypeForDownload.URL).get(basePage + "page" + page);
		MyDriver.waitXSecond(5);
		
		if (totalPage == -1 || page == totalPage) {
			simpleScrollToBottom();
		} else {
			scrollToBottom();
		}
		
		processListTagAImage(userId, page, totalPage, links, userImageInfo);
	}
	
	private void scrollToBottom() {
		JavascriptExecutor js = (JavascriptExecutor) MyDriver.getDriver(TypeForDownload.URL);
		js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
		MyDriver.waitXSecond(4);
		for(int i=0; i<20; i++) {
			List<WebElement> elements = MyDriver.getDriver(TypeForDownload.URL).findElements(By.cssSelector(".interaction-view > div > a"));
			if (elements.size() > 90) {
				break;
			}
			
			js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
			MyDriver.waitXSecond(4);
		}
	}
	
	private void simpleScrollToBottom() {
		JavascriptExecutor js = (JavascriptExecutor) MyDriver.getDriver(TypeForDownload.URL);
		js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
		MyDriver.waitXSecond(4);
		List<WebElement> elements = MyDriver.getDriver(TypeForDownload.URL).findElements(By.className("flickr-dots"));
		if (elements == null || elements.isEmpty()) {
			js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
			
			elements = MyDriver.getDriver(TypeForDownload.URL).findElements(By.className("flickr-dots"));
			if (elements != null && !elements.isEmpty()) {
				MyDriver.waitXSecond(4);
				
				js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
				
				elements = MyDriver.getDriver(TypeForDownload.URL).findElements(By.className("flickr-dots"));
				if (elements != null && !elements.isEmpty()) {
					MyDriver.waitXSecond(4);
					
					js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
					
					elements = MyDriver.getDriver(TypeForDownload.URL).findElements(By.className("flickr-dots"));
					if (elements != null && !elements.isEmpty()) {
						MyDriver.waitXSecond(4);
					}
				}
			}
		}
		
	}

	private Integer calcTotalPage() {
		List<WebElement> elements = MyDriver.getDriver(TypeForDownload.URL).findElements(By.cssSelector(".view.pagination-view.photostream > a > span"));
		if (elements == null || elements.isEmpty()) {
			return 1;
		}

		return elements.stream().map(o -> o.getText()).filter(o -> StringUtils.isNumeric(o))
				.mapToInt(s -> Integer.parseInt(s)).max().orElse(1);
	}

	private void processListTagAImage(String userId, Integer page, Integer totalPage, Set<String> links, WriterCSV userImageInfo) {
		System.out.println("\n\n");
		System.out.println(String.format("process for page %s/%s", page, totalPage));
		List<WebElement> elements = MyDriver.getDriver(TypeForDownload.URL).findElements(By.cssSelector(".interaction-view > div > a"));
		List<String> photoIds = new ArrayList<>();
		for (WebElement element : elements) {
			// String label = element.getAttribute("aria-label");
			String link = element.getAttribute("href");
			if (StringUtils.isBlank(link)) {
				continue;
			}
			if (!validLinkImg(link)) {
				continue;
			}
			
			userImageInfo.writeNewLine(link);
			Pattern pattern = Pattern.compile("(.+)\\/" + userId + "\\/([0-9]+)\\/in\\/dateposted");
			Matcher matcher = pattern.matcher(link);
			if (!matcher.find()) {
				continue;
			}
			if (matcher.groupCount() < 2) {
				continue;
			}
			if (StringUtils.isBlank(matcher.group(2))) {
				continue;
			}
			links.add(link);
			photoIds.add(matcher.group(2));

		}
		
		System.out.println(photoIds.stream().collect(Collectors.joining(", ")));
		System.out.println(String.format("found %s images", photoIds.size()));
	}
	
	private boolean validLinkImg(String link) {
		if (StringUtils.isBlank(link)) {
			return false;
		}

		if (link.endsWith("dateposted/") || link.endsWith("dateposted")) {
			return true;
		}
		return false;
	}
}

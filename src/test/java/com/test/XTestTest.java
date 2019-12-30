package com.test;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import com.test.WriterCSV.Types;

public class XTestTest {
	private static WebDriver driver = null;
	private static List<String> userIds = new ArrayList<>();
	private static String baseUrl = "https://flickr.com/photos/";
	private static Properties properties = new Properties();
	
	
	private void loginIntoFlick() {
		System.out.println("\n\n[INFO] \t Login into flickr");
		driver.get("https://flickr.com/signin");
		driver.findElement(By.id("login-email")).sendKeys(properties.getProperty("username"));

		logClick();

		driver.findElement(By.id("login-password")).sendKeys(properties.getProperty("password"));
		logClick();
		driver.manage().window().maximize();
		System.out.println("[INFO] \t Login successful");
	}
	
	@Test
	public void firstTestCommonTesting() {
		initDriver();
		
		loginIntoFlick();
		
		for(String strUserId: userIds) {
			WriterCSV userImageInfo = WriterCSV.getInstance(strUserId, Types.IMG_INFO);
			WriterCSV userFinalImage = WriterCSV.getInstance(strUserId, Types.FINAL_IMG);
			
			Set<String> links = new LinkedHashSet<>();
			processForUser(strUserId, links, userImageInfo, userFinalImage);
		}
	}

	private void initDriver() {
		System.out.println("\n\n[INFO] \t init driver");

		try (InputStream input = new FileInputStream("config/flickr.properties")) {
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String strUserIds = properties.getProperty("userids");
		assertFalse(StringUtils.isBlank(strUserIds));
		
		if (strUserIds.contains(",")) {
			userIds = Arrays.stream(strUserIds.split(","))
					.filter(s -> StringUtils.isNotBlank(s))
					.map(s -> s.trim())
					.collect(Collectors.toList());
		} else {
			userIds.add(strUserIds);
		}
		
		assertFalse(userIds == null || userIds.isEmpty());
		File browserFile = new File("files/" + properties.getProperty("driver"));
		System.setProperty("webdriver.gecko.driver", browserFile.getAbsolutePath());
		driver = new FirefoxDriver();
		System.out.println("\n[INFO] \t init done");
	}

	public void processForUser(String userId, Set<String> links, WriterCSV userImageInfo, WriterCSV userFinalImage) {
		System.out.println("\n\n[INFO] \t Process for user " + userId);
		String basePage = baseUrl + userId + "/";
		processForPage(userId, 1, basePage, links, userImageInfo);
		Integer totalPages = calcTotalPage();

		if (totalPages > 1) {
			for (int i = 2; i <= totalPages; i++) {
				processForPage(userId, i, basePage, links, userImageInfo);
			}
		}
		
		System.out.println(String.format("found %s images link", links.size()));
		
		thirdTestGetSizeImgs(links, userFinalImage);
		System.out.println("\n[INFO] \t for user " + userId + " ====> done");
	}

	public void thirdTestGetSizeImgs(Set<String> links, WriterCSV userFinalImage) {
		System.out.println("\n\nprocess for collect images");
		for (String link : links) {
			String sizeLinks = link.replace("in/dateposted/", "sizes/3k/");
			driver.get(sizeLinks);

			String[] imgLink = new String[1];
			WebElement lastTagA = getLastSize(imgLink);
			if (lastTagA != null) {
				System.out.println("need to change to another link to get max size");
				lastTagA.click();
				lastTagA = getLastSize(imgLink);
			}

			if (StringUtils.isNotBlank(imgLink[0])) {
				System.out.println(imgLink[0]);
				userFinalImage.writeNewLine(imgLink[0]);
				continue;
			}

			System.out.println("not found img");
		}
	}
	
	private WebElement getLastSize(String[] imgLink) {
		List<WebElement> elements = driver.findElements(By.cssSelector("ol.sizes-list > li > ol > li"));
		if (elements == null || elements.isEmpty()) {
			return null;
		}

		WebElement lastElement = elements.get(elements.size() - 1);
		List<WebElement> allTagsA = lastElement.findElements(By.tagName("a"));

		if (allTagsA == null || allTagsA.isEmpty()) {
			imgLink[0] = getImgLink();
			return null;
		}

		return allTagsA.get(0);
	}

	private String getImgLink() {
		List<WebElement> elements = driver.findElements(By.cssSelector("#allsizes-photo > img"));
		if (elements == null || elements.isEmpty()) {
			return "";
		}
		return elements.get(0).getAttribute("src");
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

	private void processForPage(String userId, int i, String basePage, Set<String> links, WriterCSV userImageInfo) {
		driver.get(basePage + "page" + i);
		waitXSecond(5);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
		waitXSecond(8);
		processListTagAImage(userId, i, links, userImageInfo);
	}

	private Integer calcTotalPage() {
		List<WebElement> elements = driver.findElements(By.cssSelector(".view.pagination-view.photostream > a > span"));
		if (elements == null || elements.isEmpty()) {
			return 1;
		}

		return elements.stream().map(o -> o.getText()).filter(o -> StringUtils.isNumeric(o))
				.mapToInt(s -> Integer.parseInt(s)).max().orElse(1);
	}

	private void processListTagAImage(String userId, Integer page, Set<String> links, WriterCSV userImageInfo) {
		System.out.println("\n\n");
		System.out.println(String.format("process for page %s", page));
		List<WebElement> elements = driver.findElements(By.cssSelector(".interaction-view > div > a"));
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

	private void waitXSecond(long second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException ignore) {
		}
	}

	private void logClick() {
		List<WebElement> elements = driver.findElements(By.tagName("button"));
		for (WebElement element : elements) {
			String logInBtn = element.getAttribute("data-testid");
			if (StringUtils.isNotBlank(logInBtn) && logInBtn.equalsIgnoreCase("identity-form-submit-button")) {
				element.click();
				break;
			}
		}
		waitXSecond(3);
	}
}

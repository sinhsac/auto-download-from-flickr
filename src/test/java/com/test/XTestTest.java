package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
	private static String userId = "angus219437"; //"99002729@N07"; // "angus219437";
	private static String basePage = "https://flickr.com/photos/" + userId + "/";
	private static Set<String> links = new LinkedHashSet<>();
	private static WriterCSV userImageInfo = null;
	private static WriterCSV userFinalImage = null;
	private static Properties properties = new Properties();
	

	@Test
	public void preTest() {
		System.out.println("\n\ninit driver");
		links = new LinkedHashSet<>();
		
		try (InputStream input = new FileInputStream("config/flickr.properties")) {
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		userImageInfo = WriterCSV.getInstance(userId, Types.IMG_INFO);
		userFinalImage = WriterCSV.getInstance(userId, Types.FINAL_IMG);
		
		File browserFile = new File("files/" + properties.getProperty("driver"));
		
		//browserFile = new File("files\\phantomjs.exe");
		//System.setProperty("phantomjs.binary.path", browserFile.getAbsolutePath());
		
		System.setProperty("webdriver.gecko.driver", browserFile.getAbsolutePath());
		driver = new FirefoxDriver();
	}

	@Test
	public void firstTestLoginToFlick() {
		System.out.println("\n\nLogin into flickr");
		driver.get("https://flickr.com/signin");
		driver.findElement(By.id("login-email")).sendKeys(properties.getProperty("username"));

		logClick();

		driver.findElement(By.id("login-password")).sendKeys(properties.getProperty("password"));
		logClick();
		driver.manage().window().maximize();
	}

	@Test
	public void secondTestCollectAllLinks() {
		processForPage(1);
		Integer totalPages = calcTotalPage();

		if (totalPages > 1) {
			for (int i = 2; i <= totalPages; i++) {
				processForPage(i);
			}
		}
		System.out.println(String.format("found %s images link", links.size()));
	}

	@Test
	public void thirdTestGetSizeImgs() {
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

	private void processForPage(int i) {
		driver.get(basePage + "page" + i);
		waitXSecond(5);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
		waitXSecond(8);
		processListTagAImage(i);
	}

	private Integer calcTotalPage() {
		List<WebElement> elements = driver.findElements(By.cssSelector(".view.pagination-view.photostream > a > span"));
		if (elements == null || elements.isEmpty()) {
			return 1;
		}

		return elements.stream().map(o -> o.getText()).filter(o -> StringUtils.isNumeric(o))
				.mapToInt(s -> Integer.parseInt(s)).max().orElse(1);
	}

	private void processListTagAImage(Integer page) {
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
			Pattern pattern = Pattern.compile("(.+)([0-9]{11})\\/in\\/dateposted\\/");
			Matcher matcher = pattern.matcher(link);
			if (matcher.matches() && StringUtils.isNotBlank(matcher.group(2))) {
				links.add(link);
				photoIds.add(matcher.group(2));
			}
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

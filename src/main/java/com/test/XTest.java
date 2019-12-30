package com.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;


public class XTest {
	private static WebDriver driver = null;
	private static String userId = "99002729@N07"; // "angus219437";
	private static String basePage = "https://flickr.com/photos/" + userId + "/";
	private static Set<String> links = new LinkedHashSet<>();
	private static CSVPrinter printer = null;
	private static FileWriter out = null;

	@Test
	public void preTest() {
		System.out.println("\n\ninit driver");
		File file = new File("files\\firebug-1.8.1.xpi");
		links = new LinkedHashSet<>();
		if (!file.exists()) {
			return;
		}

		File folder = new File("csv");
		if (!folder.exists()) {
			folder.mkdir();
		}

		File csvOutputFile = new File(folder, userId + ".csv");
		try {
			out = new FileWriter(csvOutputFile);
			printer = new CSVPrinter(out, CSVFormat.EXCEL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		File geckoFile = new File("files\\geckodriver.exe");

		System.setProperty("webdriver.gecko.driver", geckoFile.getAbsolutePath());
		driver = new FirefoxDriver();
	}

	@Test
	public void firstTestLoginToFlick() {
		System.out.println("\n\nLogin into flickr");
		driver.get("https://flickr.com/signin");
		driver.findElement(By.id("login-email")).sendKeys("onggiatuoi9x@yahoo.com");

		logClick();

		driver.findElement(By.id("login-password")).sendKeys("Zaq!23456789+");
		logClick();
		driver.manage().window().maximize();
	}

	@Test
	public void secondTestCollectAllLinks() {
		processForPage(1);
		Integer totalPages = calcTotalPage();

		if (totalPages > 1) {
			return;
		}
		if (totalPages > 1) {
			for (int i = 2; i <= totalPages; i++) {
				processForPage(i);
			}
		}
		try {
			printer.println();
			printer.close();
		} catch (IOException e) {
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
		for (WebElement element : elements) {
			// String label = element.getAttribute("aria-label");
			String link = element.getAttribute("href");
			if (StringUtils.isBlank(link)) {
				continue;
			}
			if (!validLinkImg(link)) {
				continue;
			}
			links.add(link);
			try {
				printer.printRecord(link);
				System.out.println(String.format("saved %s to csv", link));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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

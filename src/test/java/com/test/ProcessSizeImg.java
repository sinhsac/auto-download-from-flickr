package com.test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.test.MyDriver.TypeForDownload;
import com.test.WriterCSV.Types;

public class ProcessSizeImg {
	
	@Test
	public void downloadAllData() {
		MyDriver.loginIntoFlick(TypeForDownload.IMG);
		
		for(String strUserId: MyDriver.userIds()) {
			WriterCSV userFinalImage = WriterCSV.getInstance(strUserId, Types.FINAL_IMG);
			Set<String> links = null;
			try {
				links = new LinkedHashSet<String>(FileUtils.readLines(WriterCSV.getInstance(strUserId, Types.IMG_INFO).getFile(), "utf-8"));
			} catch (IOException e) {}
			if (links == null || links.isEmpty()) {
				continue;
			}
			thirdTestGetSizeImgs(links, userFinalImage);
		}
	}
	
	
	public void thirdTestGetSizeImgs(Set<String> links, WriterCSV userFinalImage) {
		System.out.println("\n\nprocess for collect images");
		int counter = 0;
		int itemAt = 0;
		for (String link : links) {
			String sizeLinks = link.replace("in/dateposted/", "sizes/3k/");
			MyDriver.getDriver(TypeForDownload.IMG).get(sizeLinks);

			String[] imgLink = new String[1];
			WebElement lastTagA = getLastSize(imgLink);
			if (lastTagA != null) {
				System.out.println("need to change to another link to get max size");
				lastTagA.click();
				lastTagA = getLastSize(imgLink);
			}

			if (StringUtils.isNotBlank(imgLink[0])) {
				counter++;
				itemAt++;
				if (counter == 5) {
					counter = 0;
					System.out.print(String.format(" Item(s) at %s", itemAt));
					System.out.println("");
				}
				System.out.print(imgLink[0].substring(imgLink[0].lastIndexOf("/")) + " => ");
				userFinalImage.writeNewLine(imgLink[0]);
				continue;
			}

			System.out.println("not found img");
		}
	}
	
	
	private WebElement getLastSize(String[] imgLink) {
		List<WebElement> elements = MyDriver.getDriver(TypeForDownload.IMG).findElements(By.cssSelector("ol.sizes-list > li > ol > li"));
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
		List<WebElement> elements = MyDriver.getDriver(TypeForDownload.IMG).findElements(By.cssSelector("#allsizes-photo > img"));
		if (elements == null || elements.isEmpty()) {
			return "";
		}
		return elements.get(0).getAttribute("src");
	}

	
}

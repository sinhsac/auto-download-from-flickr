package com.test;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.css.parser.CSSErrorHandler;
import com.gargoylesoftware.css.parser.CSSException;
import com.gargoylesoftware.css.parser.CSSParseException;
import com.gargoylesoftware.htmlunit.BrowserVersion;

public class MyDriver extends HtmlUnitDriver {
	public MyDriver() {
		super(new BrowserVersion.BrowserVersionBuilder(BrowserVersion.FIREFOX_60)
				.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:71.0) Gecko/20100101 Firefox/71.0")
				.build(), true);
		this.getWebClient().setCssErrorHandler(new CSSErrorHandler() {
			
			@Override
			public void warning(CSSParseException exception) throws CSSException {
				
			}
			
			@Override
			public void fatalError(CSSParseException exception) throws CSSException {
				
			}
			
			@Override
			public void error(CSSParseException exception) throws CSSException {
				
			}
		});
	}
}

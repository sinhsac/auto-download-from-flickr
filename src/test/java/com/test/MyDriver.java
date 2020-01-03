package com.test;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.test.modal.MyCookie;
import com.test.utils.JsonSerializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.proxy.CaptureType;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;

public class MyDriver {
	public static Logger log = LoggerFactory.getLogger(MyDriver.class);
	private static WebDriver driver = null;
	public static Properties properties = new Properties();
	private static List<String> userIds = new ArrayList<>();
	public static Integer proxyPort = -1;
	
	public enum TypeForDownload {
		URL, IMG
	}

	public static WebDriver getDriver(TypeForDownload type) {
		if (driver == null) {
			initDriver(type);
		}
		return driver;
	}
	
	private static HttpProxyServer getHttpProxyServer() {
		HttpProxyServer server = DefaultHttpProxyServer.bootstrap()
				.withPort(6969)
				.withFiltersSource(new HttpFiltersSourceAdapter() {
					@Override
					public HttpFilters filterRequest(HttpRequest originalRequest) {
						return new HttpFiltersAdapter(originalRequest) {
							
							@Override
							public HttpResponse clientToProxyRequest(HttpObject httpObject) {
								if (httpObject instanceof HttpRequest) {
									HttpRequest request = (HttpRequest) httpObject;
									
									return fillterRequest(request);
								}
								
								return null;
							}
							
							private HttpResponse fillterRequest(HttpRequest request) {
								
								if (!request.getMethod().equals(HttpMethod.GET) && !request.getMethod().equals(HttpMethod.CONNECT)) {
									return generateResponse(HttpResponseStatus.BAD_GATEWAY, "BAD_GATEWAY");
								}
								
								String url = request.getUri();
								
								if (url.equalsIgnoreCase("search.services.mozilla.com:443") 
										|| url.equalsIgnoreCase("flickr.com:443")
										|| url.equalsIgnoreCase("www.flickr.com:443")
										|| url.equalsIgnoreCase("identity.flickr.com:443")
										) {
									return null;
								}
								
								if (!url.endsWith("/3k/") && !url.endsWith("/4k/") && !url.endsWith(".com") && !url.endsWith("/login") && !url.endsWith("/signin")) {
									return generateResponse(HttpResponseStatus.BAD_GATEWAY, "BAD_GATEWAY");
								}
								
								return null;
							}

							private HttpResponse generateResponse(HttpResponseStatus status, String strContent) {
								byte[] bytes = strContent.getBytes(Charset.forName("UTF-8"));
								ByteBuf content = Unpooled.copiedBuffer(bytes);
								HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
								response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
								response.headers().set("Content-Type", "text/html; charset=UTF-8");
								response.headers().set("Date", ProxyUtils.formatDate(new Date()));
								response.headers().set(HttpHeaders.Names.CONNECTION, "close");
								return response;
							}
						};
					}
				}).start();
		return server;
	}
	
	
	private static Proxy getProxy(TypeForDownload type) {
		Proxy xxx = new Proxy();
		if (type.equals(TypeForDownload.IMG)) {
			HttpProxyServer proxyServer = getHttpProxyServer();
			xxx.setHttpProxy(proxyServer.getListenAddress().getHostName() + ":" + proxyServer.getListenAddress().getPort());
			xxx.setSslProxy(proxyServer.getListenAddress().getHostName() + ":" + proxyServer.getListenAddress().getPort());
		} else {
			BrowserMobProxy proxy = new BrowserMobProxyServer();
			proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
			proxy.addRequestFilter(new RequestFilter() {
	            @Override
	            public HttpResponse filterRequest(HttpRequest request, HttpMessageContents contents, HttpMessageInfo messageInfo) {
	                
	                String url = request.getUri();
	                String urx = messageInfo.getOriginalUrl();
	                
	                if (url.equalsIgnoreCase("www.google.com:443")) {
	                	return generateResponse(HttpResponseStatus.BAD_GATEWAY, "BAD_GATEWAY");
	                }
	                
	                if (urx.endsWith(".woff")
	                		|| urx.endsWith(".woff2") 
	                		|| urx.endsWith(".png") || urx.endsWith(".jpg") || urx.endsWith(".jpeg")
	                		) {
	                	return generateResponse(HttpResponseStatus.BAD_GATEWAY, "BAD_GATEWAY");
	                }
	                if (url.startsWith("/services/rest")) {
	                	//QueryStringEncoder encoder = new QueryStringEncoder(url, Charset.forName("utf-8"));
	                	QueryStringDecoder decoder = new QueryStringDecoder(url, Charset.forName("utf-8"));
	                	Map<String, List<String>> map = decoder.parameters();
	                	if (map.containsKey("method") && !map.get("method").isEmpty()) {
	                		List<String> methods = map.get("method");
	                		String method = methods.get(0);
	                		if (method.equalsIgnoreCase("flickr.people.getPhotos")) {
	                			System.out.println(url);
	                		}
	                	}
	                }
	                
	                return null;
	            }
	            
	            
	            
	            private HttpResponse generateResponse(HttpResponseStatus status, String strContent) {
					byte[] bytes = strContent.getBytes(Charset.forName("UTF-8"));
					ByteBuf content = Unpooled.copiedBuffer(bytes);
					HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
					response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
					response.headers().set("Content-Type", "text/html; charset=UTF-8");
					response.headers().set("Date", ProxyUtils.formatDate(new Date()));
					response.headers().set(HttpHeaders.Names.CONNECTION, "close");
					return response;
				}
	        });
		    proxy.start(0);
		    xxx = ClientUtil.createSeleniumProxy(proxy);
		}
		return xxx;
	}

	private static void initDriver(TypeForDownload type) {
		log.debug("init driver");

		try (InputStream input = new FileInputStream("config/flickr.properties")) {
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String strUserIds = properties.getProperty("userids");
		assertFalse(StringUtils.isBlank(strUserIds));

		if (strUserIds.contains(",")) {
			userIds = Arrays.stream(strUserIds.split(",")).filter(s -> StringUtils.isNotBlank(s)).map(s -> s.trim())
					.collect(Collectors.toList());
		} else {
			userIds.add(strUserIds);
		}

		assertFalse(userIds == null || userIds.isEmpty());

		String browserType = properties.getProperty("browser");

		assertFalse(StringUtils.isBlank(browserType));

		Boolean enableJs = Boolean.valueOf(MyDriver.properties.getProperty("browser.js"));
		DesiredCapabilities dc = new DesiredCapabilities();
		dc.setJavascriptEnabled(enableJs);

		if (browserType.equalsIgnoreCase("firefox")) {
			File browserFile = new File("files/" + properties.getProperty("driver"));
			System.setProperty("webdriver.gecko.driver", browserFile.getAbsolutePath());

			FirefoxOptions options = new FirefoxOptions().addPreference("permissions.default.image", 2);
			options.merge(dc);
			
			Proxy seleniumProxy = getProxy(type);
			
			options.setProxy(seleniumProxy);

			driver = new FirefoxDriver(options);
			

		} else if (browserType.equalsIgnoreCase("html")) {
			driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_60, true);
		}
		System.out.println("\n[INFO] \t init done");
	}

	public static void loginIntoFlick(TypeForDownload type) {
		log.debug("Login into flickr");
		File configCookie = new File("config/cookie.json");
		MyDriver.getDriver(type).get("https://flickr.com/signin");
		List<MyCookie> cookies = new ArrayList<>();
		if (configCookie.exists()) {
			cookies = JsonSerializer.file2JsonObject(configCookie, MyCookie.class);
		}

		if (!cookies.isEmpty()) {
			cookies.forEach(cookie -> driver.manage().addCookie(cookie.build()));
			MyDriver.getDriver(type).get("https://flickr.com/signin");
		}

		if (!driver.getCurrentUrl().contains("identity")) {
			return;
		}

		MyDriver.getDriver(type).findElement(By.id("login-email")).sendKeys(MyDriver.properties.getProperty("username"));

		logClick(type);

		MyDriver.getDriver(type).findElement(By.id("login-password")).sendKeys(MyDriver.properties.getProperty("password"));
		logClick(type);
		MyDriver.getDriver(type).manage().window().maximize();
		System.out.println("[INFO] \t Login successful");

		Set<Cookie> cookiesForCurrentURL = driver.manage().getCookies();
		JsonSerializer.object2JsonIntoFile(cookiesForCurrentURL, true, configCookie);
	}

	public static List<String> userIds() {
		return userIds;
	}

	private static void logClick(TypeForDownload type) {
		List<WebElement> elements = MyDriver.getDriver(type).findElements(By.tagName("button"));
		for (WebElement element : elements) {
			String logInBtn = element.getAttribute("data-testid");
			if (StringUtils.isNotBlank(logInBtn) && logInBtn.equalsIgnoreCase("identity-form-submit-button")) {
				element.click();
				break;
			}
		}
		MyDriver.waitXSecond(3);
	}

	public static void waitXSecond(long second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException ignore) {
		}
	}
}

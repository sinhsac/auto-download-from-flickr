package com.test.modal;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openqa.selenium.Cookie;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MyCookie implements Serializable {
	private static final long serialVersionUID = 4115876353625612383L;

	public String name;
	public String value;
	public String path;
	public String domain;
	public Date expiry;
	public boolean isSecure;
	public boolean isHttpOnly;

	public MyCookie() {}

	@Override
	public String toString() {
		return name + "=" + value
				+ (expiry == null ? ""
						: "; expires=" + new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z").format(expiry))
				+ ("".equals(path) ? "" : "; path=" + path) + (domain == null ? "" : "; domain=" + domain)
				+ (isSecure ? ";secure;" : "");
	}

	/**
	 * Two cookies are equal if the name and value match
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof MyCookie) {
			MyCookie cookie = (MyCookie) o;
			if (!name.equals(cookie.name)) {
				return false;
			}
			return !(value != null ? !value.equals(cookie.value) : cookie.value != null);
		}
		
		if (o instanceof Cookie) {
			Cookie cookie = (Cookie) o;
			if (!name.equals(cookie.getName())) {
				return false;
			}
			return !(value != null ? !value.equals(cookie.getValue()) : cookie.getValue() != null);
		}
		
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public Cookie build() {
		return new Cookie(name, value, domain, path, expiry, isSecure, isHttpOnly);
	}
}

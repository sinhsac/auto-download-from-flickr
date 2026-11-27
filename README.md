# auto-download-from-flickr

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Selenium](https://img.shields.io/badge/Selenium-3.141.59-43B02A.svg)](https://www.selenium.dev/)
[![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36.svg)](https://maven.apache.org/)

A Java and Selenium WebDriver automation tool that crawls Flickr user photostreams, saves authenticated session cookies, and extracts maximum-resolution image URLs in bulk using proxy-based traffic filtering for faster scraping.

## Key Features

- **Automated Flickr Authentication & Session Persistence**: Signs in via Selenium and caches browser cookies to `config/cookie.json` using Jackson so subsequent runs reuse the authenticated session.
- **Multi-User Photostream Crawling**: Processes one or multiple comma-separated Flickr user IDs, detects pagination automatically, scrolls to trigger lazy-loaded photos, and exports all photo page URLs.
- **Maximum-Resolution Image URL Extraction**: Visits each photo's size page (starting from `/sizes/3k/`), automatically selects the highest available resolution from Flickr's size list, and saves direct image URLs to disk.
- **Bandwidth-Saving Proxy Filtering**:
  - Uses **BrowserMob Proxy** during photostream crawling to block heavy static assets (`.jpg`, `.jpeg`, `.png`, `.woff`, `.woff2`) and monitor `flickr.people.getPhotos` requests.
  - Uses an embedded **LittleProxy** server (port `6969`) plus Firefox's `permissions.default.image = 2` setting during size resolution to skip unnecessary network requests and image rendering.
- **Headless Server Support**: Supports running Firefox silently on Linux/Ubuntu servers via `Xvfb`, as well as an `HtmlUnitDriver` mode.

## Tech Stack

- **Language**: Java 8 (`1.8`)
- **Build Tool**: Apache Maven (with included Maven Wrapper `./mvnw` and `mvnw.cmd`)
- **Browser Automation**: Selenium WebDriver (`3.141.59`), GeckoDriver (Firefox), HtmlUnitDriver (`2.32.1`)
- **Proxy & Traffic Filtering**: BrowserMob Proxy (`2.1.5`), LittleProxy / Netty
- **Data & Utilities**: Jackson (`2.10.1`), Apache Commons IO / CSV / Text / Codec, JUnit 4 (`4.12`), SLF4J / Logback

## Project Structure

```text
auto-download-from-flickr/
├── files/                                          # Bundled browser drivers (geckodriver.exe, phantomjs.exe, etc.)
├── src/
│   ├── main/java/com/test/
│   │   ├── modal/MyCookie.java                     # Serializable Selenium cookie model for JSON persistence
│   │   ├── utils/JsonSerializer.java               # Jackson JSON read/write helper
│   │   └── XTest.java                              # Standalone prototype crawler
│   └── test/java/com/test/
│       ├── MyDriver.java                           # WebDriver, proxy filters, properties loader, and Flickr login
│       ├── WriterCSV.java                          # Per-user output writer (csv/<userId>/IMG_INFO.txt & FINAL_IMG.txt)
│       ├── XTestTest.java                          # Stage 1: Crawl photostreams and collect photo page URLs
│       └── ProcessSizeImg.java                     # Stage 2: Resolve highest-resolution image URLs from collected links
├── mvnw / mvnw.cmd                                 # Maven Wrapper scripts
├── pom.xml                                         # Maven dependencies and build configuration
└── LICENSE                                         # MIT License
```

## Prerequisites

- **JDK 8** installed and `JAVA_HOME` configured
- **Mozilla Firefox** (when using `browser=firefox`)
- A compatible **GeckoDriver** binary placed inside `files/` (a Windows `geckodriver.exe` is included; on Linux/macOS, place the matching `geckodriver` binary in `files/`)
- A valid **Flickr account** (email and password)

## Configuration

Create `config/flickr.properties` in the project root (the `config/` directory is gitignored):

```properties
# Comma-separated list of Flickr user IDs or path aliases
userids=99002729@N07

# Browser driver mode: "firefox" or "html" (HtmlUnitDriver)
browser=firefox

# Enable JavaScript in WebDriver capabilities
browser.js=true

# GeckoDriver filename located inside the files/ directory
driver=geckodriver.exe

# Flickr login credentials
username=your-email@example.com
password=your-password
```

On the first successful login, session cookies are saved automatically to `config/cookie.json` and reused on subsequent runs.

## Usage

The workflow runs as two JUnit test stages executed via the Maven Wrapper (`./mvnw` on Linux/macOS or `mvnw.cmd` on Windows):

### 1. Collect Photostream Photo URLs

Crawls all photostream pages for each configured user ID and writes photo page URLs to `csv/<userId>/IMG_INFO.txt`:

```bash
./mvnw clean -Dtest=XTestTest test
```

### 2. Extract Highest-Resolution Image URLs

Reads `csv/<userId>/IMG_INFO.txt`, resolves the largest available image size for each photo, and writes direct image URLs to `csv/<userId>/FINAL_IMG.txt`:

```bash
./mvnw clean -Dtest=ProcessSizeImg test
```

### Output Files

Generated files are saved under the gitignored `csv/` directory:

- `csv/<userId>/IMG_INFO.txt` — Collected Flickr photo page links (`.../in/dateposted/`)
- `csv/<userId>/FINAL_IMG.txt` — Direct maximum-resolution image `src` URLs ready for batch downloading (e.g., with `wget -i` or `aria2c -i`)

## Running Headless Firefox on Ubuntu Server (Xvfb)

To run Firefox silently on a headless Ubuntu server without a desktop environment:

```bash
sudo apt-add-repository ppa:mozillateam/firefox-next
sudo apt-get update
sudo apt-get install firefox xvfb
Xvfb :10 -ac &
export DISPLAY=:10
```

Then verify Firefox starts on the virtual display (if needed) and run the Maven command:

```bash
./mvnw clean -Dtest=ProcessSizeImg test
```

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

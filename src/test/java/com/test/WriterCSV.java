package com.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class WriterCSV {
		private File file = null;
		
		public enum Types {
			IMG_INFO, FINAL_IMG
		}
		
		private WriterCSV(String userId, Types type) {
			File folder = new File("csv");
			if (!folder.exists()) {
				folder.mkdir();
			}

			File userFolder = new File(folder, userId);
			if (!userFolder.exists()) {
				userFolder.mkdir();
			}
			
			
			file = new File(userFolder, type.name() + ".txt");
		}
		
		public void writeNewLine(String line) {
			try {
				FileUtils.writeStringToFile(file, line + "\n", "utf-8", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public File getFile() {
			return file;
		}
		
		public static WriterCSV getInstance(String userId, Types type) {
			return new WriterCSV(userId, type);
		}
	}
package com.bhjelmar.util;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

@Log4j2
public class Files {

	@SneakyThrows
	public static void serializeData(Object o, String name) {
		FileOutputStream fos = new FileOutputStream(name);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(o);
		oos.close();
		fos.close();
		log.debug("Serialized data is saved in " + name + ".");
	}

	@SneakyThrows
	public static <T> T deserializeData(String fileName) {
		T obj;

		FileInputStream fis = new FileInputStream(fileName);
		ObjectInputStream ois = new ObjectInputStream(fis);
		obj = (T) ois.readObject();
		ois.close();
		fis.close();

		log.debug("Deserialized " + fileName + " successfully.");
		return obj;
	}

	@SneakyThrows
	public static String grepStreamingFile(String lolHome, boolean startAtBeginOfFile, String key) {
		Path dir = Paths.get(lolHome + "/Logs/LeagueClient Logs");
		Optional<Path> lastFilePath = java.nio.file.Files.list(dir)
			.filter(f -> !java.nio.file.Files.isDirectory(f) && StringUtils.endsWith(f.toString(), "_LeagueClient.log"))
			.max(Comparator.comparingLong(f -> f.toFile().lastModified()));
		if (lastFilePath.isPresent()) {
			File lolLog = lastFilePath.get().toFile();

			// start at the tail of file
			long lastKnownPosition = lolLog.length() - 1;
			if (startAtBeginOfFile) {
				lastKnownPosition = 0;
			}

			while (true) {
				int runEveryNSeconds = 1;
				Thread.sleep(runEveryNSeconds);
				long fileLength = lolLog.length();
				if (fileLength > lastKnownPosition) {
					RandomAccessFile readWriteFileAccess = new RandomAccessFile(lolLog, "rw");
					readWriteFileAccess.seek(lastKnownPosition);
					String line = null;
					while ((line = readWriteFileAccess.readLine()) != null) {
						if (line.contains(key)) {
							return line;
						}
					}
					lastKnownPosition = readWriteFileAccess.getFilePointer();
					readWriteFileAccess.close();
				}
			}
		}
		return null;
	}

	@SneakyThrows
	public static int pollLogForSummonerId() {
		Thread.sleep(10000);
		return 0;
	}

}

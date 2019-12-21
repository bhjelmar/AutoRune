package com.bhjelmar.util;

import com.bhjelmar.data.Champion;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
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

			log.info("Scraping file {}", lolLog.getPath());
			while (true) {
				Thread.sleep(200);
				Optional<Path> mostRecentFile = java.nio.file.Files.list(dir)
					.filter(f -> !java.nio.file.Files.isDirectory(f) && StringUtils.endsWith(f.toString(), "_LeagueClient.log"))
					.max(Comparator.comparingLong(f -> f.toFile().lastModified()));
				if (mostRecentFile.isPresent()) {
					File mostRecentLoLLog = mostRecentFile.get().toFile();
					if (!lolLog.getPath().equals(mostRecentLoLLog.getPath())) {
						log.info("LoL Log file switched. from {} to {}", lolLog.getPath(), mostRecentLoLLog.getPath());
						lastKnownPosition = lolLog.length() - 1;
						if (startAtBeginOfFile) {
							lastKnownPosition = 0;
						}
						lolLog = mostRecentLoLLog;
					}
				}
				long fileLength = lolLog.length();
				if (fileLength > lastKnownPosition) {
					RandomAccessFile readWriteFileAccess = new RandomAccessFile(lolLog, "rw");
					readWriteFileAccess.seek(lastKnownPosition);
					String line;
					while ((line = readWriteFileAccess.readLine()) != null) {
						if (line.contains(key)) {
							log.info(line);
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

	public static Pair<Boolean, Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>>> shouldUpdateStaticData(String currentLOLVersion) {
		boolean updateData = true;
		Pair<String, Map<Integer, Champion>> versionedIdChampionMap = null;
		Pair<String, Map<Integer, Integer>> versionedSkinIdMap = null;
		if (new File("versionedIdChampionMap.ser").isFile() && new File("versionedSkinIdMap.ser").isFile()) {
			versionedIdChampionMap = Files.deserializeData("versionedIdChampionMap.ser");
			versionedSkinIdMap = Files.deserializeData("versionedSkinIdMap.ser");
			if (versionedIdChampionMap.getLeft().equals(currentLOLVersion) && versionedSkinIdMap.getLeft().equals(currentLOLVersion)) {
				updateData = false;
			}
		}
		return Pair.of(updateData, Pair.of(versionedIdChampionMap, versionedSkinIdMap));
	}

}

package com.bhjelmar.util;

import lombok.extern.log4j.Log4j2;

import java.io.*;

@Log4j2
public class Files {

	public static void serializeData(Object o, String name) {
		try {
			FileOutputStream fos = new FileOutputStream(name);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(o);
			oos.close();
			fos.close();
			log.debug("Serialized data is saved in " + name + ".");
		} catch(IOException e) {
			log.error(e.getLocalizedMessage(), e);
		}
	}

	public static <T> T deserializeData(String fileName) {
		T obj;
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			obj = (T) ois.readObject();
			ois.close();
			fis.close();
		} catch(IOException e) {
			log.error(e.getLocalizedMessage(), e);
			return null;
		} catch(ClassNotFoundException e) {
			log.error(e.getLocalizedMessage(), e);
			return null;
		}
		log.debug("Deserialized " + fileName + " successfully.");
		return obj;
	}

}

package com.bhjelmar.imgcap;

import com.sun.jna.Native;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

@Data
@Log4j2
public class WindowScraper {

	int hWnd;
	WindowInfo w;
	Tesseract instance;

	public WindowScraper() throws IOException, URISyntaxException {
		instance = new Tesseract();
		log.info("getting resource");
		URL resource = getClass().getResource("/tessdata");

		File dataFolder = Paths.get(resource.toURI()).toFile();
		instance.setDatapath(dataFolder.getAbsolutePath());
		instance.setLanguage("eng");
	}

	public void findWindow(String windowTitle) {
		hWnd = IUser32.instance.FindWindowA(null, windowTitle);
		w = getWindowInfo(hWnd);
	}

	public static WindowInfo getWindowInfo(int hWnd) {
		RECT r = new RECT();
		IUser32.instance.GetWindowRect(hWnd, r);
		byte[] buffer = new byte[1024];
		IUser32.instance.GetWindowTextA(hWnd, buffer, buffer.length);
		String title = Native.toString(buffer);
		WindowInfo info = new WindowInfo(hWnd, r, title);
		return info;
	}

	public String update() {
		if(IUser32.instance.GetForegroundWindow() == w.hwnd) {
//			IUser32.instance.SetForegroundWindow(w.hwnd);
			BufferedImage image = null;
			try {
				image = new Robot().createScreenCapture(new Rectangle(w.rect.left, w.rect.top, w.rect.right - w.rect.left, w.rect.bottom - w.rect.top));
			} catch(AWTException e) {
				log.error(e.getStackTrace());
			}
			String result;
			if(captureLoLClient(image, true)) {
				result = getImgText(image);
			} else {
				//			System.out.println("not getting text");
				return "";
			}

			if(!result.startsWith("CHOOSE YOUR LOADOUT!")) {
				return "";
			}

			return result.replaceAll("\n", "").substring("CHOOSE YOUR LOADOUT!".length());
		}
		return "";
	}

	public String getImgText(BufferedImage image) {
		log.debug("Attempting to read champion text from screen");
		String result = null;
		try {
			result = instance.doOCR(image);
//			log.debug(result);
		} catch(TesseractException e) {
			e.printStackTrace();
		}
		return result;
	}

	private boolean captureLoLClient(BufferedImage image, boolean saveSnapshot) {
		@AllArgsConstructor
		class Rect {
//			a---
//			|  |
//			---b
int a_x;
			int a_y;
			int b_x;
			int b_y;

			public boolean pointLiesInRect(int x, int y) {
				if(x > a_x && x < b_x && y > a_y && y < b_y) {
					return true;
				}
				return false;
			}
		}
//				1600x900
//				1280x720
//				1024x576

		Rect chooseYourLoadoutRect = new Rect(560, 25, 1035, 55);
		Rect champSkinNameRect = new Rect(400, 568, 1200, 610);

		int allPixeBlack = 0;
		for(int w = 0; w < image.getWidth(); w++) {
			for(int h = 0; h < image.getHeight(); h++) {
				int p = image.getRGB(w, h);

				int blue = p & 0xFF;
				int green = (p & 0xFF00) >> 8;
				int red = (p & 0xFF0000) >> 16;
				double percentDeviation = .35;
//				"F0E6D2" is the color of the champ select text
//				riot in their infinite wisdom didn't make this text exactly the same color for each champ
				if(!chooseYourLoadoutRect.pointLiesInRect(w, h) && !champSkinNameRect.pointLiesInRect(w, h) ||
						!(((red ^ 0xF0) < (0xFF * percentDeviation)) && ((green ^ 0xE6) < (0xFF * percentDeviation)) && ((blue ^ 0xD2) < (0xFF * percentDeviation)))) {
					image.setRGB(w, h, 0x000000);
					allPixeBlack++;
				}
			}
		}

		if(saveSnapshot) {
			try {
				File outFile = new File("temp.png");
				ImageIO.write(image, "png", outFile);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		double percentagePixelsBlack = (allPixeBlack * 1.0 / (image.getWidth() * image.getHeight() * 1.0) * 100.0);
		return percentagePixelsBlack < 99.74; // magic number seems to work well
	}

}



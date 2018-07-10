package sample.imgcap;

import com.sun.jna.Native;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.log4j.Logger;
import sample.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class WindowScraper {

	private final Logger LOGGER = Logger.getLogger(this.getClass());

	int hWnd;
	WindowInfo w;
	Tesseract instance;

	public WindowScraper() {
		int hWnd = IUser32.instance.FindWindowA(null, "League of Legends");
//		hWnd = IUser32.instance.FindWindowA(null, "Photos");
		w = getWindowInfo(hWnd);

		instance = new Tesseract();

		URL resource = Main.class.getResource("/tessdata");
		File dataFolder = null;
		try {
			dataFolder = Paths.get(resource.toURI()).toFile();
		} catch(URISyntaxException e) {
			e.printStackTrace();
		}
		instance.setDatapath(dataFolder.getAbsolutePath());
		instance.setLanguage("eng");
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

	public void update() {
		IUser32.instance.SetForegroundWindow(w.hwnd);
		BufferedImage image = null;
		try {
			image = new Robot().createScreenCapture(new Rectangle(w.rect.left, w.rect.top, w.rect.right - w.rect.left, w.rect.bottom - w.rect.top));
		} catch(AWTException e) {
			e.printStackTrace();
		}
		captureLoLClient(image, true);
		String result = getImgText(image);
		System.out.println(result);
	}

	public String getImgText(BufferedImage image) {
		String result = null;
		try {
			result = instance.doOCR(image);
		} catch(TesseractException e) {
			System.err.println(e.getMessage());
		}
		return result;
	}

	private void captureLoLClient(BufferedImage image, boolean saveSnapshot) {
//		turn all pixels black except the text we care about

//		y: we want 25-60
//		x we want 560-1040

		for(int w = 0; w < image.getWidth(); w++) {
			for(int h = 0; h < image.getHeight(); h++) {
				int p = image.getRGB(w, h);

				int blue = p & 0xFF;
				int green = (p & 0xFF00) >> 8;
				int red = (p & 0xFF0000) >> 16;
				double percentDeviation = .15;
//				"F0E6D2" is the color of the champ select text
//				riot in their infinite wisdom didn't make this text exactly the same color for each champ
				if((h < 25 || h > 60) || (w < 560 || w < 1040)) {
					image.setRGB(w, h, 0xFF0000);
				} else if(!(((red ^ 0xF0) < (0xFF * percentDeviation)) && ((green ^ 0xE6) < (0xFF * percentDeviation)) && ((blue ^ 0xD2) < (0xFF * percentDeviation)))) { // for whiteish text
					image.setRGB(w, h, 0);
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
	}

}



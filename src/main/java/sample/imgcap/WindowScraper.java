package sample.imgcap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import com.sun.jna.Native;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import sample.Main;

public class WindowScraper {

	int hWnd;
	WindowInfo w;
	BufferedImage image;

	public WindowScraper() {
//		int hWnd = IUser32.instance.FindWindowA(null, "League of Legends");
		hWnd = IUser32.instance.FindWindowA(null, "Photos");
		w = getWindowInfo(hWnd);
	}

	public void update() throws AWTException, IOException, URISyntaxException {
		IUser32.instance.SetForegroundWindow(w.hwnd);
		image = new Robot().createScreenCapture(new Rectangle(w.rect.left, w.rect.top, w.rect.right - w.rect.left, w.rect.bottom - w.rect.top));

		File file = captureLoLClient();
		String result = getImgText(file);
		System.out.println(result);
	}

	public String getImgText(File outFile) throws URISyntaxException {
		Tesseract instance = new Tesseract();

		URL resource = Main.class.getResource("/tessdata");
		File dataFolder = Paths.get(resource.toURI()).toFile();

		//Set the tessdata path
		instance.setDatapath(dataFolder.getAbsolutePath());
		instance.setLanguage("eng");
		String result = null;
		try {
			result = instance.doOCR(outFile);
		} catch (TesseractException e) {
			System.err.println(e.getMessage());
		}
		return result;
	}

	private File captureLoLClient() {
//		turn all pixels black except the text we care about
		for(int w = 0; w < image.getWidth(); w++) {
			for(int h = 0; h < image.getHeight(); h++) {
				int p = image.getRGB(w, h);

				int blue = p & 0xff;
				int green = (p & 0xff00) >> 8;
				int red = (p & 0xff0000) >> 16;
//				"F0E6D2" is the color of the champ select text
//				riot in their infinite wisdom didn't make this text exactly the same color for each champ
//				this lets us be off by ~13 percent
//				if(!(((red ^ 0xF0) < 30) && ((green ^ 0xE6) < 30) && ((blue ^ 0xD2) < 30))) {
//					image.setRGB(w, h, 0);
//				}

				if(!(((red ^ 0xD8) < 120) && ((green ^ 0xA7) < 120) && ((blue ^ 0x0C) < 120))) {
					image.setRGB(w, h, 0);
				}
			}
		}
		File outFile = null;
		try{
			outFile = new File("temp.png");
			ImageIO.write(image, "png", outFile);
		} catch(IOException e){
			e.printStackTrace();
		}
		return outFile;
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

}



package sample;

import javafx.application.Application;
import javafx.stage.Stage;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

public class Main extends Application {

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd-MM-yyyy");

    public String getImgText(String imageLocation) throws URISyntaxException, IOException {

		URL resource = Main.class.getResource("/capture.png");
		File file = Paths.get(resource.toURI()).toFile();

		BufferedImage img = null;
		File f = null;

		//read image
		try{
			img = ImageIO.read(file);
		}catch(IOException e){
			System.out.println(e);
		}

		for(int w = 0; w < img.getWidth(); w++) {
			for(int h = 0; h < img.getHeight(); h++) {
				int p = img.getRGB(w, h);
				int blue = p & 0xff;
				int green = (p & 0xff00) >> 8;
				int red = (p & 0xff0000) >> 16;
//				"F0E6D2" is the color of the champ select text
				if(!(red == 240 && green == 230 && blue == 210)) {
					img.setRGB(w, h, 0);
				}
			}
		}
		File outFile = null;
		try{
			URL out = Main.class.getResource("/capture_cleaned.png");
			outFile = Paths.get(out.toURI()).toFile();
			ImageIO.write(img, "png", outFile);
		}catch(IOException e){
			System.out.println(e);
		}

        Tesseract instance = new Tesseract();

		resource = Main.class.getResource("/tessdata");
		File dataFolder = Paths.get(resource.toURI()).toFile();

        //Set the tessdata path
        instance.setDatapath(dataFolder.getAbsolutePath());
		instance.setLanguage("eng");
        String result = null;
        try {
            result = instance.doOCR(outFile);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
        return result;
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
//        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
//        primaryStage.setTitle("Hello World");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();


//		APIWrapper apiWrapper = new APIWrapper();
//		apiWrapper.getAllRunes();

        System.out.println(getImgText("/capture.png"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

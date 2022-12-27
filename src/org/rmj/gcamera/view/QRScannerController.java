package org.rmj.gcamera.view;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.gcamera.app.Utils;

public class QRScannerController implements Initializable { 
    @FXML
    private Button btnExit;
    @FXML
    private ImageView QRImage;
    
    private Image imageToShow;
    private BufferedImage imageToSave;
    private ScheduledExecutorService timer;
    private VideoCapture videoCapture;
    private String psFileName = "";  
    private PauseTransition delay;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnExit.setOnAction(this::cmdButton_Click); 
        //will close the stage when the PauseTransition was started
        delay = new PauseTransition(javafx.util.Duration.millis(5));
        delay.setOnFinished( event -> unloadScene());
        initOpenCv();
    }
    
    public String getQRValue(){
        return psFileName;
    }
    
    public void setFileName(String fsValue){
        psFileName = fsValue;
    }

    public static Image mat2Image(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
    
    private void cmdButton_Click(ActionEvent event) {
        unloadScene();
    }
    
    private void initOpenCv() {
        setLibraryPath();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        videoCapture = new VideoCapture();
        videoCapture.open(0);
//            VideoCapture videoCapture = new VideoCapture(0);

        if (this.videoCapture.isOpened()){
            // grab a frame every 33 ms (30 frames/sec)
            Runnable frameGrabber = new Runnable() {
                @Override
                public void run(){
                    // effectively grab and process a single frame
                    Mat frame = grabFrame();
                    
                    // convert and show the frame
                    imageToShow = Utils.mat2Image(frame);
                    imageToSave = Utils.matToBufferedImage(frame);
        
                    LuminanceSource source = new BufferedImageLuminanceSource(imageToSave);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    try {
                        Result result = new MultiFormatReader().decode(bitmap);
                        psFileName = result.getText();
                        closeCamera();
                        createTempFile();
                    } catch (NotFoundException e) {
                        psFileName = "";
                    }
                                        
                    updateImageView(QRImage, imageToShow);
                }
            };
            this.timer = Executors.newSingleThreadScheduledExecutor();
            this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
        } else{
            System.err.println("Impossible to open the camera connection...");
        }      
    }
    
    private void updateImageView(ImageView view, Image image){
        Utils.onFXThread(view.imageProperty(), image);
    }
    
    private Mat grabFrame(){
        // init everything
        Mat frame = new Mat();

        // check if the videoCapture is open
        if (this.videoCapture.isOpened()){
            try{
                // read the current frame
                this.videoCapture.read(frame);

                //if the frame is not empty, process it
                //if (!frame.empty()){
                //Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                //}
            } catch (Exception e){
                //log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }

        return frame;
    }
        
    private void createTempFile() {
        File tempFileDir = new File(System.getProperty("sys.default.path.config") + "/temp/" + "panalo.tmp");
        
        Path path = Paths.get(tempFileDir.getAbsolutePath());
        
        try{
            if (psFileName != null){
                //call api
                psFileName = APITrans.getUserPanalo(psFileName);
                
                Files.write(path, psFileName.getBytes());
            }
        }   catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private static void createQRImage(File qrFile, String psFileName, int size, String pxeDefaultExtImg) throws WriterException, IOException {
        // Create the ByteMatrix for the QR-Code that encodes the given String
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix byteMatrix = qrCodeWriter.encode(psFileName, BarcodeFormat.QR_CODE, size, size, hintMap);
        // Make the BufferedImage that are to hold the QRCode
        int matrixWidth = byteMatrix.getWidth();
        BufferedImage image = new BufferedImage(matrixWidth, matrixWidth, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, matrixWidth, matrixWidth);
        // Paint and save the image using the ByteMatrix
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < matrixWidth; i++) {
            for (int j = 0; j < matrixWidth; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        ImageIO.write(image, pxeDefaultExtImg, qrFile);
    }

    private void closeCamera(){
        if (this.timer!=null && !this.timer.isShutdown()){
            try{
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
                
                //this will unload the stage
                delay.play();
                
            } catch (InterruptedException e){
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.videoCapture.isOpened()){
            // release the camera
            this.videoCapture.release();
        }
    }
    
    private void unloadScene(){
        CommonUtils.closeStage(btnExit);
        if (this.videoCapture.isOpened()){
            closeCamera();
        }
    }
    
    private static void setLibraryPath() {
        try {
            int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
            if(bitness == 86){
                System.setProperty("java.library.path", System.getProperty("sys.default.path.config") + "/lib/opencv/x86");
            
                Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                fieldSysPath.setAccessible(true);
                fieldSysPath.set(null, null);
            } else if (bitness == 64){
                System.setProperty("java.library.path", System.getProperty("sys.default.path.config") + "/lib/opencv/x64");

                Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                fieldSysPath.setAccessible(true);
                fieldSysPath.set(null, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }catch (UnsatisfiedLinkError e) {
           System.err.println("Native code library failed to load.\n" + e);
           System.exit(1);
        }

    }

    public static String getOpenCvResource(Class<?> clazz, String path) {
        try {
            return Paths.get(clazz.getResource(path).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    } 
}


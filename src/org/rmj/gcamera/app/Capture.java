package org.rmj.gcamera.app;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.rmj.gcamera.view.QRScannerController;

/**
 *
 * @author Maynard
 */
public class Capture extends Application {
    private final static String pxeMainForm = "/org/rmj/gcamera/view/QRScanner.fxml";
    
    private double xOffset = 0; 
    private double yOffset = 0;
    
    @Override
    public void start(Stage stage) throws Exception {        
        FXMLLoader view = new FXMLLoader();
        view.setLocation(getClass().getResource(pxeMainForm));
        
        QRScannerController controller = new QRScannerController();
        
        view.setController(controller);        
        Parent parent = view.load();
        Scene scene = new Scene(parent);
        
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        parent.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
                public void handle(MouseEvent event) {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                }
            });
            parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset);
                }
            });

        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        
        System.setProperty("sys.default.path.config", path);
        
        System.setProperty("user.id", "M001111122");
        System.setProperty("user.client.id", "GGC_BM001");
        
        launch(args);
    }
}

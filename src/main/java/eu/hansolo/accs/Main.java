/*
 * Copyright (c) 2016 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.accs;

import eu.hansolo.accs.font.Fonts;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


/**
 * User: hansolo
 * Date: 22.06.16
 * Time: 13:03
 */
public class Main extends Application {
    private static final String               OPEN_STREET_MAP = "osm.html";
    private static final Dimension2D          SIZE            = new Dimension2D(800, 600);
    private static final DateTimeFormatter    DTF             = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter    TF              = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DecimalFormat        DF              = new DecimalFormat("");
    private static final DecimalFormatSymbols DFS             = new DecimalFormatSymbols(Locale.US);
    private volatile ScheduledFuture<?>       updateTask;
    private static   ScheduledExecutorService periodicUpdateExecutorService;
    private static   boolean                  readyToGo = false;
    private          ObservableList<Location> locationList;
    private          ListView<Location>       listView;
    private          WebView                  webView;
    private          WebEngine                webEngine;
    private          Region                   header;
    private          Text                     title;
    private          Region                   locationPanel;
    private          Text                     locationPanelTitle;


    // ******************** Initialization ************************************
    @Override public void init() {
        DFS.setDecimalSeparator('.');
        DFS.setGroupingSeparator(' ');
        DF.setDecimalFormatSymbols(DFS);

        locationList = FXCollections.observableArrayList();

        initGraphics();

        registerListeners();
        initTasks();
    }

    private void initGraphics() {
        // Header
        header = new Region();
        header.setPrefHeight(50);
        header.getStyleClass().add("overlay");

        AnchorPane.setTopAnchor(header, 0d);
        AnchorPane.setLeftAnchor(header, 0d);
        AnchorPane.setRightAnchor(header, 0d);


        // Title
        title = new Text("ACCS Desktop");
        title.setFont(Fonts.latoLight(24));
        title.setFill(Color.WHITE);

        AnchorPane.setTopAnchor(title, 10d);
        AnchorPane.setLeftAnchor(title, 10d);


        // LocationPanel
        locationPanel = new Region();
        locationPanel.setPrefWidth(270);
        locationPanel.getStyleClass().add("overlay");

        AnchorPane.setTopAnchor(locationPanel, header.getPrefHeight());
        AnchorPane.setRightAnchor(locationPanel, 0d);
        AnchorPane.setBottomAnchor(locationPanel, 0d);


        locationPanelTitle = new Text("Persons");
        locationPanelTitle.setFont(Fonts.latoLight(14));
        locationPanelTitle.setFill(Color.WHITE);

        AnchorPane.setTopAnchor(locationPanelTitle, header.getPrefHeight() + 20d);
        AnchorPane.setRightAnchor(locationPanelTitle, 213d);


        // LocationList
        listView = new ListView<>(locationList);
        listView.setPrefWidth(270);
        listView.setCellFactory(new Callback<ListView<Location>, ListCell<Location>>(){
            @Override public ListCell<Location> call(ListView<Location> p) {
                ListCell<Location> cell = new ListCell<Location>() {
                    @Override protected void updateItem(final Location LOCATION, final boolean IS_EMPTY) {
                        super.updateItem(LOCATION, IS_EMPTY);
                        if (LOCATION != null) {
                            String        dateTime;
                            Circle        circle        = new Circle(5);
                            LocalDateTime localDateTime = LocalDateTime.ofInstant(LOCATION.timestamp, ZoneId.systemDefault());
                            if (localDateTime.getDayOfYear() < LocalDateTime.now().getDayOfYear()) {
                                dateTime = DTF.format(localDateTime);
                                circle.getStyleClass().add("old-marker");
                                LOCATION.isUpToDate = false;
                            } else {
                                dateTime = TF.format(localDateTime);
                                circle.getStyleClass().add("active-marker");
                                LOCATION.isUpToDate = true;
                            }
                            String locationDateTime = LOCATION.info.isEmpty() ? dateTime : LOCATION.info + " - " + dateTime;

                            Label text = new Label(LOCATION.name);
                            text.setAlignment(Pos.CENTER_LEFT);
                            text.setMaxWidth(Double.MAX_VALUE);
                            text.getStyleClass().add("location-info");
                            HBox.setHgrow(text, Priority.ALWAYS);
                            HBox hbox = new HBox(5, circle, text);
                            hbox.setAlignment(Pos.CENTER);
                            Label lastUpdate = new Label(locationDateTime);
                            lastUpdate.setAlignment(Pos.CENTER_LEFT);
                            lastUpdate.getStyleClass().add("last-update");

                            VBox vBox = new VBox(hbox, lastUpdate);
                            setGraphic(vBox);
                            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        }
                    }
                };
                return cell;
            }
        });

        AnchorPane.setTopAnchor(listView, header.getPrefHeight() + 50d);
        AnchorPane.setRightAnchor(listView, 0d);
        AnchorPane.setBottomAnchor(listView, 90d);
    }

    private void registerListeners() {
        listView.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (null == nv) return;
            // Move map to selected location
            if (readyToGo) {
                StringBuilder scriptCommand = new StringBuilder();
                Platform.runLater(() -> {
                    scriptCommand.append("window.userName = \"").append(nv.name).append("\";")
                                 .append("document.panToMarker(window.userName);");
                    webEngine.executeScript(scriptCommand.toString());
                });
            }
        });
    }

    private void initTasks() {
        scheduleUpdateTask();
    }

    private void initOnFxApplicationThread() {
        webView = new WebView();
        webView.setPrefSize(530, 400);
        webView.setMinSize(530, 400);
        AnchorPane.setTopAnchor(webView, header.getPrefHeight());
        AnchorPane.setBottomAnchor(webView, 0d);
        webEngine = webView.getEngine();
        webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
            if (Worker.State.SUCCEEDED == n) {
                readyToGo = true;
                updateLocations();
            }
        });
        URL maps = Main.class.getResource(OPEN_STREET_MAP);
        webEngine.load(maps.toExternalForm());
    }


    // ******************** Methods *******************************************
    private void updateLocations() {
        locationList.clear();
        JSONArray locationArray = RestClient.INSTANCE.getLocations();
        for (Object obj : locationArray) {
            Location location = new Location(((JSONObject) obj));
            LocalDateTime localDateTime = LocalDateTime.ofInstant(location.timestamp, ZoneId.systemDefault());
            location.isUpToDate = (localDateTime.getDayOfYear() < LocalDateTime.now().getDayOfYear() || localDateTime.getYear() != LocalDateTime.now().getYear()) ? false : true;
            locationList.add(location);
            if (readyToGo) {
                StringBuilder scriptCommand = new StringBuilder();
                Platform.runLater(() -> {
                    scriptCommand.append("window.lat = ").append(location.latitude).append(";")
                                 .append("window.lon = ").append(location.longitude).append(";")
                                 .append("window.userName = \"").append(location.name).append("\";")
                                 .append("window.userInfo = \"").append(getLocationInfo(location)).append("\";")
                                 .append("window.upToDate = \"").append(location.isUpToDate).append("\";")
                                 .append("document.moveMarker(window.userName, window.userInfo, window.upToDate, window.lat, window.lon);");
                    webEngine.executeScript(scriptCommand.toString());
                });
            }
        }
    }

    private String getLocationInfo(final Location LOCATION) {
        StringBuilder userInfo = new StringBuilder("<table>");
        userInfo.append("<tr><td>Person  :</td><td>").append(LOCATION.name).append("</td></tr>")
                .append("<tr><td>Location:</td><td>").append(LOCATION.info).append("</td></tr>")
                .append("<tr><td>Time    :</td><td>").append(DTF.format(LocalDateTime.ofInstant(LOCATION.timestamp, ZoneId.systemDefault()))).append("</td></tr>");
        userInfo.append("</table>");
        return userInfo.toString();
    }


    // ******************** Scheduled task related ****************************
    private synchronized static void enableUpdateExecutorService() {
        if (null == periodicUpdateExecutorService) {
            periodicUpdateExecutorService = new ScheduledThreadPoolExecutor(1, getThreadFactory("UpdateTask", false));
        }
    }
    private synchronized void scheduleUpdateTask() {
        enableUpdateExecutorService();
        stopTask(updateTask);
        updateTask = periodicUpdateExecutorService.scheduleAtFixedRate(() ->
            Platform.runLater(() -> updateLocations())
        , 60, 30, TimeUnit.SECONDS);
    }

    private static ThreadFactory getThreadFactory(final String THREAD_NAME, final boolean IS_DAEMON) {
        return runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(IS_DAEMON);
            return thread;
        };
    }

    private void stopTask(ScheduledFuture<?> task) {
        if (null == task) return;

        task.cancel(true);
        task = null;
    }


    // ******************** Application related *******************************
    @Override public void start(final Stage STAGE) {
        initOnFxApplicationThread();

        AnchorPane pane = new AnchorPane();
        pane.getStyleClass().add("background");
        pane.getChildren().addAll(header, title, webView, locationPanel, locationPanelTitle, listView);

        Scene scene = new Scene(pane, SIZE.getWidth(), SIZE.getHeight());
        scene.getStylesheets().add(Main.class.getResource("styles.css").toExternalForm());

        STAGE.setTitle("ACCS Desktop");
        STAGE.setScene(scene);
        //STAGE.setMinWidth(780);
        //STAGE.setMinHeight(550);
        STAGE.setResizable(false);
        STAGE.show();
    }

    @Override public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

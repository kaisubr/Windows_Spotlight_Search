package sample;

import com.sun.javafx.application.PlatformImpl;
import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.Light;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import jdk.nashorn.internal.objects.Global;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.dispatcher.SwingDispatchService;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.plaf.PopupMenuUI;
import java.awt.*;
import java.awt.MenuItem;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main extends Application {

    public static final double WIDTH = 800;
    public static final double HEIGHT = 400;
    public static double X, Y;
    public static TrayIcon trayIcon;
    public static boolean isTray;
    private Stage mPrimaryStage;
    private Scene mScene;
    private boolean currentFocus;
    private static TextField textField;
    private static TextField labelResult;
    private static Label wikipediaResult;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mPrimaryStage = primaryStage;

        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        mPrimaryStage.initStyle(StageStyle.UTILITY);
        mPrimaryStage.setTitle(null);
        mScene = new Scene(root, 0, 0);
        mPrimaryStage.setScene(mScene);
        mPrimaryStage.setResizable(false);
        mPrimaryStage.show();

        windowToFocus(false);

        listenForOpenCall();

        addToTaskbar();

        textField = (TextField) getNodeById("textField_input");
        labelResult = (TextField) getNodeById("label_result");
        wikipediaResult = (Label) getNodeById("label_wikipedia_result");

        labelResult.setOnMouseClicked(e -> {
            String lastResult = labelResult.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(lastResult), null);
            textField.setText(lastResult);
            textField.requestFocus();
        });

        SpotlightHandler spotlightHandler = new SpotlightHandler(textField.getText());

        final boolean[] apiIsHolding = {false};

        //IF: Nothing found. Try refining your search query. For example, if you were looking up Windows, type in 'Windows' rather than asking the question, 'What is Windows?'
        mScene.setOnKeyPressed(e -> {
            if (apiIsHolding[0]) {
                System.out.println("There is a current request. Please hold.");
            }

            if (e.getCode() == KeyCode.ENTER && !apiIsHolding[0]) {
                //clear all
                labelResult.setText("Let me see what I can find..."); wikipediaResult.setText("");

                if (textField.getText().toLowerCase().equals("exit")) exitConf();

                //set input
                spotlightHandler.setmInput(textField.getText());

                final String[] wolframContentResult = {null};
                final String[] wikipediaContentResult = {null};
                final int[] timeout = {5000}; //timeout value, in milliseconds
                apiIsHolding[0] = true;

                new Thread(() -> {
                    System.out.println("Thread start");

                    try {
                        System.out.println("Getting WA response... ...");
                        Object[] WAresponse = spotlightHandler.getResponse(SpotlightHandler.SHORT_ANSWER_RESPONSE);
                        System.out.println("Check wikipedia?" + (boolean) WAresponse[1]);
                        wolframContentResult[0] = String.valueOf(WAresponse[0]);

                        if ((boolean) WAresponse[1]) {
                            Object[] WIKIresponse = spotlightHandler.getResponse(SpotlightHandler.WIKIPEDIA_RESPONSE);

                            //replace WA's answer if Wikipedia is supposed to trump
                            if ((boolean) WAresponse[2]) wolframContentResult[0] = (String.valueOf(WIKIresponse[0])).split(SpotlightHandler.SEPARATOR_KEY)[0];

                            System.out.println("Getting Wikipedia response... ...");

                            //Wikipedia content result
                            wikipediaContentResult[0] = (String.valueOf(WIKIresponse[0])).split(SpotlightHandler.SEPARATOR_KEY)[1];

                            apiIsHolding[0] = false;
                            this.stop();
                        }
                    } catch (Exception exception) {
                        System.out.println(exception.getMessage());
                        apiIsHolding[0] = false;
                        if (Main.isTray) Main.trayIcon.displayMessage(exception.getClass().getName(), exception.getStackTrace()[0].toString(), TrayIcon.MessageType.ERROR);

                    }
                }).start();

                final int[] cycles = {0};
                new java.util.Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //check, five times a second, if the api got an answer.
                        if (!apiIsHolding[0]) {
                            try {
                                if (wolframContentResult[0].equals(spotlightHandler.getmInput()) && (wikipediaContentResult[0].equals("ERROR_WIKIPEDIA") || wikipediaContentResult[0] == null)) {
                                    String[] randomOopsTitle = {
                                            "I'm not sure I understand.",
                                            "Sorry, I don't get that.",
                                            "That may be beyond my abilities at the moment.",
                                            "I'm sorry, I'm drawing a blank here.",
                                            "I don't understand what you mean.",
                                            "Sorry, I don't understand that.",
                                            "I'm sorry, but I don't know what you mean by that.",
                                            "That's an odd request. Can you try rephrasing it?"
                                    };

                                    String randomOopsHint[] = {
                                            "If you're looking for a term related to a specific field, try placing it in parentheses, like: Force (Physics)",
                                            "Try rephrasing your search.",
                                            "Your request might be too vague or too specific."
                                    };

                                    wolframContentResult[0] = randomOopsTitle[new Random().nextInt(randomOopsTitle.length)];
                                    wikipediaContentResult[0] = randomOopsHint[new Random().nextInt(randomOopsHint.length)];
                                } else if (wikipediaContentResult[0].equals("ERROR_WIKIPEDIA") || wikipediaContentResult[0] == null) {
                                    wikipediaContentResult[0] = "";
                                }
                            } catch (Exception exception) {
                                exception.printStackTrace();
                                if (Main.isTray) Main.trayIcon.displayMessage(exception.getClass().getName(), exception.getStackTrace()[0].toString(), TrayIcon.MessageType.ERROR);
                            }

                            System.out.println("[final] - " + wolframContentResult[0]);
                            System.out.println("[final] - " + wikipediaContentResult[0]);
                            wolframContentResult[0] = wolframContentResult[0].replaceAll("I was created by ", "This program uses WolframAlpha, which was created by ");

                            Platform.runLater(() -> {
                                labelResult.setText(wolframContentResult[0]);
                                wikipediaResult.setText(wikipediaContentResult[0]);
                            });

                            this.cancel();
                        } else {
                            //still waiting
                            cycles[0]++;
                            if (cycles[0] % 5 == 0) { //every second
                                Platform.runLater(() -> {
                                    labelResult.setText("Almost there");
                                    for (int i = 1; i <= cycles[0]/5; i++) {
                                        labelResult.appendText(".");
                                        if (i > 4) break;
                                    }
                                    wikipediaResult.setText("");
                                });
                            }
                        }
                    }
                }, 0, 200);


            }
        });

        java.util.Timer t = new java.util.Timer();

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final boolean[] changed = new boolean[1];

//        textField.textProperty().addListener((obs, oldVal, newVal) -> {
//            System.out.println(newVal);
//            changed[0] = true;
//
//            scheduler.schedule(() -> {
//                changed[0] = false;
//            }, 2000, TimeUnit.MILLISECONDS);
//
//            if (!changed[0]) {
//                spotlightHandler.setmInput(newVal);
//                final String[] res = {"No result found. Try refining your search."};
//
//                try {
//                    res[0] = spotlightHandler.getResponse(SpotlightHandler.SHORT_ANSWER_RESPONSE);
//                } catch (IOException e) {
//                    System.out.println(e.getMessage());
//                }
//
//                System.out.println(res[0]);
//
//                Platform.runLater(() -> {
//                    labelResult.setText(res[0]);
//                });
//            }
////            t.schedule(new TimerTask() {
////                @Override
////                public void run() {
////                    spotlightHandler.setmInput(newVal);
////                    final String[] res = {"No result found. Try refining your search."};
////                    try {
////                        res[0] = spotlightHandler.getResponse(SpotlightHandler.SHORT_ANSWER_RESPONSE);
////                    } catch (IOException e) {
////                        System.out.println(e.getMessage());
////                    }
////
////                    System.out.println(res[0]);
////
////                    Platform.runLater(() -> {
////                        labelResult.setText(res[0]);
////                    });
////
////                }
////            }, 2000); //two seconds
//
//        });
    }

    private Fader f;
    /**
     * Uses input by t to fade in out.
     * @param start start/stop value
     * @param t FadeTransition value
     */
    private void fadeInOut(boolean start, FadeTransition t) {
        //animation
        if (start) {
            f = new Fader(t);
            f.start();
        } else {
            f.sendFlag(Fader.STOP);
            f.stop(); //deprecated, used flag (above) instead
        }
    }

    private Node getNodeById(String id) {
        return mScene.lookup("#" + id);
    }

    private void addToTaskbar() {
        if (!SystemTray.isSupported()) {
            System.out.println("[i] System tray not supported!");
            return;
        }

        PopupMenu popupMenu = new PopupMenu();
//        TrayIcon trayIcon = new TrayIcon(new ImageIcon(
//                "src/images/sp-search/ic_search_white_24dp/ic_search_white_24dp/android/drawable-xxhdpi/ic_search_white_24dp.png").getImage());
        trayIcon = new TrayIcon(new ImageIcon(
               "src/images/sp-search/Search.png", "Windows Spotlight Search").getImage());
        SystemTray systemTray = SystemTray.getSystemTray();
        //popup menu
        Font newFont = new Font("Arial", Font.PLAIN, 27);

        MenuItem refreshItem = new MenuItem("Bring to front"); refreshItem.setFont(newFont);
        MenuItem restartItem = new MenuItem("Restart app"); restartItem.setFont(newFont);
        MenuItem exitItem = new MenuItem("Exit"); exitItem.setFont(newFont.deriveFont(Font.BOLD));

        popupMenu.add(restartItem);
        popupMenu.add(refreshItem);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);

        trayIcon.setPopupMenu(popupMenu);

        try {
            systemTray.add(trayIcon);
            trayIcon.displayMessage("Spotlight Search is activated.", "Press F7 to launch Spotlight Search.", TrayIcon.MessageType.INFO);
            isTray = true;
        } catch (AWTException e) {
            isTray = false;
            System.out.println("TrayIcon could not be added.");
        }

        refreshItem.addActionListener((l) -> {
            refresh();
        });

        exitItem.addActionListener((l) -> {
            Platform.runLater(() -> {
                exitConf();
            });

        });

        restartItem.addActionListener(l -> {
            //unimplemented
        });


    }

    private void exitConf() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Spotlight Search");
        alert.setHeaderText("Exit?");
        alert.setContentText("Are you sure you want to exit Spotlight Search?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.get() == ButtonType.OK){
            System.exit(0);
        }
    }

    private int[] cycle_X_key_history = {1, 0, 0};

    private void listenForOpenCall() {
        NativeKeyListener nativeKeyListener = new NativeKeyListener() {
            @Override
            public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
                /* unimplemented */
            }

            @Override
            public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
                if (cycle_X_key_history[0] == 2) cycle_X_key_history[0] = 1;
                cycle_X_key_history[cycle_X_key_history[0]] = nativeKeyEvent.getKeyCode();
                cycle_X_key_history[0]++;

                //System.out.println("[i] Pressed " + nativeKeyEvent.getKeyCode() + ", " + NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode()));
                if (cycle_X_key_history[1] == 65 || cycle_X_key_history[2] == 65 //||        //F7
                        //() other stuff if you want, don't try two combinations plz
                        ) {
                    System.out.println("caught!");
                    windowToFocus(!currentFocus);
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
                /* unimplemented */
            }
        };

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.setEventDispatcher(new SwingDispatchService());
        } catch (NativeHookException e) {
            //System not supported
            e.printStackTrace();
        }

        GlobalScreen.addNativeKeyListener(nativeKeyListener);

        // Clear previous logging configurations.
        LogManager.getLogManager().reset();

        // Get the logger for "org.jnativehook" and set the level to off.
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

//        mScene.setOnKeyPressed(e -> {
//            if (e.isControlDown() && e.getCode() == KeyCode.SPACE) {
//                System.out.println("Ctrl + Space!");
//                //bring window to focus
//                windowToFocus(true);
//            }
//        });

        mPrimaryStage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(oldValue + " to " + newValue);
            if (!newValue) {
                //changed to out of view
                mPrimaryStage.setIconified(true);
                currentFocus = false;
                System.out.println("Already clicked out!");
                //windowToFocus(false);     does not work because the listener is called AFTER the window closes AUTOMATICALLY by the OS, so the opacity DOES happen, but the window is already in the background so we can't see it.
            } else {
                //changed to in view
                mPrimaryStage.setIconified(false);
                currentFocus = true;
            }
        });

        mPrimaryStage.setOnCloseRequest(ev -> {
            ev.consume();
            windowToFocus(false);
            System.out.println("event to close was consumed");
        });
    }

    private void refresh(){
        System.out.println("[i] REFRESHING STAGE...");
        windowToFocus(false); //reensure false
        windowToFocus(true);
        //clear text, etc
        System.out.println("[i] Done. ");
    }

    boolean isCurrentlyClosing = false;

    private void windowToFocus(boolean isFocus) {
        System.out.println(isFocus);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        System.out.println("Currently closing, " + isCurrentlyClosing);

        //requires to be fully closed.
        if (isCurrentlyClosing) return;

        if (isFocus) {
            //recheck before shown
            Platform.runLater(() -> {
                mPrimaryStage.setOpacity(1.0);
            });

            mPrimaryStage.setWidth(WIDTH);
            mPrimaryStage.setHeight(HEIGHT);

            X = (screen.getWidth() - WIDTH)/2; Y = (screen.getHeight() - HEIGHT)/2;
            mPrimaryStage.setX(X); mPrimaryStage.setY(Y);

            Platform.runLater(() -> {
                mPrimaryStage.setIconified(false);
//                mPrimaryStage.requestFocus();
                mPrimaryStage.show();
            });
        } else {
            isCurrentlyClosing = true;

            double[] opacity = {1};
            while (opacity[0] > 0.05) {
                if (isCurrentlyClosing) {
                    Platform.runLater(() -> {
                        mPrimaryStage.setOpacity(opacity[0]);
                    });

                    System.out.println("Currently closing, " + isCurrentlyClosing + " at opacity " + opacity[0]);
                    opacity[0] -= 0.05;

                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            isCurrentlyClosing = false;

            Platform.runLater(() -> {
                mPrimaryStage.setIconified(true);
            });

            //set after hidden
            mPrimaryStage.setWidth(WIDTH);
            mPrimaryStage.setHeight(HEIGHT);
            //mPrimaryStage.setX(screen.getWidth()); mPrimaryStage.setY(screen.getHeight());
        }

    }


    public static void main(String[] args) {
        launch(args);
    }
}

package sample;

import javafx.animation.FadeTransition;

/**
 * Created by Kailash Sub for Windows_Spotlight_Search.
 */
public class Fader extends Thread {
    FadeTransition t;
    public static final boolean STOP = false;
    boolean flag = true;

    public Fader(FadeTransition t) {
        this.t = t;
    }

    public void run() {
        while (flag) {
//            System.out.println("Running.");
//            t.setFromValue(1.0);
//            t.setToValue(0.1);
////            t.setCycleCount(FadeTransition.INDEFINITE);
//            t.setAutoReverse(true);
        }
        System.out.println("Stopped!");
        t.stop();
    }

    public void sendFlag(boolean flag) {
        System.out.println("Flag set, applying changes...");
        this.flag = flag;
    }


}

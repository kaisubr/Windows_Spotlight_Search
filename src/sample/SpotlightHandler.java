package sample;

import org.apache.commons.lang3.StringEscapeUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Kailash Sub for Windows_Spotlight_Search.
 */
public class SpotlightHandler {

    private String mInput;
    private final static String APP_ID = "Q75KY3-JTQEA52KGQ";
    public final static int SHORT_ANSWER_RESPONSE = 0;
    public final static int FULL_RESULT_RESPONSE = 1;
    public static final int WIKIPEDIA_RESPONSE = 2;
    public static final String SEPARATOR_KEY = " _ _SEP_ _ ";

    public SpotlightHandler(String input) {
        mInput = input;
    }

    public static boolean wolframAlphaIsDown = false;
    /**
     *
     * @param type
     * @return object array with {String: WolframAlpha result, boolean: try Wikipedia?, boolean: doesWikipediaTrump?}
     * @throws IOException
     */
    public Object[] getResponse(int type) throws IOException {
        if (type == SHORT_ANSWER_RESPONSE) {
            URL outUrl = new URL("https://api.wolframalpha.com/v1/result?i=" + java.net.URLEncoder.encode(getmInput(), "UTF-8") + "&appid=" + APP_ID);
            HttpURLConnection con = (HttpURLConnection) outUrl.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            //send request
            int responseCode = -1;

            try {
                responseCode = con.getResponseCode();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            System.out.println("Request from " + outUrl + " responded with " + responseCode);

            if (responseCode == -1) {
                wolframAlphaIsDown = true;
                return new Object[]{"No internet connection, or WolframAlpha is down. Trying Wikipedia.", true, false};
            }

            wolframAlphaIsDown = false;

            if (responseCode != HttpURLConnection.HTTP_OK) {
                //not supported, try Wikipedia
                return new Object[]{"(Wikipedia trumps so this won't be shown)", true, true};
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine; StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if (response.toString().equals("Wolfram|Alpha did not understand your input")) {
                //WA didn't understand input. Try Wikipedia.
                System.out.println("[WARNING] Response code was " + responseCode + " but response is 'unable to understand input'.");
                if (Main.isTray) Main.trayIcon.displayMessage("Spotlight Search encountered an unknown error.", "Your request could not be queried.", TrayIcon.MessageType.ERROR);
                return new Object[]{"(Wikipedia trumps so this won't be shown)", true, true};
            }

            System.out.println("equalto? " + response.toString().toLowerCase().equals(getmInput().toLowerCase()));
            if (response.toString().toLowerCase().equals(getmInput().toLowerCase())) {
                //response same as input
                return new Object[]{StringEscapeUtils.unescapeJava(response.toString()), true, true};
            }

            //normal response
            return new Object[]{StringEscapeUtils.unescapeJava(response.toString()), true, false};

        } else if (type == FULL_RESULT_RESPONSE) {
            //not supported yet
        } else if (type == WIKIPEDIA_RESPONSE) {
            //mInput = mInput.toLowerCase(); causes problems with actors like "idina menzel"
            URL outUrl = new URL("https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" +
                    java.net.URLEncoder.encode(getmInput(), "UTF-8") + "&redirects=1");

            HttpURLConnection con = (HttpURLConnection) outUrl.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            //send request
            int responseCode = -1;
            try {
                responseCode = con.getResponseCode();
            } catch (Exception e) {
                if (wolframAlphaIsDown) if (Main.isTray) Main.trayIcon.displayMessage("HTTP request failed with code " + responseCode, "Please check your internet connection.", TrayIcon.MessageType.ERROR);
                if (!wolframAlphaIsDown) if (Main.isTray) Main.trayIcon.displayMessage("HTTP request failed with code " + responseCode, "It seems that Wikipedia is down at the moment.", TrayIcon.MessageType.ERROR);
            }

            System.out.println("Request from " + outUrl + " responded with " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine; StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String responseTitle, responseContent;
            responseTitle = response.toString().split("\"title\":\"")[1].split("\"")[0];

            System.out.println("refer to: is " + response.toString().indexOf("refer to:"));
            if (response.toString().indexOf("refer to:") == -1) {
                //more than 5 words
                try {
                    //responseContent = response.toString().split("\"extract\":\"")[1].split("\"}")[0].replaceAll("\\<[^>]*>","").replaceAll("\\\\n", " ");//remove all html content
                    CharSequence responseCS = response.toString().split("\"extract\":\"")[1].split("\"}")[0];

                    //if it has a new line character, cut at the new line.
                    int newLineChar = (responseCS.toString()).indexOf("\\n");
                    System.out.println("\\n is " + newLineChar);
                    if (newLineChar != -1) {
                        System.out.println(responseCS.charAt(newLineChar));
                        responseCS = responseCS.subSequence(0, newLineChar);
                    }

                    //System.out.println("escaping html, found: " + Html.escapeHtml(responseCS));
                    responseContent = Html.escapeHtml(responseCS);
                    System.out.println(responseTitle + ": " + responseContent);
                    return new Object[]{(StringEscapeUtils.unescapeJava(responseTitle)) + SEPARATOR_KEY + (StringEscapeUtils.unescapeJava(responseContent)), false, null};
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

            System.out.println("Nothing found in Wikipedia! Try refining search query");
            return new Object[]{responseTitle + SEPARATOR_KEY + "ERROR_WIKIPEDIA", false, null};

        }

        //nothing?
        if (Main.isTray) Main.trayIcon.displayMessage("Spotlight Search encountered an unknown error.", "Your request could not be queried.", TrayIcon.MessageType.ERROR);
        return new Object[]{"Something went wrong in SpotlightHandler#getResponse(...)", null, null};
    }

    public String getmInput() {
        return mInput;
    }

    public void setmInput(String input) {
        mInput = input;
    }
}

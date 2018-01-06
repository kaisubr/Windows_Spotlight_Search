package sample;

/**
 * Note: source code extracted from android.text.Html and edited, to suit current needs, by me. All edits are cited with
 *      // EDIT
 * at the end of the line.
 */
public class Html {
    //Line 272
    /**
     * Returns an HTML escaped representation of the given plain text.
     */
    public static String escapeHtml(CharSequence text) {
        StringBuilder out = new StringBuilder();
        withinStyle(out, text, 0, text.length());

        return out.toString();
    }

    //Line 643
    private static void withinStyle(StringBuilder out, CharSequence text,
                                    int start, int end) {

        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append(" ");        // EDIT
            } else if (c == '>') {
                out.append(" ");        // EDIT
            } else if (c == '&') {
                out.append(" ");
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                //if (c < 0xDC00 && i + 1 < end) {
                    //char d = text.charAt(i + 1);
                    //if (d >= 0xDC00 && d <= 0xDFFF) {
                        //i++;
                        //int codepoint = 0x010000 | (int) c - 0xD800 << 10 | (int) d - 0xDC00;
                        out.append(" "); //.append(codepoint).append(";");                      // EDIT lines 31 - 39
                    //}
                //}
            } else if (c > 0x7E || c < ' ') {
                out.append(" "); //.append((int) c).append(";"); // EDIT
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append(" "); // EDIT
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }
}

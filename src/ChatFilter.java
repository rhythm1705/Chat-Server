import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Chat Filter class
 *
 * Class of Chat Filter
 *
 * @author Sadiq and Rhythm Goel
 *
 * @version Nov 25, 2018
 *
 */

public class ChatFilter {

    private ArrayList<String> bw = new ArrayList<>();

    public ChatFilter(String badWordsFileName) {
        File f = new File(badWordsFileName);
        
        try {
            BufferedReader bfr = new BufferedReader(new FileReader(f));
            
            while (true) {
                String s = bfr.readLine();
                if (s == null) {
                    break;
                }
                bw.add(s);
            }
            
            bfr.close();
            
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        
    }

    public String filter(String msg) {
        String replaced = msg;
        
        for (int i = 0; i < bw.size(); i++) {
            if (msg.toLowerCase().contains(bw.get(i).toLowerCase())) {
                String replacement = "";
                
                for (int j = 0; j < bw.get(i).length(); j++) {
                    replacement += "*";
                }
                
                replaced = replaced.replaceAll("(?i)" + Pattern.quote(bw.get(i)), replacement);
            }
        }
        
        return replaced;
    }
}

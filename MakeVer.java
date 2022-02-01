
import java.text.DateFormat;
import java.util.Date;
import java.io.FileOutputStream;

public class MakeVer {
    static String compileDateTime =DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date());
        public static void main(String[] args) {
            String ubluVersion = args[0];
            String targetFile = args[1];
            System.out.println("Setting Ublu version to " + ubluVersion);
            //TODO: validate args
            String s = String.format("package ublu;\n" + 
            "class Version {\n"+
            "    static String compileDateTime =\"%s\";\n"+
            "    static String ubluVersion = \"%s\";\n"+
            "}\n", compileDateTime, ubluVersion);
            try(FileOutputStream out = new FileOutputStream(targetFile)) {
                out.write(s.getBytes("UTF-8"));
            } catch(Exception e) {
                System.err.println("Error writing to " + targetFile);
                e.printStackTrace();
            }
        }
}
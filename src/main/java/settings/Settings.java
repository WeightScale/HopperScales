package settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Settings {
    public static Map<String, String> properties = new HashMap<String, String>();
    public static List<Integer> nodes = new ArrayList<>();

    public Settings(String name) throws Exception {
        File file = new File(name);
        InputStream inputStream = null;
        StringBuilder sb = new StringBuilder();
        inputStream = new FileInputStream(file);
        for (int ch; (ch = inputStream.read()) != -1; ) {
            sb.append((char) ch);
        }

        String config = sb.toString();
        String[] pairs = config.split("\r\n");
        for (int i=0;i<pairs.length;i++) {
            String pair = pairs[i];
            String[] keyValue = pair.split(":");
            properties.put(keyValue[0], keyValue[1]);
        }
        if(properties.containsKey("hosts")){
            String str = properties.get("hosts");
            nodes = Stream.of(str.replace(" ", "").split(",")).map(Integer::valueOf).collect(Collectors.toList());
        }
    }
}

package org.cneko.toneko.common.util;

import com.google.gson.Gson;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class ConfigBuilder {
    private final Path path;
    private YamlC config;
    private final Map<String,Entry> defaults = new LinkedHashMap<>();
    public ConfigBuilder(Path path){
        this.path = path;
        // 尝试读取文件
        try {
            config = new YamlC(path);
        } catch (Exception e) {
            // 出现错误，创建一个空的配置文件
            config = new YamlC("");
        }
    }

    private ConfigBuilder add(String key, Entry defaultValue, String comment) {
        defaults.put(key, defaultValue);
        return this;
    }

    private ConfigBuilder add(String key, Entry defaultValue, String... comments) {
        String combinedComment = String.join("\n", comments);
        return this.add(key, defaultValue, combinedComment);
    }

    public ConfigBuilder addString(String key, String value,String url, String comment) {
        return this.add(key, Entry.of(value, comment,url), comment);
    }

    public ConfigBuilder addString(String key, String value,String url, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments),url), comments);
    }

    public ConfigBuilder addBoolean(String key, Boolean value,String url, String comment) {
        return this.add(key, Entry.of(value, comment,url), comment);
    }

    public ConfigBuilder addBoolean(String key, Boolean value,String url, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments),url), comments);
    }
    public void setBoolean(String key, boolean value) {
        config.set(key, value);
        try {
            config.save(path.toFile());
        } catch (IOException ignored) {
        }
    }
    public void setString(String key, String value) {
        config.set(key, value);
        try {
            config.save(path.toFile());
        } catch (IOException ignored) {
        }
    }
    public Entry get(String key){
        return defaults.get(key);
    }
    public Entry getExist(String key){
        return Entry.of(config.get(key), get(key).comment, config.getString("url"));
    }
    public String getKey(Entry entry){
        for (String key : defaults.keySet()) {
            if (defaults.get(key).equals(entry)) {
                return key;
            }
        }
        return null;
    }

    public List<String> getKeys() {
        return new ArrayList<>(defaults.keySet());
    }



    public ConfigBuilder build() {
        for (String key : defaults.keySet()) {
            Entry entry = defaults.get(key);
            if (!config.contains(key)) {
                //config.addComment(key, entry.comment);
                config.set(key, entry.get());
            }
        }
        try {
            Path configPath = Path.of("config/");
            if (!Files.exists(configPath)){
                Files.createDirectories(configPath);
            }
            config.save(path.toFile());
        } catch (IOException e) {
            LOGGER.error("Unable to save config file", e);
        }
        return this;
    }



    public YamlC createConfig(){
        try {
            return new YamlC(path);
        } catch (IOException e) {
            return config;
        }
    }



    public static ConfigBuilder create(Path path){
        return new ConfigBuilder(path);
    }

    public static class Entry{
        private final Object value;
        private final Types type;
        private final String comment;
        private String url;
        public Entry(Object value, Types type,String comment){
            this.value = value;
            this.type = type;
            this.comment = comment;
        }
        public Entry(Object value, Types type,String comment,@Nullable String url){
            this(value,type,comment);
            this.url = url;
        }
        public Object get(){
            return value;
        }
        public Types type(){
            return type;
        }
        public String comment(){
            return comment;
        }
        public Entry setUrl(String url){
            this.url = url;
            return this;
        }
        public String url(){
            return url;
        }
        public String string(){
            return (String) value;
        }
        public Number number(){
            return (Number) value;
        }
        public boolean bool(){
            return (boolean) value;
        }
        public ConfigBuilder config(){
            return (ConfigBuilder) value;
        }

        public static Entry of(Object value,String comment,String url){
            if (value instanceof String){
                return new Entry(value,Types.STRING,comment,url);
            }else if (value instanceof Number){
                return new Entry(value,Types.NUMBER,comment,url);
            }else if (value instanceof Boolean){
                return new Entry(value,Types.BOOLEAN,comment,url);
            }else if (value instanceof List<?>){
                return new Entry(value,Types.LIST,comment,url);
            }else if (value instanceof ConfigBuilder){
                return new Entry(value,Types.CONFIG,comment,url);
            }
            throw new IllegalArgumentException("Invalid type: " + value.getClass().getName());
        }

        public enum Types{
            STRING,
            NUMBER,
            BOOLEAN,
            LIST,
            CONFIG
        }
    }

    public static class YamlC{

        private final Map<String, Object> data;
        private final Path path;

        public YamlC(Path path) throws IOException {
            this.path = path;
            if (Files.exists(path)) {
                InputStream in = Files.newInputStream(path);

                try {
                    // 使用 UTF-8 编码读取
                    InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                    Yaml yaml = new Yaml();
                    this.data = yaml.load(reader);
                } catch (Throwable var6) {
                    try {
                        in.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }

                    throw var6;
                }

                in.close();
            } else {
                this.data = new LinkedHashMap<>();
            }
        }

        public YamlC(File file) throws IOException {
            this(file.toPath());
        }

        public YamlC(String yamlContent) {
            Yaml yaml = new Yaml();
            this.data = yaml.load(yamlContent);
            this.path = null;
        }

        public Object get(String path) {
            String[] keys = path.split("\\.");
            Map<String, Object> current = this.data;

            for(int i = 0; i < keys.length - 1; ++i) {
                current = (Map)current.get(keys[i]);
                if (current == null) {
                    return null;
                }
            }

            return current.get(keys[keys.length - 1]);
        }

        public void set(String path, Object value) {
            String[] keys = path.split("\\.");
            Map<String, Object> current = this.data;

            for(int i = 0; i < keys.length - 1; ++i) {
                current = (Map)current.computeIfAbsent(keys[i], (k) -> {
                    return new LinkedHashMap();
                });
            }

            current.put(keys[keys.length - 1], value);

            try {
                this.save();
            } catch (IOException var6) {
                System.out.println(var6.getMessage());
            }

        }

        public void save() throws IOException {
            if (this.path != null) {
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(options);
                OutputStream out = Files.newOutputStream(this.path);

                try {
                    yaml.dump(this.data, new OutputStreamWriter(out, StandardCharsets.UTF_8));
                } catch (Throwable var7) {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (out != null) {
                    out.close();
                }

            }
        }

        public void save(Path targetPath) throws IOException {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            OutputStream out = Files.newOutputStream(targetPath);

            try {
                yaml.dump(this.data, new OutputStreamWriter(out,StandardCharsets.UTF_8));
            } catch (Throwable var8) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                    }
                }

                throw var8;
            }

            if (out != null) {
                out.close();
            }

        }

        public void save(File targetFile) throws IOException {
            this.save(targetFile.toPath());
        }

        public String getString(String path) {
            return (String)this.get(path);
        }

        public List<String> getStringList(String path) {
            return (List)this.get(path);
        }

        public float getFloat(String path) {
            Object value = this.get(path);
            return value instanceof Float ? (Float)value : 0.0F;
        }

        public double getDouble(String path) {
            Object value = this.get(path);
            return value instanceof Double ? (Double)value : 0.0;
        }

        public int getInt(String path) {
            Object value = this.get(path);
            return value instanceof Integer ? (Integer)value : 0;
        }

        public boolean getBoolean(String path) {
            Object value = this.get(path);
            return value instanceof Boolean && (Boolean)value;
        }

        public boolean getBoolean(String path, boolean defValue) {
            Object value = this.get(path);
            return value instanceof Boolean ? (Boolean)value : defValue;
        }

        public boolean isSet(String path) {
            return this.get(path) != null;
        }

        public boolean contains(String path) {
            return this.isSet(path);
        }

        public ArrayList<Integer> getIntList(String path) {
            Object value = this.get(path);
            if (value instanceof List) {
                ArrayList<Integer> list = new ArrayList();
                Iterator var4 = ((List)value).iterator();

                while(var4.hasNext()) {
                    Object obj = var4.next();
                    if (obj instanceof Integer) {
                        list.add((Integer)obj);
                    }
                }

                return list;
            } else {
                return new ArrayList();
            }
        }

        public ArrayList<Double> getDoubleList(String path) {
            Object value = this.get(path);
            if (value instanceof List) {
                ArrayList<Double> list = new ArrayList();
                Iterator var4 = ((List)value).iterator();

                while(var4.hasNext()) {
                    Object obj = var4.next();
                    if (obj instanceof Double) {
                        list.add((Double)obj);
                    }
                }

                return list;
            } else {
                return new ArrayList();
            }
        }

        public ArrayList<Float> getFloatList(String path) {
            Object value = this.get(path);
            if (value instanceof List) {
                ArrayList<Float> list = new ArrayList();
                Iterator var4 = ((List)value).iterator();

                while(var4.hasNext()) {
                    Object obj = var4.next();
                    if (obj instanceof Float) {
                        list.add((Float)obj);
                    }
                }

                return list;
            } else {
                return new ArrayList();
            }
        }

        public ArrayList<Object> getList(String path) {
            Object value = this.get(path);
            return value instanceof List ? new ArrayList((List)value) : new ArrayList();
        }

        public String toString() {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            return yaml.dump(this.data);
        }

        public boolean equals(Object obj) {
            return obj.toString().equals(this.toString());
        }

        public boolean equalsCaseIgnoreCase(Object obj) {
            return obj.toString() != null ? obj.toString().equalsIgnoreCase(this.toString()) : false;
        }

        public static JsonConfiguration toJson(String yamlContent) {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = (Map)yaml.load(yamlContent);
            Gson gson = new Gson();
            return JsonConfiguration.of(gson.toJson(yamlMap));
        }

    }

    public static class YC {
        private Map<String, Object> data;
        private DumperOptions options;
        private Yaml yaml;


        public YC() {
            data = new LinkedHashMap<>(); // 保持顺序
            options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // 使用块样式，更易读
            options.setWidth(200); // 防止过早换行
            yaml = new Yaml(options);

        }

        public YC(String yamlString) {
            this();
            loadFromString(yamlString);
        }

        public YC(File yamlFile) throws IOException {
            this();
            loadFromFile(yamlFile);
        }

        public YC(Path filePath) throws IOException{
            this(filePath.toFile());
        }

        public void loadFromString(String yamlString) {
            data = yaml.load(yamlString);
            if (data == null){
                data = new LinkedHashMap<>();
            }
        }

        public void loadFromFile(File yamlFile) throws IOException {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(yamlFile))) {
                data = yaml.load(reader);
            }
        }


        public void set(String key, Object value) {
            // 支持嵌套key，例如 "a.b.c"
            String[] keys = key.split("\\.");
            Map<String, Object> current = data;
            for (int i = 0; i < keys.length - 1; i++) {
                if (!current.containsKey(keys[i]) || !(current.get(keys[i]) instanceof Map)) {
                    current.put(keys[i], new LinkedHashMap<>());
                }
                current = (Map<String, Object>) current.get(keys[i]);
            }
            current.put(keys[keys.length - 1], value);
        }

        public String getString(String key) {
            return getString(key, null);
        }

        public String getString(String key, String defaultValue) {
            Object value = get(key);
            return value != null ? value.toString() : defaultValue;
        }


        public Boolean getBoolean(String key) {
            return getBoolean(key, null);
        }

        public Boolean getBoolean(String key, Boolean defaultValue) {
            Object value = get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value); // 尝试将字符串解析为布尔值
            }
            return defaultValue;
        }



        private Object get(String key) {
            String[] keys = key.split("\\.");
            Map<String, Object> current = data;
            for (int i = 0; i < keys.length - 1; i++) {
                if (!current.containsKey(keys[i]) || !(current.get(keys[i]) instanceof Map)) {
                    return null;
                }
                current = (Map<String, Object>) current.get(keys[i]);
            }
            return current.get(keys[keys.length - 1]);
        }

        public boolean contains(String key) {
            return data.containsKey(key);
        }

        public boolean containsNestedKey(String key) {
            String[] keys = key.split("\\.");
            Map<String, Object> current = data;
            for (String k : keys) {
                if (!current.containsKey(k)) {
                    return false;
                }
                Object value = current.get(k);
                if (value instanceof Map) {
                    current = (Map<String, Object>) value;
                } else if (!(value instanceof Map) && keys[keys.length -1] != k) {
                    return false;
                }


            }
            return true;
        }

        public void addComment(String key, String... commentLines) {
            // SnakeYaml 不直接支持在特定 key 前添加注释.  变通方法: 使用 Tag.COMMENT
            StringBuilder comment = new StringBuilder();
            for (String line : commentLines) {
                comment.append("# ").append(line).append("\n");
            }
            set(key + ".comment", new Tag("!comment"), comment.toString()); // 添加虚拟 key 用于存储注释
        }


        private void set(String key, Tag tag, Object value) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(tag.getValue(), value);
            set(key,map);

        }

        @Override
        public String toString() {
            return yaml.dump(data);
        }



        public void saveToFile(File yamlFile) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(yamlFile))) {
                dump(data, writer, 0);  // 使用递归方法处理嵌套结构
            }
        }


        private void dump(Object value, BufferedWriter writer, int indent) throws IOException {
            if (value instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    String key = entry.getKey().toString();
                    Object val = entry.getValue();

                    // 先处理注释
                    if (key.endsWith(".comment")) {
                        writeComment(writer, (Map<String, String>)val, indent);

                    } else {
                        writeIndent(writer, indent);
                        writer.write(key + ": ");


                        if (val instanceof Map && ((Map<?, ?>) val).containsKey("!comment")) {
                            writeComment(writer, (Map<String, String>) val, indent);

                            // 直接获取非注释的值
                            Set<?> keySet = ((Map<?, ?>) val).keySet();

                            for(Object k:keySet){
                                if(!k.equals("!comment")){
                                    val = ((Map<?, ?>) val).get(k);
                                    break; // 找到非注释值后跳出循环
                                }
                            }

                        }
                        // 再写 key-value 对
                        if(val != null) {
                            if (val instanceof Map || val instanceof List){
                                writer.newLine();
                                dump(val, writer, indent + 2);
                            } else{
                                writer.write(yaml.dump(val).trim());
                                writer.newLine();
                            }

                        }

                    }

                }

            } else if (value instanceof List) {
                for(Object item : (List) value) {
                    writeIndent(writer,indent);
                    writer.write("- ");
                    if(item instanceof  Map || item instanceof List) {
                        writer.newLine();
                        dump(item,writer,indent +2);
                    } else {
                        writer.write(yaml.dump(item).trim());
                        writer.newLine();
                    }


                }


            }
        }

        private void writeComment(BufferedWriter writer, Map<String, String> val, int indent) throws IOException {

            String comment = val.get("!comment");
            for (String line : comment.split("\n")) {
                writeIndent(writer, indent);
                if (!line.trim().startsWith("#")) writer.write("# ");
                writer.write(line.trim());
                writer.newLine();
            }
        }


        private void writeIndent(BufferedWriter writer, int indent) throws IOException {
            for (int i = 0; i < indent; i++) {
                writer.write(" ");
            }
        }




        // 工厂方法
        public static YC fromFile(Path filePath) throws IOException {
            return new YC(filePath);
        }

        public static YC of(String yamlContent) {
            return new YC(yamlContent);
        }
    }


}

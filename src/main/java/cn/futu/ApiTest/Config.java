package cn.futu.ApiTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.util.HashMap;

public class Config {

    public String current="test";
    public HashMap<String, HashMap<String, String>> env;


    private Config(){};
    private static Config config;
    public static Config getInstance(){
        if (config==null){
            config = loadConfig("config/Config.yaml");
        }
        return config;
    }

    public static Config loadConfig(String path){
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(Config.class.getResourceAsStream(path), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

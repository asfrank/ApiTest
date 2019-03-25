package cn.futu.ApiTest;

import java.util.HashMap;

public class Restful {
    public String url;
    public String method;
    public HashMap<String, String> headers = new HashMap<>();
    public HashMap<String, String> query = new HashMap<>();
    public String body;
}

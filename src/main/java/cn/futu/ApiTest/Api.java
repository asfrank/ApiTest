package cn.futu.ApiTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarRequest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import static io.restassured.RestAssured.*;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Api {
    public Api(){
        useRelaxedHTTPSValidation();
    }

    public RequestSpecification getDefaultRequestSpecification(){
        return given().log().all();
    }

    public static String updateJsonFromMap(String path, HashMap<String, Object> map){
        DocumentContext documentContext = JsonPath.parse(Api.class.getResourceAsStream(path));
        map.forEach((key, value) -> documentContext.set(key, value));
        return documentContext.jsonString();
    }

    public Restful getApiFromYaml(String path){
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(getClass().getResourceAsStream(path), Restful.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Restful updateApiFromMap(Restful restful, HashMap<String, Object> map){
        if (map==null){
            return restful;
        }
        if (restful.method.toLowerCase().contains("get")){
            map.forEach((key, value) -> restful.query.replace(key, value.toString()));
        }
        if (restful.method.toLowerCase().contains("post")){
            if (map.containsKey("json_body")){
                restful.body = map.get("json_body").toString();
            }
            if (map.containsKey("json_file")){
                String filePath = map.get("json_file").toString();
                map.remove("json_file");
                restful.body = updateJsonFromMap(filePath, map);
            }
        }
        return restful;
    }

    public Response getResponseFromRestful(Restful restful){
        RequestSpecification requestSpecification = getDefaultRequestSpecification();

        if (restful.query!=null){
            restful.query.forEach((key, value) -> requestSpecification.queryParam(key, value));
        }
        if (restful.body!=null){
            requestSpecification.body(restful.body);
        }
        //fixed: 多环境支持
        String[] bind = updateUrl(restful.url);

        return requestSpecification
                .header("Host",bind[0])
                .when().request(restful.method, bind[1])
                .then().log().all().extract().response();
    }

    public Response getResponseFromYaml(String path, HashMap<String, Object> map){
        Restful restful = getApiFromYaml(path);
        restful = updateApiFromMap(restful, map);
        return getResponseFromRestful(restful);
    }

    public Restful getApiFromHar(String path, String pattern){
        HarReader harReader = new HarReader();
        try {
            Har har = harReader.readFromFile(new File(URLDecoder.decode(getClass().getResource(path).getFile()),"utf-8"));
            HarRequest request = new HarRequest();
            boolean match = false;
            for (HarEntry entry :har.getLog().getEntries()){
                request = entry.getRequest();
                if (request.getUrl().matches(pattern)){
                    match = true;
                    break;
                }
            }
            if (!match){
                request = null;
                throw new Exception("没有匹配的url");
            }
            Restful restful = new Restful();
            restful.method = request.getMethod().name().toLowerCase();
            //fixed: 去掉url中的query部分
            int index = request.getUrl().indexOf("?");
            restful.url = request.getUrl().substring(0,index);
            request.getQueryString().forEach(q->{
                restful.query.put(q.getName(), q.getValue());
            });
            restful.body = request.getPostData().getText();
            return restful;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Response getResponseFromHar(String path, String pattern, HashMap<String, Object> map){
        Restful restful = getApiFromHar(path, pattern);
        restful = updateApiFromMap(restful, map);
        return getResponseFromRestful(restful);
    }

    public String[] updateUrl(String url){
        HashMap<String, String> hosts = Config.getInstance().env.get(Config.getInstance().current);

        String host = "";
        String urlNew = "";
        for (Map.Entry<String ,String> entry : hosts.entrySet()){
            if (url.contains(entry.getKey())){
                host = entry.getKey();
                urlNew = url.replace(entry.getKey(), entry.getValue());
            }
        }
        return new String[]{host, urlNew};
    }
}

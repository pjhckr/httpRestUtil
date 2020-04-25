package qa;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qa.constants.AuthType;
import qa.constants.BodyType;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.reset;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.HttpClientConfig.httpClientConfig;

public class HttpRestUtil {
    private static Integer connectionTimeout = 160000;
    long waitForSleepTime = 10000;
    private static Logger log = LoggerFactory.getLogger(HttpRestUtil.class);
    private Gson gson = new Gson();
    private RequestSpecification requestSpecification;
    private String baseUri;
    private String basePath;
    private String request;
    private AuthType type;
    private String userName;
    private String password;
    private int defaultRunningStackTrace = 2;

    /**
     * To set authentication
     *
     * @param authType       preemptive/non_preemptive/bearer_token
     * @param userName       userName required in case of preemptive and non_preemptive
     * @param password_token password or session token
     */
    public void basic(AuthType authType, String userName, String password_token) {
        this.type = authType;
        this.userName = userName;
        this.password = password_token;
    }

    /**
     * This Function will evaluate the type of authentication needs to be added.
     */
    private void authenticationSetter() {
        if (this.type == AuthType.NON_PREEMPTIVE) {
            requestSpecification.auth().basic(this.userName, this.password);
        } else if (this.type == AuthType.PREEMPTIVE) {
            requestSpecification.auth().preemptive().basic(this.userName, this.password);
        } else if (this.type == AuthType.BEARER_TOKEN) {
            requestSpecification.auth().oauth2(this.password);
        }
    }

    /**
     * To set BaseURI for API
     *
     * @param baseUri base URI as a string
     */
    public void baseUri(String baseUri) {
        if (baseUri != null && !baseUri.isEmpty()) {
            if (this.baseUri != null) {
                if (!this.baseUri.equals(baseUri)) {
                    basePath = null;
                }
            }
            this.baseUri = baseUri.trim();
        }
    }

    /**
     * To set basePath for API
     *
     * @param basePath base Path as a string
     */
    public void basePath(String basePath) {
        if (baseUri != null && !baseUri.isEmpty()) {
            if (basePath != null && !basePath.isEmpty()) {
                this.basePath = basePath.trim();
            }
        }

    }

    /**
     * To set header
     *
     * @param header header in hashMap
     */
    private void header(Map header) {
        requestSpecification.headers(header);
    }

    /**
     * To set content type
     *
     * @param contentType Content type derived from ContectType class of restAssured.
     */
    private void contentType(ContentType contentType) {
        requestSpecification.contentType(contentType);
    }

    /**
     * To set Body as an object
     *
     * @param body
     */
    private void body(Object body) {
        request = gson.toJson(body);
        requestSpecification.body(body);
    }

    /**
     * To set Body with object mapper.
     *
     * @param body             body object
     * @param objectMapperType ObjectMapperType derived from restAssured
     */
    private void bodyWithObjectMappper(Object body, ObjectMapperType objectMapperType) {
        request = gson.toJson(body);
        requestSpecification.body(body, objectMapperType);
    }

    /**
     * To set body as params, which would be mapped to a MAP and sent to restAssured
     *
     * @param paramObject params object / pojo object
     */
    private void params(Object paramObject) {
        Map body;
        if (paramObject.getClass().getSuperclass() == HashMap.class) {
            body = (Map) paramObject;
        } else if (paramObject instanceof String) {
            body = stringToMapConverter(paramObject);
        } else {
            body = pojoObjToMap(paramObject);
        }
        request = gson.toJson(body);
        requestSpecification.params(body);
    }

    /**
     * To set formParam, which would be mapped to a MAP and sent to restAssured
     *
     * @param formParam formParam object / pojo object
     */
    private void formParam(Object formParam) {
        Map body;
        if (formParam.getClass().getSuperclass() == HashMap.class) {
            body = (Map) formParam;
        } else if (formParam instanceof String) {
            body = stringToMapConverter(formParam);
        } else {
            body = pojoObjToMap(formParam);
        }
        request = gson.toJson(body);
        requestSpecification.formParams(body);
    }

    /**
     * To set queryParam, which would be mapped to a MAP and sent to restAssured
     *
     * @param queryParam queryParam / pojo object
     */
    private void queryParam(Object queryParam) {
        Map body;
        if (queryParam.getClass().getSuperclass() == HashMap.class) {
            body = (Map) queryParam;
        } else if (queryParam instanceof String) {
            body = stringToMapConverter(queryParam);
        } else {
            body = pojoObjToMap(queryParam);
        }
        request = gson.toJson(body);
        requestSpecification.queryParams(body);
    }

    /**
     * pojo object to MAP converter
     *
     * @param pojoGsonObject input pojoObject
     * @return Map<Object, Object>
     */
    private Map<String, ?> pojoObjToMap(Object pojoGsonObject) {
        Type type = new TypeToken<Map<String, ?>>() {
        }.getType();
        return gson.fromJson(gson.toJson(pojoGsonObject, pojoGsonObject.getClass()), type);
    }

    /**
     * This method convert string given in below example and convert to map
     *
     * @param stringToMapObject - Eg., "email=hello,email=hi,email=,email=null,email=  ,email=abc@xyz.in,id=1,id=8,From=09XXXXXXXXXX1,Url=http://my.us2.abc.com/connect";
     * @return
     */
    private Map<String, ArrayList<String>> stringToMapConverter(Object stringToMapObject) {
        HashMap<String, ArrayList<String>> map = new HashMap<>();

        String query = (String) stringToMapObject;
        String[] paramStringsArray = query.split(",");
        for (String paramKeyValueString : paramStringsArray) {
            String[] paramKeyValueArray = paramKeyValueString.split("=");

            /**
             * duplicate key values will be put into list
             */
            if (!map.containsKey(paramKeyValueArray[0].trim())) {
                map.put(paramKeyValueArray[0].trim(), new ArrayList<>());
            }
            /**
             * checking length to handle "empty string" value
             */
            if (paramKeyValueArray.length == 2) {
                if (paramKeyValueArray[1].trim().equals("null")) {
                    map.get(paramKeyValueArray[0].trim()).add(null);
                } else {
                    /**
                     * adding parameter values to arraylist
                     */
                    map.get(paramKeyValueArray[0].trim()).add(paramKeyValueArray[1].trim());
                }

            } else {
                map.get(paramKeyValueArray[0].trim()).add("");
            }
        }
        return map;
    }

    /**
     * To process request and hit on endPoint on the basis of basic, baseURI, basePath
     *
     * @param methodName  request type like GET,POST,PUT,DELETE. class is derived from restAssured.
     * @param bodyType    type of body which is being sent to method
     * @param contentType type of content if required.
     * @param header      header in a HashMap
     * @param object      request object which could be pojo, string.
     * @return returns response object derived from restAssured.
     */
    private Response processMain(Method methodName, BodyType bodyType, ContentType contentType, Map header, Object object, Integer runningStackTrace) {
        request = null;

        RestAssuredConfig config = RestAssured.config().encoderConfig(encoderConfig().
                appendDefaultContentCharsetToContentTypeIfUndefined(false)).httpClient(httpClientConfig().
                setParam("http.connection.timeout", connectionTimeout).
                setParam("http.socket.timeout", connectionTimeout).
                setParam("http.connection-manager.timeout", connectionTimeout));

        requestSpecification = given().config(config);

        if (type != null) {
            authenticationSetter();
        }

        String endpoint;
        if (basePath != null && !basePath.isEmpty()) {
            endpoint = baseUri + basePath;


        } else {
            endpoint = baseUri;
        }

        if (header != null) {
            header(header);
        }

        if (contentType != null) {
            contentType(contentType);
        }

        if (object != null) {
            if (bodyType == BodyType.BODY) {
                body(object);
            } else if (bodyType == BodyType.BODY_GSON_OBJ) {
                bodyWithObjectMappper(object, ObjectMapperType.GSON);
            } else if (bodyType == BodyType.PARAMS) {
                params(object);
            } else if (bodyType == BodyType.FORM_PARAM) {
                formParam(object);
            } else if (bodyType == BodyType.QUERY_PARAM) {
                queryParam(object);
            }
        }

        String logReq, logBasePath;
        if (basePath == null) {
            logBasePath = "No BasePath Provided";
        } else {
            logBasePath = basePath;
        }
        if (request == null) {
            logReq = "No Body Provided";
        } else {
            logReq = request;
        }

        Response response;
        try {
            response = requestSpecification.request(methodName, endpoint);
            log.info("\n \n" + "----------------------------------------------------------------------------------------------------" + "\n \n"
                    + "Running Method: " + Thread.currentThread().getStackTrace()[runningStackTrace].getMethodName() + "\n \n"
                    + "Operation :" + methodName + "\n \n"
                    + "BodyType :" + bodyType + "\n \n"
                    + "Status Code :" + response.getStatusCode() + "\n \n"
                    + "BaseUrl :" + baseUri + "\n \n"
                    + "BasePath :" + logBasePath + "\n \n"
                    + "Final EndPoint To Hit :" + endpoint + "\n \n"
                    + "#############################-REQUEST-#############################"
                    + "\n \n" + logReq + "\n \n"
                    + "###########################-REQUEST_END-###########################" + "\n \n"
                    + "#############################-RESPONSE-#############################"
                    + "\n \n" + response.asString() + "\n \n"
                    + "###########################-RESPONSE_END-###########################"
                    + "\n \n" + "---------------------------------------------------------------------------------------------------" + "\n \n");
        } catch (Exception e) {
            log.error(e.toString());
            response = null;
        } finally {
            reset();
        }
        return response;
    }

    public Response process(Method methodName, BodyType bodyType, ContentType contentType, Map header, Object object, int runningMethodStackTrace) {
        return this.processMain(methodName, bodyType, contentType, header, object, runningMethodStackTrace);
    }

    public Response process(Method methodName, BodyType bodyType, ContentType contentType, Map header, Object object) {
        return this.process(methodName, bodyType, contentType, header, object, defaultRunningStackTrace);
    }

    public Response process(Method methodName, BodyType bodyType, Object object) {
        return this.process(methodName, bodyType, null, null, object);
    }

    public <T> T process(Method methodName, BodyType bodyType, ContentType contentType, Map header, Object object, Type responseGsonPojoClass) {
        Response response = this.process(methodName, bodyType, contentType, header, object);

        return response.as(responseGsonPojoClass, ObjectMapperType.GSON);
    }

    public Response process(Method methodName) {
        return this.process(methodName, BodyType.EMPTY, null);
    }

    public <T> T process(Method methodName, BodyType bodyType, ContentType contentType, Map header, Object object, Type responseGsonPojoClass, int runningMethodStackTrace) {
        Response response = this.process(methodName, bodyType, contentType, header, object, runningMethodStackTrace);
        return response.as(responseGsonPojoClass, ObjectMapperType.GSON);
    }

    public <T> T process(Method methodName, BodyType bodyType, Object object, Type responseGsonPojoClass) {
        return this.process(methodName, bodyType, null, null, object, responseGsonPojoClass);
    }

    public <T> T process(Method methodName, Type responseGsonPojoClass) {
        return this.process(methodName, BodyType.EMPTY, null, null, null, responseGsonPojoClass);
    }

    /**
     * This function "processXML" is for converting xml value of a API response into a POJO object and return the "T class" Type POJO.
     */
    public <T> T processXML(Method methodName, BodyType bodyType, ContentType contentType, Map header, Object object, Class pojoClass) {
        Response response = this.process(methodName, bodyType, contentType, header, object);

        XmlMapper xmlMapper = new XmlMapper();
        Object resp = null;

        String xmlString = response.asString();
        try {
            resp = xmlMapper.readValue(xmlString, pojoClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (T) resp;
    }
}

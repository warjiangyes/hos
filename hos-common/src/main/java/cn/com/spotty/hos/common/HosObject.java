package cn.com.spotty.hos.common;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;

public class HosObject {
    @Getter
    @Setter
    private ObjectMetaData metaData;
    @Getter
    @Setter
    private InputStream content;
    private Response response;

    public HosObject() {
    }

    public HosObject(Response response) {
        this.response = response;
    }

    public void close() {
        try {
            if (content != null) {
                this.content.close();
            }
            if (response != null) {
                response.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

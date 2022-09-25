package cn.com.spotty.hos;

import cn.com.spotty.hos.common.BucketModel;

import java.util.List;

public class HosSdkTest {

    private static String token = "7121a36f685d4c4784f19570ee79eabe";
    private static String endPoints = "http://127.0.0.1:9080";

    public static void main(String[] args) {
        final HosClient client = HosClientFactory.getOrClient(endPoints, token);
        try {
            List<BucketModel> bucketModelList = client.listBucket();
            bucketModelList.forEach(bucketModel -> {
                System.out.println(bucketModel.getBucketName());
            });
        } catch (Exception e) {
            //nothing to do
        }
    }
}

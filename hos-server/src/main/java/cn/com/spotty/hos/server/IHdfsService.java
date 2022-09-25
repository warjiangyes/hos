package cn.com.spotty.hos.server;

import java.io.IOException;
import java.io.InputStream;

public interface IHdfsService {
    void saveFile(String dir, String name, InputStream input, long length, short replication) throws IOException;

    void deleteFile(String dir, String name) throws IOException;

    InputStream openFile(String dir, String name) throws IOException;

    void mkDir(String dir) throws IOException;

    void deleteDir(String dir) throws IOException;
}

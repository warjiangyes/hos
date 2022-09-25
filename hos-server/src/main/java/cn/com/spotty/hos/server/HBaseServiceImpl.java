package cn.com.spotty.hos.server;

import cn.com.spotty.hos.core.ErrorCodes;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;

import java.io.IOException;
import java.util.List;

public class HBaseServiceImpl {
    // 1. 创建表
    public static boolean createTable(Connection connection, String tableName, String[] cfs, byte[][] splitKeys) {
        try (HBaseAdmin admin = (HBaseAdmin) connection.getAdmin()) {
            if (admin.tableExists(tableName)) {
                return false;
            }
            HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
            for (String cf : cfs) {
                HColumnDescriptor columnDescriptor = new HColumnDescriptor(cf);
                columnDescriptor.setMaxVersions(1);
                tableDescriptor.addFamily(columnDescriptor);
            }
            if (splitKeys != null) {
                admin.createTable(tableDescriptor, splitKeys);
            } else {
                admin.createTable(tableDescriptor);
            }

        } catch (Exception ex) {
            String msg = String.format("create table=%s error. msg=%s", tableName, ex.getMessage());
            throw new HosServerException(ErrorCodes.ERROR_HBASE, msg);
        }
        return true;
    }

    public static boolean createTable(Connection connection, String tableName, String[] cfs) {
        return createTable(connection, tableName, cfs, null);
    }

    // 2. 删除表
    public static boolean deleteTable(Connection connection, String tableName) {
        try (HBaseAdmin admin = (HBaseAdmin) connection.getAdmin()) {
            if (admin.tableExists(tableName)) {
                admin.disableTable(tableName);
                admin.deleteTable(tableName);
            }
        } catch (Exception ex) {
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete table error");
        }
        return true;
    }

    // 3. 删除列簇
    public static boolean deleteColumnFamily(Connection connection, String tableName, String cf) {
        try (HBaseAdmin admin = (HBaseAdmin) connection.getAdmin()) {
            if (admin.tableExists(tableName)) {
                admin.deleteColumn(tableName, cf);
            }
        } catch (Exception ex) {
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete cf error");
        }
        return true;
    }

    // 4. 删除列
    public static boolean deleteColumnQualifier(Connection connection, String tableName, String rowKey, String cf, String column) {
        Delete delete = new Delete(rowKey.getBytes());
        delete.addColumn(cf.getBytes(), column.getBytes());
        return deleteRow(connection, tableName, delete);
    }

    public static boolean deleteRow(Connection connection, String tableName, Delete delete) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.delete(delete);
        } catch (Exception ex) {
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete qualifier error");
        }
        return true;
    }

    // 5. 删除行
    public static boolean deleteRow(Connection connection, String tableName, String rowKey) {
        Delete delete = new Delete(rowKey.getBytes());
        return deleteRow(connection, tableName, delete);
    }

    // 6. 读取行
    public static Result getRow(Connection connection, String tableName, Get get) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.get(get);
        } catch (Exception ex) {
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "get row error");
        }
    }

    public static Result getRow(Connection connection, String tableName, String rowKey) {
        Get get = new Get(rowKey.getBytes());
        return getRow(connection, tableName, get);
    }

    public static Result getRow(Connection connection, String tableName, String rowKey, FilterList filterList) {
        Get get = new Get(rowKey.getBytes());
        get.setFilter(filterList);
        return getRow(connection, tableName, get);
    }

    public static Result getRow(Connection connection, String tableName, String rowKey, byte[] cf, byte[] qualifier) {
        Get get = new Get(rowKey.getBytes());
        get.addColumn(cf, qualifier);
        return getRow(connection, tableName, get);
    }


    // 7. 获取scanner
    public static ResultScanner getScanner(Connection connection, String tableName, Scan scan) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.getScanner(scan);
        } catch (Exception ex) {
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "get scanner error");
        }
    }

    public static ResultScanner getScanner(Connection connection, String tableName, byte[] startRowKey, byte[] stopRowKey) {
        Scan scan = new Scan();
        scan.setStartRow(startRowKey);
        scan.setStopRow(stopRowKey);
        return getScanner(connection, tableName, scan);
    }

    public static ResultScanner getScanner(Connection connection, String tableName, String startRowKey, String stopRowKey) {
        Scan scan = new Scan();
        scan.setStartRow(startRowKey.getBytes());
        scan.setStopRow(stopRowKey.getBytes());
        return getScanner(connection, tableName, scan);
    }

    public static ResultScanner getScanner(Connection connection, String tableName, byte[] startKey, byte[] endKey, FilterList filterList) {
        Scan scan = new Scan();
        scan.setStartRow(startKey);
        scan.setStopRow(endKey);
        scan.setFilter(filterList);
        scan.setCaching(1000);
        return getScanner(connection, tableName, scan);
    }

    // 8. 插入行
    public static boolean putRow(Connection connection, String tableName, Put put) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.put(put);
        } catch (Exception ex) {
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "put row error");
        }
        return true;
    }

    // 9. 批量插入行
    public static boolean putRows(Connection connection, String tableName, List<Put> puts) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.put(puts);
        } catch (Exception ex) {
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "put rows error");
        }
        return true;
    }

    // 10. incrementColumnValue, 通过该方法，生成目录的seqid
    public static long incrementColumnValue(Connection connection, String tableName, String rowKey, byte[] cf, byte[] qualifier, int value) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.incrementColumnValue(rowKey.getBytes(), cf, qualifier, value);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "incrementColumnValue error");
        }
    }

    public static boolean existsRow(Connection connection, String tableName, String row) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Get get = new Get(row.getBytes());
            return table.exists(get);
        } catch (IOException e) {
            String msg = String.format("check exists row from table=%s error. msg=%s", tableName, e.getMessage());
            throw new HosServerException(ErrorCodes.ERROR_HBASE, msg);
        }
    }
}

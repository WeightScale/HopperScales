package database;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public class Database implements AutoCloseable {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String substitute(String sql, Object... values) {
        int v = 0;
        StringBuilder builder = new StringBuilder();
        for (int c = 0; c < sql.length(); c++)
            if (sql.charAt(c) == '?') {
                if (values[v] instanceof String)
                    builder.append('\'').append(values[v]).append('\'');
                else if (values[v] == null)
                    builder.append("NULL");
                else
                    builder.append(values[v]);
                v++;
            } else
                builder.append(sql.charAt(c));
        return builder.toString();
    }

    public void initialize() throws SQLException {
        if (!statement.executeQuery("SELECT `name` FROM `sqlite_master` WHERE `type` = 'table' AND `name` = 'indications'").next())
            statement.execute("create table indications(`id` integer not null constraint indications_pk primary key autoincrement, `node` integer not null, `timestamp` timestamp default CURRENT_TIMESTAMP not null, `gross` integer not null, `net` integer not null)");
    }

    public final Connection connection;
    public final Statement statement;
    public ResultSet results;

    public void execute(String sql, Object... values) throws SQLException {
        statement.execute(substitute(sql, values));
    }

    public ResultSet executeQuery(String sql, Object... values) throws SQLException {
        results = statement.executeQuery(substitute(sql, values));
        return results;
    }

    public Database() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
        Properties properties = new Properties();
        properties.setProperty("characterEncoding", StandardCharsets.UTF_8.name());
        connection = DriverManager.getConnection("jdbc:sqlite:database.db", properties);
        statement = connection.createStatement();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    public void insertIndication(int node, int gross, int net) throws SQLException {
        execute("INSERT INTO `indications` (`node`, `gross`, `net`) VALUES (?, ?, ?)", node, gross, net);
    }

    public Indication[] selectIndications(String where) throws SQLException {
        executeQuery("SELECT `id`, `node`, strftime('%s', `timestamp`) AS `timestamp_`, `gross`, `net` FROM `indications`" + (!where.isEmpty() ? " WHERE " + where : ""));
        ArrayList<Indication> indications = new ArrayList<>();
        while (results.next()) {
            int timestamp = results.getInt("timestamp_");
            indications.add(new Indication(results.getInt("id"), results.getInt("node"), new Date(results.getInt("timestamp_") * 1000L), results.getInt("gross"), results.getInt("net")));
        }
        return indications.toArray(new Indication[0]);
    }

    public Indication selectIndication(int id) throws SQLException {
        return selectIndications(substitute("`id` = ?", id))[0];
    }

    public Indication[] selectIndications(Date fromDate, Date toDate) throws SQLException {
        return selectIndications(substitute("CAST(`timestamp_` AS INTEGER) >= ? AND CAST(`timestamp_` AS INTEGER) < ?", fromDate.getTime() / 1000, toDate.getTime() / 1000));
    }
}

package com.blank038.onlinereward.interfaces.execute.sub;

import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.data.cache.CommonData;
import com.blank038.onlinereward.data.cache.PlayerData;
import com.blank038.onlinereward.interfaces.execute.DataInterface;

import java.sql.*;

import com.blank038.onlinereward.interfaces.execute.ExecuteModel;
import com.blank038.onlinereward.util.Base64Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Blank038
 */
public class MySQLData extends DataInterface {
    private final String user = OnlineReward.getInstance().getConfig().getString("save-option.user"), url = OnlineReward.getInstance().getConfig().getString("save-option.url"),
            password = OnlineReward.getInstance().getConfig().getString("save-option.password");

    public MySQLData() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        createTable();
    }

    public void createTable() {
        connect((connection, statement) -> {
            try {
                String sql = "CREATE TABLE IF NOT EXISTS onlinereward (user VARCHAR(30) NOT NULL, data TEXT, locked INT, PRIMARY KEY ( user ))";
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void setLocked(Player player, boolean locked) {
        connect((connection, statement) -> {
            String sql = String.format("UPDATE onlinereward SET locked=%s WHERE user='%s'", (locked ? 0 : 1), player.getName());
            try {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean isLocked(Player player) {
        AtomicReference<Boolean> result = new AtomicReference<>();
        result.set(false);
        connect((connection, statement) -> {
            String sql = String.format("SELECT locked FROM onlinereward WHERE user='%s'", player.getName());
            try {
                ResultSet resultSet = statement.executeQuery(sql);
                while (resultSet.next()) {
                    int locked = resultSet.getInt("locked");
                    if (locked == 0) {
                        result.set(true);
                        break;
                    }
                }
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return result.get();
    }

    @Override
    public void save(PlayerData data, boolean locked) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("day", data.getDailyOnline());
        jsonObject.addProperty("time", data.getOnlineTime());
        // 读取奖励
        JsonArray rewards = new JsonArray();
        data.getRewards().forEach(rewards::add);
        jsonObject.add("rewards", rewards);
        JsonArray dayRewards = new JsonArray();
        data.getDayRewards().forEach(dayRewards::add);
        jsonObject.add("dayRewards", dayRewards);
        jsonObject.addProperty("dayOfYear", data.getResetDayOfYear());
        String text = Base64Util.encode(jsonObject);
        connect((connection, statement) -> {
            String sql;
            int number = locked ? 0 : 1;
            if (data.isNew()) {
                sql = String.format("INSERT INTO onlinereward (user,data,locked) VALUES ('%s','%s','%s')", data.getName(), text, number);
            } else {
                sql = String.format("UPDATE onlinereward SET data='%s',locked='%s' WHERE user='%s'", text, number, data.getName());
            }
            try {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public PlayerData get(String name) {
        if (CommonData.DATA_MAP.containsKey(name)) {
            return CommonData.DATA_MAP.get(name);
        }
        AtomicReference<JsonObject> jsonObject = new AtomicReference<>();
        connect((connection, statement) -> {
            String sql = String.format("SELECT data FROM onlinereward WHERE user='%s'", name);
            try {
                ResultSet resultSet = statement.executeQuery(sql);
                while (resultSet.next()) {
                    String data = resultSet.getString("data");
                    if (data != null) {
                        jsonObject.set(Base64Util.decode(data));
                    }
                }
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        if (jsonObject.get() == null) {
            JsonObject object = new JsonObject();
            object.addProperty("day", 0);
            object.addProperty("time", 0);
            object.addProperty("isNew", true);
            object.add("rewards", new JsonArray());
            object.add("dayRewards", new JsonArray());
            jsonObject.set(object);
        }
        return new PlayerData(name, jsonObject.get());
    }

    @Override
    public void connect(ExecuteModel executeModel) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            executeModel.run(connection, statement);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(connection, statement);
        }
    }

    @Override
    public void close(Connection connection, Statement statement) {
        try {
            if (connection != null) {
                connection.close();
            }
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

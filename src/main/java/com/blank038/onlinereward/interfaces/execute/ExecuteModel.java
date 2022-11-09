package com.blank038.onlinereward.interfaces.execute;

import java.sql.Connection;
import java.sql.Statement;

@FunctionalInterface
public interface ExecuteModel {

    void run(Connection connection, Statement statement);
}

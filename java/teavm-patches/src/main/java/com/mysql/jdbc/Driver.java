package com.mysql.jdbc;

/**
 * Present so that Class.forName("com.mysql.jdbc.Driver") succeeds in the browser; connections are made by
 * java.sql.DriverManager to the page's embedded database.
 */
public class Driver {
    public Driver() {
    }
}

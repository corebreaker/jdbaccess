package com.corebreaker.dbaccess.example;

import com.corebreaker.dbaccess.DbAccess;
import com.corebreaker.dbaccess.DbException;
import com.corebreaker.dbaccess.IConnection;

import com.corebreaker.dbaccess.example.entity.Person;
import com.corebreaker.dbaccess.example.model.IPerson;

public class Main
{
    private final static String DB_URL = "jdbc:mysql://localhost/example?user=example&password=example";

    private static void process(final IConnection aConn) throws DbException
    {
        final IPerson p = aConn.query(1, Person.class);

        System.out.println("Name of person with ID=1: " + p.getName());
    }

    public static void main(final String[] aArgs) throws Throwable
    {
        Class.forName ("com.mysql.jdbc.Driver");
        // Init database connection
        final DbAccess db = new DbAccess(DB_URL).debug(true);

        // Get a connection
        final IConnection conn = db.getConnection();

        try
        {
            process(conn);
        }
        finally
        {
            conn.close();
        }
    }
}

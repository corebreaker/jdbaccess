package com.corebreaker.dbaccess.example;

import com.corebreaker.dbaccess.DbAccess;
import com.corebreaker.dbaccess.DbException;
import com.corebreaker.dbaccess.DbOperation;
import com.corebreaker.dbaccess.IConnection;

import com.corebreaker.dbaccess.example.entity.Person;
import com.corebreaker.dbaccess.example.model.IPerson;
import com.corebreaker.dbaccess.util.DateUtil;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.time.Instant;
import java.util.List;

public class Main
{
    private final static String DB_URL = "jdbc:mysql://localhost/example?user=example&password=example";

    private static void process(final IConnection aConn) throws DbException
    {
        // Get by ID
        final IPerson p = aConn.query(2, Person.class);
        final String name = p.getName();

        System.out.println("Name of the person with ID=2: " + name);

        // Get FK
        final IPerson manager = p.getManager();

        if( manager != null )
            System.out.println("Name of the manager of the person with ID=2: " + manager.getName());

        // Update a field
        p.setName("karl");
        aConn.save(p);

        System.out.println("Name of the person with ID=2 after an update" + aConn.query(2, Person.class).getName());

        // Put the old name
        p.setName(name);
        aConn.save(p);

        // Create a new person
        final IPerson newPerson = new Person("victor", Date.from(Instant.parse("1990-02-03T00:00:00.00Z")), manager);

        aConn.save(newPerson);

        // Prepare query (get date 30 years earlier
        final Date birth = DateUtil.asDate(LocalDate.now().minusYears(30));

        // Query
        final IPerson anyPerson = new Person();

        anyPerson.setOperation("mBirth", DbOperation.LT);
        anyPerson.setBirth(birth);

        // Do query
        final List<IPerson> persons = aConn.query(anyPerson);

        // Result of the query
        System.out.println("Persons who were born before " + birth + ":");
        for(final IPerson person : persons)
        {
            System.out.println("   - " + person.getName() + " (" + person.getBirth() + ")");

            person.setName(person.getName() + " (me)");
        }

        // Modifications in a transaction
        aConn.save(persons);

        // Suery after transaction to check the modification
        final List<IPerson> modifiedPersons = aConn.query(anyPerson);

        // Result of the query
        System.out.println("Persons who were born before " + birth + " (after the modification):");
        for(final IPerson person : modifiedPersons)
        {
            System.out.println("   - " + person.getName() + " (" + person.getBirth() + ")");

            final String pName = person.getName();

            person.setName(pName.substring(0, pName.length() - 5));
        }

        // Another modifications in a transaction (cancels modifications done previously)
        aConn.save(modifiedPersons);

        // Remove a person
        newPerson.delete();
        aConn.save(newPerson);

    }

    public static void main(final String[] aArgs) throws Throwable
    {
        System.out.println(System.getProperty("javax.sql.rowset.RowSetFactory"));
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

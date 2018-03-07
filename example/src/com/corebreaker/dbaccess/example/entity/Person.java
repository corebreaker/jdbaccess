package com.corebreaker.dbaccess.example.entity;

import com.corebreaker.dbaccess.DbObject;
import com.corebreaker.dbaccess.NEntity;
import com.corebreaker.dbaccess.NField;
import com.corebreaker.dbaccess.example.model.IPerson;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
@NEntity("example.person") // Table name
public class Person extends DbObject implements IPerson
{
    @NField(value= "ref_manager", entity= Person.class) // Field name + foreign key
    private IPerson mManager = null;

    @NField("name") // Field name
    private String mName = null;

    public Person()
    {
        super();
    }

    public Person(final String aName)
    {
        super();

        mName = aName;
    }

    public Person(final String aName, final IPerson aManager)
    {
        super();

        mName= aName;
        mManager = aManager;
    }


    /**
     * @return the name of this person
     */
    @Override
    public String getName()
    {
        return mName;
    }

    /**
     * @param aName the name of this person
     */
    @Override
    public void setName(final String aName)
    {
        mName = aName;
    }

    /**
     * @return the manager of this person
     */
    @Override
    public IPerson getManager()
    {
        return mManager;
    }

    /**
     * @param aPerson the manager of this person
     */
    @Override
    public void setManager(final IPerson aPerson)
    {
        mManager = aPerson;
    }
}

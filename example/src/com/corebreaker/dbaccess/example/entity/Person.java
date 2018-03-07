package com.corebreaker.dbaccess.example.entity;

import com.corebreaker.dbaccess.DbObject;
import com.corebreaker.dbaccess.NEntity;
import com.corebreaker.dbaccess.NField;
import com.corebreaker.dbaccess.example.model.IPerson;

import java.util.Date;

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

    @NField("birth") // Field name
    private Date mBirth = null;

    public Person()
    {
        super();
    }

    public Person(final String aName, final Date aBirth)
    {
        super();

        mName = aName;
        mBirth = aBirth;
    }

    public Person(final String aName, final Date aBirth, final IPerson aManager)
    {
        super();

        mName= aName;
        mManager = aManager;
        mBirth = aBirth;
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

    /**
     * @return the bithdate of this person
     */
    @Override
    public Date getBirth()
    {
        return mBirth;
    }

    /**
     * @param aBirth the bithdate of this person
     */
    @Override
    public void setBirth(final Date aBirth)
    {
        mBirth = aBirth;
    }
}

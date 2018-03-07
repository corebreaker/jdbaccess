package com.corebreaker.dbaccess.example.model;

import com.corebreaker.dbaccess.IObject;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public interface IPerson extends IObject
{
    /**
     * @return the name of this person
     */
    String getName();

    /**
     * @param aName the name of this person
     */
    void setName(final String aName);

    /**
     * @return the manager of this person
     */
    IPerson getManager();

    /**
     * @param aPerson the manager of this person
     */
    void setManager(final IPerson aPerson);
}

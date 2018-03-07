/**
 *
 */
package com.corebreaker.dbaccess;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public interface IObject
{
   /**
    * @return
    */
   Long getId();

   /**
    *
    */
   void delete();

   /**
    * @param aFieldName
    */
   DbObject setNull(final String aFieldName);

   /**
    * @param aFieldName
    */
   DbObject addGroupBy(final String aFieldName);

   /**
    * @param aFieldName
    */
   DbObject addOrderBy(final String aFieldName);

   /**
    * @param aFieldName
    * @param aDesc
    */
   DbObject addOrderBy(final String aFieldName, boolean aDesc);

   /**
    * @param aFieldName
    * @param aOp
    */
   DbObject setOperation(final String aFieldName, final DbOperation aOp);
}

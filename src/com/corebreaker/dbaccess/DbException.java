/**
 *
 */
package com.corebreaker.dbaccess;

import java.sql.SQLException;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public class DbException extends SQLException
{
   /**
    *
    */
   private static final long serialVersionUID = 5396827524340229167L;

   /**
    *
    */
   public DbException()
   {
      super();
   }

   /**
    * @param aMessage
    */
   public DbException(final String aMessage)
   {
      super(aMessage);
   }

   /**
    * @param aMessage
    * @param aCause
    */
   public DbException(final String aMessage, final Exception aCause)
   {
      super(aMessage, aCause);
   }
}

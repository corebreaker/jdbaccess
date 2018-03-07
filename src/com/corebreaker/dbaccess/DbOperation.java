/**
 *
 */
package com.corebreaker.dbaccess;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public enum DbOperation
{
   LIKE("LIKE"),
   NOT_LIKE("NOT LIKE"),
   LT("<"),
   GT(">"),
   LE("<="),
   GE(">="),
   NE("<>");

   private String mOperator;

   private DbOperation(final String aOperator)
   {
      mOperator= aOperator;
   }

   public String getOperator()
   {
      return mOperator;
   }
}

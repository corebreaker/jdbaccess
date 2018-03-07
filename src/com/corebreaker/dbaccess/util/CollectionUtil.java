/**
 *
 */
package com.corebreaker.dbaccess.util;

import java.util.Collection;
import java.util.Vector;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public class CollectionUtil
{
   /**
    * @param aC
    * @param aGlue
    * @return
    */
   public static String join(final Collection<?> aC, final String aGlue)
   {
      return CollectionUtil.join(aC, aGlue, "", "", false);
   }

   /**
    * @param aC
    * @param aGlue
    * @param aPrefix
    * @param aSuffix
    * @return
    */
   public static String join(final Collection<?> aC, final String aGlue, final String aPrefix, final String aSuffix)
   {
      return CollectionUtil.join(aC, aGlue, aPrefix, aSuffix, false);
   }

   /**
    * @param aC
    * @param aGlue
    * @param aPrefix
    * @param aSuffix
    * @param aForceBounds
    * @return
    */
   public static String join(final Collection<?> aC, final String aGlue, final String aPrefix, final String aSuffix, final boolean aForceBounds)
   {
      final StringBuilder lRes= new StringBuilder();

      try
      {
         for(final Object fO : aC)
         {
            lRes.append(aGlue);
            lRes.append(fO);
         }
      }
      catch(final NullPointerException eNp)
      {
      }

      if( lRes.length() == 0 )
      {
         if( aForceBounds )
         {
            lRes.append(aPrefix);
            lRes.append(aSuffix);
         }
      }
      else
      {
         lRes.replace(0, aGlue.length(), aPrefix);
         lRes.append(aSuffix);
      }

      return lRes.toString();
   }

   /**
    * @param aIterable
    * @return
    */
   public static <T> Vector<T> list(final Iterable<T> aIterable)
   {
      final Vector<T> lRes= new Vector<T>();

      for(final T fElt : aIterable)
         lRes.add(fElt);

      return lRes;
   }
}

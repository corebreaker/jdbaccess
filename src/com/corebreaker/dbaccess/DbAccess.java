package com.corebreaker.dbaccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.corebreaker.dbaccess.util.Notifier;
import com.corebreaker.dbaccess.util.Application;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public class DbAccess
{
   private final static String DB_PREFIX = "com.corebreaker.dbaccess.";

   /**
    * @param aClassname
    * @return
    */
   /*
   private static boolean loadDriver(final String aClassname)
   {
      if( aClassname == null )
         return false;

      try
      {
         Class.forName(aClassname);
      }
      catch(final Exception e)
      {
      }

      return true;
   }
   */

   /**
    * @throws IOException
    */
   /*
   private static void loadDrivers()
      throws IOException
   {
      final InputStream lStream= DbAccess.class.getClassLoader().getResourceAsStream("META-INF/drivers.lst");
      final BufferedReader lIn= new BufferedReader(new InputStreamReader(lStream));

      try
      {
         while( DbAccess.loadDriver(lIn.readLine()) )
            ;
      }
      finally
      {
         try
         {
            lIn.close();
         }
         catch(final Exception e)
         {
         }
      }
   }

   private static DbAccess sInstance = null;
*/
   /**
    * @return
    */
   /*
   public static DbAccess i()
   {
      if( DbAccess.sInstance == null )
      {
         try
         {
            final String iUrl= System.getProperty(DbAccess.DB_PREFIX + "url").replace("${root}", Application.ROOT.getAbsolutePath());

            DbAccess.loadDrivers();

            DbAccess.sInstance= new DbAccess(iUrl);
         }
         catch(final Exception e)
         {
            e.printStackTrace();

            System.exit(100);
         }
      }

      return DbAccess.sInstance;
   }
   */

   private final Notifier           mNotifier       = new Notifier();

   private final Set<DbConnection>  mAllConnections = new HashSet<DbConnection>();
   private final List<DbConnection> mAvailableConnections;

   /**
    * @param aDbUrl
    * @throws SQLException
    */
   public DbAccess(final String aDbUrl) throws SQLException
   {
      super();

      int lCount= 8;
      final Properties lProps= new Properties();

      try
      {
         lCount= Integer.parseInt(System.getProperty(DbAccess.DB_PREFIX + "pool.size"));
      }
      catch(final Exception eEx)
      {
      }

      lProps.setProperty("autoReconnect", "true");

      for(int fI= 0; fI < lCount; fI++)
         mAllConnections.add(new DbConnection(fI, DriverManager.getConnection(aDbUrl, lProps)));

      mAvailableConnections= new ArrayList<DbConnection>(mAllConnections);
   }

   /**
    * @see java.lang.Object#finalize()
    */
   @Override
   protected void finalize()
      throws Throwable
   {
      mAvailableConnections.clear();
      mAllConnections.clear();

      super.finalize();
   }

   /**
    * @param aDebug
    */
   public DbAccess debug(final boolean aDebug)
   {
      for(final DbConnection fConn : mAllConnections)
         fConn.debug(aDebug);

      return this;
   }

   /**
    * @return
    */
   public synchronized IConnection getConnection()
   {
      if( mAvailableConnections.isEmpty() )
         mNotifier.await();

      return new IConnection()
      {
         private final DbConnection mCurrent = mAvailableConnections.remove(0);

         public int count(final IObject aKey)
         {
            return mCurrent.count(aKey);
         }

         public int count(final Class<? extends DbObject> aDbClass)
         {
            return mCurrent.count(aDbClass);
         }

         public void save(final Collection<? extends IObject> aDatas)
            throws DbException
         {
            mCurrent.save(aDatas);
         }

         public void save(final IObject... aDatas)
            throws DbException
         {
            mCurrent.save(aDatas);
         }

         public <U extends IObject> List<U> query(final U aKey)
         {
            return mCurrent.query(aKey);
         }

         public <U extends IResult> List<U> query(final Class<U> aResClass, final IObject aKey)
         {
            return mCurrent.query(aResClass, aKey);
         }

         public <U extends DbObject> List<U> query(final Class<U> aDbClass)
         {
            return mCurrent.query(aDbClass);
         }

         public <U extends IObject> List<U> query(final U aKey, final int aOffset, final int aCount)
         {
            return mCurrent.query(aKey, aOffset, aCount);
         }

         public <U extends IResult> List<U> query(final Class<U> aResClass, final IObject aKey, final int aOffset, final int aCount)
         {
            return mCurrent.query(aResClass, aKey, aOffset, aCount);
         }

         public <U extends DbObject> List<U> query(final Class<U> aDbClass, final int aOffset, final int aCount)
         {
            return mCurrent.query(aDbClass, aOffset, aCount);
         }

         public <U extends DbObject> U query(final long aId, final Class<U> aDbClass) throws DbException
         {
            return mCurrent.query(aId, aDbClass);
         }

         @Override
         public void close()
         {
            mAvailableConnections.add(mCurrent);

            mNotifier.signal();
         }
      };

   }
}

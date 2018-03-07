package com.corebreaker.dbaccess;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import com.corebreaker.dbaccess.util.CollectionUtil;
import com.corebreaker.dbaccess.util.ConvertedList;
import com.corebreaker.dbaccess.util.SortedValueMap;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
@SuppressWarnings("restriction")
public class DbConnection implements IConnection
{
   /**
    * @author Frédéric Meyer
    * @param <T>
    */
   private interface IFactory<T>
   {
      /**
       * @return
       */
      T create() throws InstantiationException, IllegalAccessException;

      /**
       * @param aObj
       * @param aData
       */
      void setData(T aObj, IDataGetter aData);
   }

   /**
    * @author Frédéric Meyer
    * @param <T>
    */
   private static class DbResultFactory<T extends IResult> implements IFactory<T>
   {
      /**
       * @author Frédéric Meyer
       */
      private static class ColumnSpecifier
      {
         private final String              mMethod;
         private final String              mColumn;
         private Class<? extends IObject>  mInterface = null;
         private Class<? extends DbObject> mEntity    = null;

         @SuppressWarnings("unchecked")
         ColumnSpecifier(final Method aMethod)
         {
            super();

            mMethod= aMethod.getName();
            mColumn= aMethod.getAnnotation(NColumn.class).value() + " AS " + mMethod;

            final Class<?> lType= aMethod.getReturnType();

            if( IObject.class.isAssignableFrom(lType) )
            {
               mInterface= (Class<? extends IObject>) lType;

               final Class<? extends DbObject> iEntity= aMethod.getAnnotation(NField.class).entity();

               if( iEntity == null || DbObject.class.equals(iEntity) )
               {
                  try
                  {
                     mEntity= (Class<? extends DbObject>) Class.forName(mInterface.getName().replace(".model.I", ".entity."));
                  }
                  catch(final ClassNotFoundException e)
                  {
                     e.printStackTrace();

                     System.exit(110);
                  }
               }
               else
                  mEntity= iEntity;
            }
            else if( DbObject.class.isAssignableFrom(lType) )
               mEntity= (Class<? extends DbObject>) lType;
         }

         /**
          * @return
          */
         Class<? extends IObject> getInterface()
         {
            return mInterface;
         }

         /**
          * @return
          */
         Class<? extends DbObject> getEntity()
         {
            return mEntity;
         }

         /**
          * @return
          */
         String getDbName()
         {
            return mMethod;
         }

         /**
          * @see java.lang.Object#equals(java.lang.Object)
          */
         @Override
         public boolean equals(final Object aObj)
         {
            try
            {
               return ColumnSpecifier.class.cast(aObj).mMethod.equals(mMethod);
            }
            catch(final ClassCastException eCc)
            {
               return aObj.toString().equals(mMethod);
            }
            catch(final NullPointerException eNp)
            {
               return false;
            }
         }

         /**
          * @see java.lang.Object#hashCode()
          */
         @Override
         public int hashCode()
         {
            return mMethod.hashCode();
         }

         /**
          * @return the column
          */
         String getColumn()
         {
            return mColumn;
         }
      }

      /**
       * @author Frédéric Meyer
       */
      private static class ResultSpecifier implements Iterable<ColumnSpecifier>
      {
         private final String                       mName;
         private final String                       mColumns;
         private final Map<String, ColumnSpecifier> mFields = new HashMap<String, ColumnSpecifier>();

         /**
          * @param aClass
          */
         ResultSpecifier(final Class<?> aClass)
         {
            super();

            mName= aClass.getName();

            final List<String> lColumns= new LinkedList<String>();

            for(final Method fMeth : aClass.getDeclaredMethods())
            {
               if( (fMeth.getParameterTypes().length > 0) || fMeth.getReturnType().equals(void.class) )
                  continue;

               try
               {
                  final ColumnSpecifier wSpec= new ColumnSpecifier(fMeth);

                  mFields.put(fMeth.getName(), wSpec);
                  lColumns.add(wSpec.getColumn());
               }
               catch(final Exception eEx)
               {
               }
            }

            if( lColumns.isEmpty() )
               throw new Error("No annotation found in type " + aClass.getName());

            mColumns= CollectionUtil.join(lColumns, ", ");
         }

         /**
          * @see java.lang.Object#equals(java.lang.Object)
          */
         @Override
         public boolean equals(final Object aObj)
         {
            try
            {
               return ResultSpecifier.class.cast(aObj).mName.equals(mName);
            }
            catch(final ClassCastException eCc)
            {
               return aObj.toString().equals(mName);
            }
            catch(final NullPointerException eNp)
            {
               return false;
            }
         }

         /**
          * @see java.lang.Object#hashCode()
          */
         @Override
         public int hashCode()
         {
            return mName.hashCode();
         }

         /**
          * @see java.lang.Object#toString()
          */
         @Override
         public String toString()
         {
            return mName;
         }

         /**
          * @see java.lang.Iterable#iterator()
          */
         public Iterator<ColumnSpecifier> iterator()
         {
            return mFields.values().iterator();
         }

         /**
          * @return
          */
         public String getColumns()
         {
            return mColumns;
         }
      }

      /**
       * @author Frédéric Meyer
       */
      private static class ResultHandler implements InvocationHandler
      {
         private final Map<String, Object> mValues = new HashMap<String, Object>();

         /**
          * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
          */
         public Object invoke(final Object aProxy, final Method aMethod, final Object[] aArgs)
            throws Throwable
         {
            return mValues.get(aMethod.getName());
         }

         /**
          * @param aName
          * @param aObject
          */
         public void set(final String aName, final Object aObject)
         {
            mValues.put(aName, aObject);
         }
      }

      private final static Map<Class<?>, ResultSpecifier> RES_STRUCTS = new HashMap<Class<?>, ResultSpecifier>();

      private final Class<T>                              mClass;
      private final ResultSpecifier                       mSpec;

      /**
       * @param aClass
       */
      DbResultFactory(final Class<T> aClass)
      {
         super();

         mClass= aClass;

         ResultSpecifier lSpec= DbResultFactory.RES_STRUCTS.get(aClass);

         if( lSpec == null )
         {
            lSpec= new ResultSpecifier(aClass);
            DbResultFactory.RES_STRUCTS.put(aClass, lSpec);
         }

         mSpec= lSpec;
      }

      /**
       * @see IFactory#create()
       */
      public T create()
         throws InstantiationException, IllegalAccessException
      {
         return mClass.cast(Proxy.newProxyInstance(mClass.getClassLoader(), new Class<?>[] { mClass }, new ResultHandler()));
      }

      /**
       * @see IFactory#setData(Object, IDataGetter)
       */
      public void setData(final T aObj, final IDataGetter aData)
      {
         try
         {
            final ResultHandler lRes= ResultHandler.class.cast(Proxy.getInvocationHandler(aObj));

            for(final ColumnSpecifier fCol : mSpec)
               try
               {
                  lRes.set(fCol.getDbName(), aData.get(fCol.getDbName(), fCol.getInterface(), fCol.getEntity()));
               }
               catch(final Exception eEx)
               {
                  eEx.printStackTrace();
               }
         }
         catch(final Exception eEx)
         {
            eEx.printStackTrace();
         }
      }

      /**
       * @return
       */
      String getColumns()
      {
         return mSpec.getColumns();
      }
   }

   /**
    * @author Frédéric Meyer
    * @param <T>
    */
   private static class DbObjectFactory<T extends DbObject> implements IFactory<T>
   {
      private final Class<T> mClass;

      /**
       * @param aClass
       */
      DbObjectFactory(final Class<T> aClass)
      {
         super();

         mClass= aClass;
      }

      /**
       * @see IFactory#create()
       */
      public T create()
         throws InstantiationException, IllegalAccessException
      {
         return mClass.newInstance();
      }

      /**
       * @see IFactory#setData(Object, IDataGetter)
       */
      public void setData(final T aObj, final IDataGetter aData)
      {
         aObj.importData(aData);
      }
   }

   /**
    * @author Frédéric Meyer
    * @param <U>
    * @param <V>
    */
   private static class Desc<U, V> implements Comparable<Desc<U, V>>
   {
      final Integer mIndex;
      final U       mIdentifier;
      final V       mValue;

      Desc(final int aIndex, final U aIdentifier, final V aValue)
      {
         super();

         mIndex= aIndex;
         mIdentifier= aIdentifier;
         mValue= aValue;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(final Object aObj)
      {
         try
         {
            return Desc.class.cast(aObj).mIndex == mIndex;
         }
         catch(final Exception e)
         {
            return false;
         }
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode()
      {
         return mIndex.hashCode();
      }

      /**
       * @param aDesc
       * @return
       */
      public int compareTo(final Desc<U, V> aDesc)
      {
         return mIndex.compareTo(aDesc.mIndex);
      }
   }

   /**
    * @author Frédéric Meyer
    */
   private static class ValueDesc extends Desc<DbOperation, Object>
   {
      /**
       * @param aIndex
       * @param aOp
       * @param aValue
       */
      ValueDesc(final int aIndex, final DbOperation aOp, final Object aValue)
      {
         super(aIndex, aOp, aValue);
      }
   }

   /**
    * @author Frédéric Meyer
    */
   private static class ParamDesc extends Desc<String, ValueDesc>
   {
      /**
       * @param aIndex
       * @param aName
       * @param aValue
       */
      ParamDesc(final int aIndex, final String aName, final ValueDesc aValue)
      {
         super(aIndex, aName, aValue);
      }
   }

   /**
    * @author Frédéric Meyer
    */
   private static class TableDesc extends Desc<PreparedStatement, Map<String, ParamDesc>>
   {
      /**
       * @param aIndex
       * @param aStmt
       * @param aDescs
       */
      TableDesc(final int aIndex, final PreparedStatement aStmt, final Map<String, ParamDesc> aDescs)
      {
         super(aIndex, aStmt, aDescs);
      }
   }

   /**
    * @author Frédéric Meyer
    */
   private class EntityHandler implements InvocationHandler
   {
      private DbObject                        mEntity = null;
      private final Class<? extends DbObject> mClass;
      private final long                      mId;

      EntityHandler(final Class<? extends DbObject> aEntity, final long aId)
      {
         super();

         mClass= aEntity;
         mId= aId;
      }

      /**
       * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
       */
      @Override
      public Object invoke(final Object aProxy, final Method aMethod, final Object[] aArgs)
         throws Throwable
      {
         final String lMethName= aMethod.getName();

         if( lMethName.equals("getId") )
            return mId;

         if( mEntity == null )
            mEntity= query(mId, mClass);

         return aMethod.invoke(mEntity, aArgs);
      }

      /**
       * @return
       */
      DbObject get()
      {
         return mEntity;
      }
   }

   private static RowSetFactory sRowsetFactory = null;

   private static CachedRowSet createCachedRowset() throws SQLException
   {
      if( sRowsetFactory == null )
         sRowsetFactory = RowSetProvider.newFactory("com.corebreaker.dbaccess.rowset.RowSetFactory", null);

      return sRowsetFactory.createCachedRowSet();
   }

   /**
    * @param aName
    * @param aInsert
    * @param aDelete
    * @return
    */
   private static String makeTableId(final String aName, final boolean aInsert, final boolean aDelete)
   {
      if( aDelete )
         return ((aInsert) ? "DK:" : "DI:") + aName;
      else
         return ((aInsert) ? "IN:" : "UP:") + aName;
   }

   /**
    * @param aValues
    * @return
    */
   private static String makeWhere(final Collection<ParamDesc> aValues)
   {
      String lRes= "";

      for(final ParamDesc fDesc : aValues)
      {
         final String wOp= (fDesc.mValue.mIdentifier == null) ? "=" : fDesc.mValue.mIdentifier.getOperator();

         lRes+= " AND (" + fDesc.mIdentifier + " " + wOp + " ?)";
      }

      return (lRes.length() == 0) ? "" : (" WHERE" + lRes.substring(4));
   }

   private boolean          mDebug = false;
   private final Integer    mConnId;
   private final Connection mConn;
   private final boolean    mSavepoints;
   private final boolean    mTransactions;

   /**
    * @param aId
    * @param aConnection
    */
   DbConnection(final int aId, final Connection aConnection)
   {
      super();

      mConnId= aId;
      mConn= aConnection;

      try
      {
         mConn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
      }
      catch(final SQLException eSql)
      {
      }

      try
      {
         mConn.setAutoCommit(false);
         mConn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
      }
      catch(final SQLException eSql)
      {
      }

      boolean lSavepoints;
      boolean lTransactions;

      try
      {
         lSavepoints= mConn.getMetaData().supportsSavepoints();
      }
      catch(final SQLException e)
      {
         lSavepoints= false;
      }

      try
      {
         lTransactions= mConn.getMetaData().supportsTransactions();
      }
      catch(final SQLException e)
      {
         lTransactions= false;
      }

      mSavepoints= lSavepoints;
      mTransactions= lTransactions;

      if( lSavepoints || lTransactions )
         try
         {
            mConn.setAutoCommit(false);
         }
         catch(final SQLException e)
         {
         }
   }

   /**
    * @param aCurs
    * @param aList
    * @param aF
    * @param aOffset
    * @param aCount
    * @return
    * @throws SQLException
    */
   private <T> ResultSet populate(final ResultSet aCurs, final List<T> aList, final IFactory<T> aF, final int aOffset, final int aCount)
      throws SQLException
   {
      try
      {
         ResultSet lSet= aCurs;

         if( (aOffset > 0) || (aCount > 0) )
         {
            final CachedRowSet iSet= createCachedRowset();

            if( aCount > 0 )
               iSet.setPageSize(aCount);

            iSet.populate(aCurs, aOffset + 1);

            lSet= iSet;
         }

         final ResultSet lCurs= lSet;
         int lCount= 0;

         if( mDebug )
         {
            final ResultSetMetaData iMeta= lCurs.getMetaData();
            final List<String> iRes= new ArrayList<String>();
            final int iCnt= iMeta.getColumnCount();

            for(int fI= 1; fI <= iCnt; fI++)
               iRes.add(iMeta.getColumnName(fI));

            System.out.println(String.format("[%05d]:    COLUMNS= %s", mConnId, iRes));
         }

         while( lCurs.next() )
         {
            try
            {
               final T wRes= aF.create();

               aF.setData(wRes, new IDataGetter()
               {
                  public Object get(final String aColumnName, final Class<? extends IObject> aModel,
                        final Class<? extends DbObject> aEntity)
                     throws SQLException
                  {
                     final Object lVal= lCurs.getObject(aColumnName);

                     if( (lVal == null) || ((aModel == null) && (aEntity == null)) )
                        return lVal;
                     else
                     {
                        final long iId= lCurs.getLong(aColumnName);

                        if( aModel == null )
                           return query(iId, aEntity);
                        else
                           return Proxy.newProxyInstance(aModel.getClassLoader(), new Class[] { aModel }, new EntityHandler(aEntity, iId));
                     }
                  }
               });

               aList.add(wRes);
               lCount++;
            }
            catch(final Exception eEx)
            {
               eEx.printStackTrace();
            }
         }

         if( mDebug )
            System.out.println(String.format("[%05d]:    RESULT COUNT= %d", mConnId, lCount));

         return lCurs;
      }
      finally
      {
         if( (aOffset > 0) || (aCount > 0) )
         {
            try
            {
               aCurs.close();
            }
            catch(final Exception eEx)
            {
            }
         }

      }
   }

   /**
    * @param aKey
    * @param aFields
    * @return
    * @throws SQLException
    */
   private PreparedStatement makeGetStatement(final DbObject aKey, final String aFields)
      throws SQLException
   {
      final Set<ParamDesc> lValues= new HashSet<ParamDesc>();
      final List<String> lGroupBy= new LinkedList<String>();
      final List<String> lOrderBy= new LinkedList<String>();

      aKey.updateData(new IDataSetter()
      {
         private int mIdx = 1;

         public void setId(final Long aId)
         {
         }

         public void delete()
         {
         }

         public void listGroupBy(final List<String> aGroupBy)
         {
            lGroupBy.addAll(aGroupBy);
         }

         public void listOrderBy(final List<String> aOrderBy)
         {
            lOrderBy.addAll(aOrderBy);
         }

         public void set(final String aColumnName, final Object aValue, final boolean aIsNull, final DbOperation aOp)
         {
            if( aIsNull )
            {
               final int iIdx= mIdx++;

               lValues.add(new ParamDesc(iIdx, aColumnName, new ValueDesc(iIdx, aOp, null)));
            }
            else if( aValue != null )
            {
               final int iIdx= mIdx++;

               lValues.add(new ParamDesc(iIdx, aColumnName, new ValueDesc(iIdx, aOp, aValue)));
            }
         }
      });

      final String lQuery= "SELECT " + aFields +
                           " FROM " + aKey.getDbTable() +
                           DbConnection.makeWhere(lValues) +
                           CollectionUtil.join(lGroupBy, ", ", " GROUP BY ", "") +
                           CollectionUtil.join(lOrderBy, ", ", " ORDER BY ", "");

      if( mDebug )
         System.out.println(String.format("[%05d]: QUERY= %s [%d]", mConnId, lQuery, lValues.size()));

      final PreparedStatement lStmt= mConn.prepareStatement(lQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
      final ParameterMetaData lMetas= lStmt.getParameterMetaData();
      final String[] lParamVals= new String[lValues.size()];

      for(final ParamDesc fDesc : lValues)
      {
         Object wValue= fDesc.mValue.mValue;

         try
         {
            final Class<?> wType= Class.forName(lMetas.getParameterClassName(fDesc.mIndex));

            if( Date.class.isAssignableFrom(wType) )
               wValue= wType.getConstructor(long.class).newInstance(Date.class.cast(wValue).getTime());
            else if( wValue instanceof IObject )
               wValue= IObject.class.cast(wValue).getId();
         }
         catch(final Exception eEx)
         {
         }

         lStmt.setObject(fDesc.mIndex, wValue);

         if( mDebug )
            lParamVals[fDesc.mIndex - 1]= String.format("<%s>", wValue);
      }

      if( mDebug )
         System.out.println(String.format("[%05d]:    VALLUES= %s", mConnId, Arrays.asList(lParamVals)));

      return lStmt;
   }

   /**
    * @see java.lang.Object#finalize()
    */
   @Override
   protected void finalize()
      throws Throwable
   {
      close();

      super.finalize();
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      return mConnId.hashCode();
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(final Object aObj)
   {
      try
      {
         return DbConnection.class.cast(aObj).mConnId.equals(mConnId);
      }
      catch(final Exception eEx)
      {
         return false;
      }
   }

   /**
    * @param aDebug
    */
   void debug(final boolean aDebug)
   {
      mDebug= aDebug;
   }

   /**
    * @see IConnection#query(long, Class)
    */
   public <U extends DbObject> U query(final long aId, final Class<U> aDbClass)
      throws DbException
   {
      final List<U> lRes= new ArrayList<U>();
      Statement lStmt= null;
      ResultSet lCurs= null;

      try
      {
         final U lRec= aDbClass.newInstance();
         final String lQuery= "SELECT * FROM " + lRec.getDbTable() + " WHERE id = " + aId;

         if( mDebug )
            System.out.println(String.format("[%05d]: QUERY= %s", mConnId, lQuery));

         lStmt= mConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

         try
         {
            lStmt.setMaxRows(1);
         }
         catch(final SQLException e)
         {
         }

         lCurs= lStmt.executeQuery(lQuery);
         lCurs= populate(lCurs, lRes, new DbObjectFactory<U>(aDbClass), 0, 1);
      }
      catch(final Exception eEx)
      {
         eEx.printStackTrace();
      }

      try
      {
         lCurs.close();
      }
      catch(final Exception e)
      {
      }

      try
      {
         lStmt.close();
      }
      catch(final Exception e)
      {
      }

      if( lRes.size() == 0 )
         throw new DbException("Not found");

      return lRes.get(0);
   }

   /**
    * @see IConnection#count(IObject)
    */
   public int count(final IObject aKey)
   {
      DbObject lObj= null;

      if( Proxy.isProxyClass(aKey.getClass()) )
         lObj= EntityHandler.class.cast(Proxy.getInvocationHandler(aKey)).get();
      else if( aKey instanceof DbObject )
         lObj= DbObject.class.cast(aKey);

      int lRes= 0;

      if( lObj != null )
      {
         final int iRes= countEntities(lObj);

         lRes= iRes;
      }

      return lRes;
   }

   /**
    * @param aKey
    * @return
    */
   private int countEntities(final DbObject aKey)
   {
      int lRes= 0;
      PreparedStatement lStmt= null;
      ResultSet lCurs= null;

      try
      {
         lStmt= makeGetStatement(aKey, "COUNT(1)");
         lCurs= lStmt.executeQuery();

         lCurs.next();
         lRes= lCurs.getInt(1);
      }
      catch(final Exception e)
      {
      }

      try
      {
         lCurs.close();
      }
      catch(final Exception e)
      {
      }

      try
      {
         lStmt.close();
      }
      catch(final Exception e)
      {
      }

      return lRes;
   }

   /**
    * @see IConnection#count(Class)
    */
   public int count(final Class<? extends DbObject> aDbClass)
   {
      int lRes= 0;
      Statement lStmt= null;
      ResultSet lCurs= null;

      try
      {
         final String lQuery= "SELECT COUNT(1) FROM " + aDbClass.newInstance().getDbTable();

         if( mDebug )
            System.out.println(String.format("[%05d]: QUERY= %s", mConnId, lQuery));

         lStmt= mConn.createStatement();
         lCurs= lStmt.executeQuery(lQuery);

         if( mDebug )
         {
            final ResultSetMetaData iMeta= lCurs.getMetaData();
            final List<String> iRes= new ArrayList<String>();
            final int iCnt= iMeta.getColumnCount();

            for(int fI= 1; fI <= iCnt; fI++)
               iRes.add(iMeta.getColumnName(fI));

            System.out.println(String.format("[%05d]:    COLUMNS= %s", mConnId, iRes));
         }

         lCurs.next();
         lRes= lCurs.getInt(1);
      }
      catch(final Exception e)
      {
      }

      try
      {
         lCurs.close();
      }
      catch(final Exception e)
      {
      }

      try
      {
         lStmt.close();
      }
      catch(final Exception e)
      {
      }

      return lRes;
   }

   /**
    * @see IConnection#query(U)
    */
   public <U extends IObject> List<U> query(final U aKey)
   {
      return query(aKey, 0, -1);
   }

   /**
    * @see IConnection#query(Class, IObject)
    */
   public <U extends IResult> List<U> query(final Class<U> aResClass, final IObject aKey)
   {
      return query(aResClass, aKey, 0, -1);
   }

   /**
    * @see IConnection#query(Class)
    */
   public <U extends DbObject> List<U> query(final Class<U> aDbClass)
   {
      return query(aDbClass, 0, -1);
   }

   /**
    * @see IConnection#query(IObject, int, int)
    */
   public <U extends IObject> List<U> query(final U aKey, final int aOffset, final int aCount)
   {
      @SuppressWarnings("unchecked")
      final Class<U> lClass= (Class<U>) aKey.getClass();
      DbObject lObj= null;

      if( Proxy.isProxyClass(aKey.getClass()) )
         lObj= EntityHandler.class.cast(Proxy.getInvocationHandler(aKey)).get();
      else if( aKey instanceof DbObject )
         lObj= DbObject.class.cast(aKey);

      List<U> lRes;

      if( lObj != null )
      {
         final List<U> iRes= new ConvertedList<U>(lClass, fetch(lObj, aOffset, aCount));

         lRes= iRes;
      }
      else
         lRes= Collections.emptyList();

      return lRes;
   }

   /**
    * @see IConnection#query(Class, int, int)
    */
   public <U extends IResult> List<U> query(final Class<U> aResClass, final IObject aKey, final int aOffset,
         final int aCount)
   {
      DbObject lObj= null;

      if( Proxy.isProxyClass(aKey.getClass()) )
         lObj= EntityHandler.class.cast(Proxy.getInvocationHandler(aKey)).get();
      else if( aKey instanceof DbObject )
         lObj= DbObject.class.cast(aKey);

      List<U> lRes;

      if( lObj != null )
      {
         final List<U> iRes= fetch(aResClass, lObj, aOffset, aCount);

         lRes= iRes;
      }
      else
         lRes= Collections.emptyList();

      return lRes;
   }

   /**
    * @param aKey
    * @param aOffset
    * @param aCount
    * @return
    */
   private <U extends DbObject> List<U> fetch(final U aKey, final int aOffset, final int aCount)
   {
      @SuppressWarnings("unchecked")
      final Class<U> lClass= (Class<U>) aKey.getClass();
      final List<U> lRes= new ArrayList<U>();
      final int lMax= (aCount > 0) ? (aOffset + aCount) : 0;
      PreparedStatement lStmt= null;
      ResultSet lCurs= null;

      try
      {
         lStmt= makeGetStatement(aKey, "*");

         try
         {
            lStmt.setMaxRows(lMax);
         }
         catch(final SQLException e)
         {
         }

         lCurs= lStmt.executeQuery();
         lCurs= populate(lCurs, lRes, new DbObjectFactory<U>(lClass), aOffset, aCount);
      }
      catch(final Exception eEx)
      {
         eEx.printStackTrace();
      }

      try
      {
         lCurs.close();
      }
      catch(final Exception e)
      {
      }

      try
      {
         lStmt.close();
      }
      catch(final Exception e)
      {
      }

      return lRes;
   }

   /**
    * @param aResClass
    * @param aKey
    * @param aOffset
    * @param aCount
    * @return
    */
   private <U extends IResult> List<U> fetch(final Class<U> aResClass, final DbObject aKey, final int aOffset, final int aCount)
   {
      final DbResultFactory<U> lFactory= new DbResultFactory<U>(aResClass);
      final List<U> lRes= new ArrayList<U>();
      final int lMax= (aCount > 0) ? (aOffset + aCount) : 0;
      PreparedStatement lStmt= null;
      ResultSet lCurs= null;

      try
      {
         lStmt= makeGetStatement(aKey, lFactory.getColumns());

         try
         {
            lStmt.setMaxRows(lMax);
         }
         catch(final SQLException e)
         {
         }

         lCurs= lStmt.executeQuery();
         lCurs= populate(lCurs, lRes, lFactory, aOffset, aCount);
      }
      catch(final Exception eEx)
      {
         eEx.printStackTrace();
      }

      try
      {
         lCurs.close();
      }
      catch(final Exception e)
      {
      }

      try
      {
         lStmt.close();
      }
      catch(final Exception e)
      {
      }

      return lRes;
   }

   /**
    * @see IConnection#query(Class, int, int)
    */
   public <U extends DbObject> List<U> query(final Class<U> aDbClass, final int aOffset, final int aCount)
   {
      final List<U> lRes= new ArrayList<U>();
      final int lMax= (aCount > 0) ? (aOffset + aCount) : 0;
      Statement lStmt= null;
      ResultSet lCurs= null;

      try
      {
         final String lQuery= "SELECT * FROM " + aDbClass.newInstance().getDbTable();

         if( mDebug )
            System.out.println(String.format("[%05d]: QUERY= %s", mConnId, lQuery));

         lStmt= mConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

         try
         {
            lStmt.setMaxRows(lMax);
         }
         catch(final SQLException e)
         {
         }

         lCurs= lStmt.executeQuery(lQuery);
         lCurs= populate(lCurs, lRes, new DbObjectFactory<U>(aDbClass), aOffset, aCount);
      }
      catch(final Exception eEx)
      {
         eEx.printStackTrace();
      }

      try
      {
         lCurs.close();
      }
      catch(final Exception e)
      {
      }

      try
      {
         lStmt.close();
      }
      catch(final Exception e)
      {
      }

      return lRes;
   }

   /**
    * @see IConnection#save(IObject...)
    */
   public void save(final IObject... aDatas) throws DbException
   {
      save(Arrays.asList(aDatas));
   }

   /**
    * @see IConnection#save(Collection)
    */
   public void save(final Collection<? extends IObject> aDatas) throws DbException
   {
      final List<DbObject> lDatas= new ArrayList<DbObject>();

      for(final IObject fObj : aDatas)
      {
         if( fObj == null )
            continue;

         DbObject wObj= null;

         if( Proxy.isProxyClass(fObj.getClass()) )
            wObj= EntityHandler.class.cast(Proxy.getInvocationHandler(fObj)).get();
         else if( fObj instanceof DbObject )
            wObj= DbObject.class.cast(fObj);

         if( wObj != null )
            lDatas.add(wObj);
      }

      saveData(lDatas);
   }

   /**
    * @param aDatas
    * @throws DbException
    */
   private void saveData(final List<? extends DbObject> aDatas)
      throws DbException
   {
      final Map<String, TableDesc> lDescs= new HashMap<String, TableDesc>();
      Savepoint lSavepoint= null;
      int lI= 0;

      for(final DbObject fObj : aDatas)
      {
         lI++;
         if( fObj.isReadOnly() )
            throw new DbException(String.format("DB object #%d is read-only (%s)", lI, fObj.getClass().getName()));
      }

      if( (!mTransactions) && mSavepoints )
         try
         {
            lSavepoint= mConn.setSavepoint();
         }
         catch(final SQLException e)
         {
         }

      try
      {
         lI= 0;
         for(final DbObject fObj : aDatas)
            lI= saveData(lI, lDescs, fObj);

         if( lSavepoint != null )
            try
            {
               mConn.releaseSavepoint(lSavepoint);
            }
            catch(final SQLException e)
            {
            }
         else if( mTransactions )
            try
            {
               mConn.commit();
            }
            catch(final SQLException e)
            {
            }
      }
      catch(final SQLException eS)
      {
         if( lSavepoint != null )
            try
            {
               mConn.rollback(lSavepoint);
            }
            catch(final SQLException e)
            {
            }
         else if( mTransactions )
            try
            {
               mConn.rollback();
            }
            catch(final SQLException e)
            {
            }

         throw new DbException("SQL Error", eS);
      }
      finally
      {
         for(final TableDesc fDesc : lDescs.values())
            try
            {
               fDesc.mIdentifier.close();
            }
            catch(final Exception eEx)
            {
            }
      }
   }

   /**
    * @param aI
    * @param aDescs
    * @param aObj
    * @return
    * @throws SQLException
    */
   private int saveData(final int aI, final Map<String, TableDesc> aDescs, final DbObject aObj)
      throws SQLException
   {
      final AtomicInteger lI= new AtomicInteger(aI);
      final String lName= aObj.getDbTable();
      final Map<String, ParamDesc> lValues= new SortedValueMap<String, ParamDesc>();
      final AtomicLong lId= new AtomicLong(-1L);
      final AtomicBoolean lDel= new AtomicBoolean();

      aObj.updateData(new IDataSetter()
      {
         private int mIdx = 1;

         public void delete()
         {
            lDel.set(true);
         }

         public void setId(final Long aId)
         {
            try
            {
               lId.set(aId);
            }
            catch(final NullPointerException eNp)
            {
            }
         }

         public void set(final String aColumnName, final Object aValue, final boolean aIsNull, final DbOperation aOp)
            throws SQLException
         {
            if( aIsNull )
            {
               final int iIdx= mIdx++;

               lValues.put(aColumnName, new ParamDesc(iIdx, aColumnName, new ValueDesc(iIdx, aOp, null)));
            }
            else if( aValue != null )
            {
               final int iIdx= mIdx++;

               if( aValue instanceof IObject )
               {
                  DbObject iValue= null;

                  if( Proxy.isProxyClass(aValue.getClass()) )
                     iValue= EntityHandler.class.cast(Proxy.getInvocationHandler(aValue)).get();
                  else if( aValue instanceof DbObject )
                     iValue= DbObject.class.cast(aValue);

                  if( (iValue != null) && (iValue.getId() == null) )
                     lI.set(saveData(lI.get(), aDescs, iValue));
               }

               lValues.put(aColumnName, new ParamDesc(iIdx, aColumnName, new ValueDesc(iIdx, aOp, aValue)));
            }
         }

         public void listGroupBy(final List<String> aGroupBy)
         {
         }

         public void listOrderBy(final List<String> aOrderBy)
         {
         }
      });

      final boolean lInsert= lId.get() < 0;
      final boolean lDelete= lDel.get();
      final String lTableName= DbConnection.makeTableId(lName, lInsert, lDelete);
      TableDesc lDesc= aDescs.get(lTableName);

      if( lDesc == null )
      {
         String iQuery= "";
         int iFlag= Statement.NO_GENERATED_KEYS;

         if( lDelete )
         {
            if( lInsert )
               iQuery= "DELETE FROM " + lName + DbConnection.makeWhere(lValues.values());
            else
               iQuery= "DELETE FROM " + lName + " WHERE id = ?";
         }
         else if( lInsert )
         {
            String iCols= "";
            String iVals= "";

            for(final ParamDesc fDesc : lValues.values())
            {
               iCols+= ", " + fDesc.mIdentifier;
               iVals+= ", ?";
            }

            if( iCols.length() > 0 )
               iCols= iCols.substring(2);

            if( iVals.length() > 0 )
               iVals= iVals.substring(2);

            iQuery= "INSERT INTO " + lName + "(" + iCols + ") VALUES(" + iVals + ")";
            iFlag= Statement.RETURN_GENERATED_KEYS;
         }
         else
         {
            String iVals= "";

            for(final ParamDesc fDesc : lValues.values())
               iVals+= ", " + fDesc.mIdentifier + "= ?";

            if( iVals.length() > 0 )
               iVals= iVals.substring(2);
            else
               iVals= "id= id";

            iQuery= "UPDATE " + lName + " SET " + iVals + " WHERE id = ?";
         }

         if( mDebug )
            System.out.println(String.format("[%05d]: QUERY= %s  (ID=%d)", mConnId, iQuery, aObj.getId()));

         lDesc= new TableDesc(lI.getAndDecrement(), mConn.prepareStatement(iQuery, iFlag), lValues);
         aDescs.put(lTableName, lDesc);
      }
      else
         lDesc.mIdentifier.clearParameters();

      if( (!lDelete) || lInsert )
      {
         final ParameterMetaData iMetas= lDesc.mIdentifier.getParameterMetaData();
         final String[] iParamVals= new String[lValues.size()];

         for(final ParamDesc fDesc : lValues.values())
         {
            boolean wIsValue= true;
            final int wIndex= lDesc.mValue.get(fDesc.mIdentifier).mIndex;
            Object wValue= fDesc.mValue.mValue;

            try
            {
               wValue= IObject.class.cast(wValue).getId();
               wIsValue= false;
            }
            catch(final Exception eEx)
            {
            }

            if( wIsValue )
            {
               try
               {
                  final Class<?> wType= Class.forName(iMetas.getParameterClassName(wIndex));

                  if( Date.class.isAssignableFrom(wType) )
                     wValue= wType.getConstructor(long.class).newInstance(Date.class.cast(wValue).getTime());
                  else
                  {
                     final Class<?> iClass= wValue.getClass();

                     if( Character.class.isAssignableFrom(iClass) || char.class.isAssignableFrom(iClass) )
                        wValue= wValue.toString();
                  }
               }
               catch(final Exception eEx)
               {
               }
            }

            lDesc.mIdentifier.setObject(wIndex, wValue);

            if( mDebug )
               iParamVals[fDesc.mIndex - 1]= String.format("<%s>", wValue);
         }

         if( mDebug )
            System.out.println(String.format("[%05d]:    VALUES= %s", mConnId, Arrays.asList(iParamVals)));
      }

      if( !lInsert )
      {
         lDesc.mIdentifier.setLong((lDelete) ? 1 : (lDesc.mValue.size() + 1), lId.get());
         if( mDebug )
            System.out.println(String.format("[%05d]:    ID= %d", mConnId, lId.get()));
      }

      if( (lDesc.mIdentifier.executeUpdate() == 1) && lInsert && (!lDelete) )
      {
         final ResultSet iRes= lDesc.mIdentifier.getGeneratedKeys();

         try
         {
            if( iRes.next() )
            {
               final long iId= iRes.getLong(1);

               if( mDebug )
                  System.out.println(String.format("[%05d]:    ID INSERTED= %d", mConnId, iId));

               aObj.importData(new IDataGetter()
               {
                  @Override
                  public Object get(final String aColumnName, final Class<? extends IObject> aModel, final Class<? extends DbObject> aEntity)
                     throws SQLException
                  {
                     if( !aColumnName.equals("id") )
                        throw new IdFilterException();

                     return iId;
                  }
               });
            }
         }
         finally
         {
            iRes.close();
         }
      }

      return lI.get();
   }

   /**
    * @see IConnection#close()
    */
   public void close()
   {
      try
      {
         mConn.close();
      }
      catch(final Exception eEx)
      {
      }
   }
}

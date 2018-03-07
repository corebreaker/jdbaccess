/**
 *
 */
package com.corebreaker.dbaccess;

import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public abstract class DbObject implements Comparable<DbObject>
{
   private static class FieldSpecifier
   {
      private final Field               mField;
      private Class<? extends IObject>  mInterface = null;
      private Class<? extends DbObject> mEntity    = null;

      @SuppressWarnings("unchecked")
      FieldSpecifier(final Field aField)
      {
         super();

         mField= aField;

         final Class<?> lType= mField.getType();

         if( IObject.class.isAssignableFrom(lType) )
         {
            mInterface= (Class<? extends IObject>) lType;

            final Class<? extends DbObject> iEntity= mField.getAnnotation(NField.class).entity();

            if( iEntity == null || DbObject.class.equals(iEntity) )
            {
               try
               {
                  mEntity= (Class<? extends DbObject>) Class.forName(mInterface.getName().replace(".model.I",
                        ".entity."));
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
      Field get()
      {
         return mField;
      }

      /**
       * @return
       */
      String getDbName()
      {
         String lRes= mField.getAnnotation(NField.class).value();

         if( lRes.length() == 0 )
            lRes= mField.getName().substring(1).toLowerCase();

         return lRes;
      }

      /**
       * Renvoi une exception si le champ n'est pas annoté
       */
      void check()
      {
         getDbName();
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(final Object aObj)
      {
         try
         {
            return FieldSpecifier.class.cast(aObj).mField.getName().equals(mField.getName());
         }
         catch(final ClassCastException eCc)
         {
            return aObj.toString().equals(mField.getName());
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
         return mField.getName().hashCode();
      }

      /**
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString()
      {
         return mField.getName();
      }
   }

   private static class TableSpecifier implements Comparable<TableSpecifier>, Iterable<FieldSpecifier>
   {
      private final String                      mName;
      private final boolean                     mReadOnly;
      private final Map<String, FieldSpecifier> mFields = new HashMap<String, FieldSpecifier>();

      TableSpecifier(final Class<?> aClass)
      {
         super();

         String lName= "";
         boolean lReadOnly= true;
         final NEntity lEntity= aClass.getAnnotation(NEntity.class);

         try
         {
            lName= lEntity.value();
            lReadOnly= lEntity.readOnly();
         }
         catch(final NullPointerException eNp)
         {
         }

         if( lName.length() == 0 )
            lName= aClass.getSimpleName().toUpperCase();

         for(final Field fField : aClass.getDeclaredFields())
         {
            final FieldSpecifier wSpec= new FieldSpecifier(fField);

            try
            {
               wSpec.check();

               fField.setAccessible(true);
               mFields.put(fField.getName(), wSpec);
            }
            catch(final Exception eEx)
            {
            }
         }

         mName= lName;
         mReadOnly= lReadOnly;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(final Object aObj)
      {
         try
         {
            return TableSpecifier.class.cast(aObj).mName.equals(mName);
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
       * @return
       */
      boolean isReadOnly()
      {
         return mReadOnly;
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
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      public int compareTo(final TableSpecifier aTable)
      {
         return mName.compareTo(aTable.mName);
      }

      /**
       * @param aFieldName
       * @return
       */
      FieldSpecifier getField(final String aFieldName)
      {
         return mFields.get(aFieldName);
      }

      /**
       * @see java.lang.Iterable#iterator()
       */
      public Iterator<FieldSpecifier> iterator()
      {
         return mFields.values().iterator();
      }
   }

   private final static Map<Class<?>, TableSpecifier> DB_STRUCTS = new HashMap<Class<?>, TableSpecifier>();

   private Long                                       mId        = null;
   private boolean                                    mDelete    = false;
   private final TableSpecifier                       mSpec;
   private final Set<String>                          mNulls     = new HashSet<String>();
   private final Map<String, DbOperation>             mOps       = new HashMap<String, DbOperation>();
   private final List<String>                         mGroupBy   = new LinkedList<String>();
   private final List<String>                         mOrderBy   = new LinkedList<String>();

   /**
    *
    */
   protected DbObject()
   {
      super();

      final Class<?> lClass= getClass();

      TableSpecifier lSpec= DbObject.DB_STRUCTS.get(lClass);

      if( lSpec == null )
      {
         lSpec= new TableSpecifier(lClass);
         DbObject.DB_STRUCTS.put(lClass, lSpec);
      }

      mSpec= lSpec;
   }

   /**
    * @param aId
    */
   protected DbObject(final long aId)
   {
      this();

      mId= aId;
   }

   /**
    * @return
    */
   final String getDbTable()
   {
      return mSpec.toString();
   }

   /**
    * @return
    */
   final boolean isReadOnly()
   {
      return mSpec.isReadOnly();
   }

   /**
    * @param aData
    * @throws SQLException
    */
   final void updateData(final IDataSetter aData)
      throws SQLException
   {
      SQLException lEx= null;

      aData.setId(mId);

      if( mDelete )
         aData.delete();
      else
      {
         aData.listOrderBy(Collections.unmodifiableList(mOrderBy)); mOrderBy.clear();
         aData.listGroupBy(Collections.unmodifiableList(mGroupBy)); mGroupBy.clear();
      }

      for(final FieldSpecifier fField : mSpec)
         try
         {
            try
            {
               final Field wField= fField.get();
               final String wName= wField.getName();

               aData.set(fField.getDbName(), wField.get(this), mNulls.contains(wName), mOps.get(wName));
            }
            catch(final SQLException eSql)
            {
               lEx= eSql;
               break;
            }
         }
         catch(final Exception e)
         {
         }

      if( lEx != null )
         throw lEx;
   }

   /**
    * @param aData
    */
   final void importData(final IDataGetter aData)
   {
      try
      {
         mId= Long.class.cast(aData.get("id", null, null));

         for(final FieldSpecifier fField : mSpec)
            try
            {
               try
               {
                  Object wObj= aData.get(fField.getDbName(), fField.getInterface(), fField.getEntity());
                  final Class<?> wType= fField.get().getType();

                  if( (wObj != null) && wType.equals(String.class) )
                     wObj= wObj.toString();
                  else if( wType.equals(char.class) || ((wObj != null) && wType.equals(Character.class)) )
                     wObj= String.valueOf(wObj).charAt(0);
                  else if( (wObj != null) && (wObj instanceof Integer) )
                  {
                     final Integer iObj= Integer.class.cast(wObj);

                     if( wType.equals(short.class) || wType.equals(Short.class) )
                        wObj= iObj.shortValue();
                     else if( wType.equals(byte.class) || wType.equals(Byte.class) )
                        wObj= iObj.byteValue();
                  }
                  else if( (wObj != null) && (wObj instanceof Blob) )
                  {
                     final Blob iBlob= Blob.class.cast(wObj);

                     wObj= iBlob.getBytes(1L, (int)iBlob.length());
                  }

                  fField.get().set(this, wObj);
               }
               catch(final IdFilterException eEx)
               {
               }
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

      mNulls.clear();
      mOps.clear();
      mDelete= false;
   }

   /**
    * @return
    */
   public Long getId()
   {
      return mId;
   }

   /**
    * @param aFieldName
    */
   public DbObject setNull(final String aFieldName)
   {
      mNulls.add(aFieldName);

      return this;
   }

   /**
    * @param aFieldName
    */
   public DbObject addGroupBy(final String aFieldName)
   {
      mGroupBy.add(mSpec.getField(aFieldName).getDbName());

      return this;
   }

   /**
    * @param aFieldName
    */
   public DbObject addOrderBy(final String aFieldName)
   {
      addOrderBy(aFieldName, false);

      return this;
   }

   /**
    * @param aFieldName
    * @param aDesc
    */
   public DbObject addOrderBy(final String aFieldName, final boolean aDesc)
   {
      mOrderBy.add(mSpec.getField(aFieldName).getDbName() + (( aDesc ) ? " DESC" : " ASC"));

      return this;
   }

   /**
    * @param aFieldName
    * @param aOp
    */
   public DbObject setOperation(final String aFieldName, final DbOperation aOp)
   {
      mOps.put(aFieldName, aOp);

      return this;
   }

   /**
    *
    */
   public void delete()
   {
      delete(true);
   }

   /**
    * @param aDeleteFlag
    */
   public void delete(final boolean aDeleteFlag)
   {
      mDelete= aDeleteFlag;
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public final int hashCode()
   {
      return (mId == null) ? super.hashCode() : mId.hashCode();
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public final boolean equals(final Object aObj)
   {
      try
      {
         final DbObject lObj= DbObject.class.cast(aObj);

         return lObj.mSpec.equals(this) && lObj.mId.equals(mId);
      }
      catch(final NullPointerException eNp)
      {
         return super.equals(aObj);
      }
      catch(final ClassCastException e)
      {
         return false;
      }
   }

   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   @Override
   public final int compareTo(final DbObject aObj)
   {
      try
      {
         final int lCmp= mSpec.compareTo(aObj.mSpec);

         if( lCmp == 0 )
         {
            Long iVal1= new Long(hashCode());
            Long iVal2= new Long(aObj.hashCode());

            if( mId != null )
               iVal1= mId;

            if( aObj.mId != null )
               iVal2= aObj.mId;

            return iVal1.compareTo(iVal2);
         }
         else
            return lCmp;
      }
      catch(final NullPointerException e)
      {
         return -1;
      }
   }
}

package com.corebreaker.dbaccess;

import java.util.Collection;
import java.util.List;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public interface IConnection
{
   /**
    * @param aKey
    * @return
    */
   int count(IObject aKey);

   /**
    * @param aDbClass
    * @return
    */
   int count(Class<? extends DbObject> aDbClass);

   /**
    * @param aId
    * @param aDbClass
    * @return
    * @throws DbException
    */
   <U extends DbObject> U query(long aId, Class<U> aDbClass) throws DbException;

   /**
    * @param aKey
    * @return
    */
   <U extends IObject> List<U> query(U aKey);

   /**
    * @param aResClass
    * @param aKey
    * @return
    */
   <U extends IResult> List<U> query(Class<U> aResClass, IObject aKey);

   /**
    * @param aDbClass
    * @return
    */
   <U extends DbObject> List<U> query(Class<U> aDbClass);

   /**
    * @param aKey
    * @param aOffset
    * @param aCount
    * @return
    */
   <U extends IObject> List<U> query(U aKey, int aOffset, int aCount);

   /**
    * @param aResClass
    * @param aKey
    * @param aOffset
    * @param aCount
    * @return
    */
   <U extends IResult> List<U> query(Class<U> aResClass, IObject aKey, int aOffset, int aCount);

   /**
    * @param aDbClass
    * @param aOffset
    * @param aCount
    * @return
    */
   <U extends DbObject> List<U> query(Class<U> aDbClass, int aOffset, int aCount);

   /**
    * @param aDatas
    * @throws DbException
    */
   void save(IObject... aDatas) throws DbException;

   /**
    * @param aData
    * @throws DbException
    */
   void save(Collection<? extends IObject> aDatas) throws DbException;

   /**
    *
    */
   void close();
}

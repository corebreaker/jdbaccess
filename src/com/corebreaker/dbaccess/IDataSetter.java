/**
 *
 */
package com.corebreaker.dbaccess;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
interface IDataSetter
{
   void delete();
   void listGroupBy(List<String> aGroupBy);
   void listOrderBy(List<String> aOrderBy);
   void setId(Long aId) throws SQLException;
   void set(String aColumnName, Object aValue, boolean aIsNull, DbOperation aOp) throws SQLException;
}

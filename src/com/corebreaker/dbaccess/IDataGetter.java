/**
 *
 */
package com.corebreaker.dbaccess;

import java.sql.SQLException;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */

interface IDataGetter
{
   Object get(String aColumnName, Class<? extends IObject> aModel, Class<? extends DbObject> aEntity) throws SQLException;
}

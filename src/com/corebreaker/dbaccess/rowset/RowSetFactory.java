package com.corebreaker.dbaccess.rowset;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.sql.rowset.*;
import java.sql.SQLException;

public class RowSetFactory implements javax.sql.rowset.RowSetFactory
{
    private final static String CACHED_ROWSET = "com.sun.rowset.CachedRowSetImpl";
    private final static String FILTERED_ROWSET = "com.sun.rowset.FilteredRowSetImpl";
    private final static String JDBC_ROWSET = "com.sun.rowset.JdbcRowSetImpl";
    private final static String JOIN_ROWSET = "com.sun.rowset.JoinRowSetImpl";
    private final static String WEB_ROWSET = "com.sun.rowset.WebRowSetImpl";

    private static Class<CachedRowSet> sCachedClass = importClass(CACHED_ROWSET, CachedRowSet.class);
    private static Class<FilteredRowSet> sFilteredClass = importClass(FILTERED_ROWSET, FilteredRowSet.class);
    private static Class<JdbcRowSet> sJdbcClass = importClass(JDBC_ROWSET, JdbcRowSet.class);
    private static Class<JoinRowSet> sJoinClass = importClass(JOIN_ROWSET, JoinRowSet.class);
    private static Class<WebRowSet> sWebClass = importClass(WEB_ROWSET, WebRowSet.class);

    private static <T> Class<T> convert(Class<?> from, Class<T> to)
    {
        return to.getClass().cast(from);
    }

    private static <T> Class<T> importClass(final String name, final Class<T> dest) throws RuntimeException
    {
        try
        {
            return convert(Class.forName(name), dest);
        }
        catch(final ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Creates a new instance of a CachedRowSet.</p>
     *
     * @return A new instance of a CachedRowSet.
     * @throws SQLException if a CachedRowSet cannot
     *                      be created.
     * @since 1.7
     */
    @Override
    public CachedRowSet createCachedRowSet() throws SQLException {
        return new CachedRowSetImpl();
    }

    /**
     * <p>Creates a new instance of a FilteredRowSet.</p>
     *
     * @return A new instance of a FilteredRowSet.
     * @throws SQLException if a FilteredRowSet cannot
     *                      be created.
     * @since 1.7
     */
    @Override
    public FilteredRowSet createFilteredRowSet() throws SQLException {
        throw new NotImplementedException();
    }

    /**
     * <p>Creates a new instance of a JdbcRowSet.</p>
     *
     * @return A new instance of a JdbcRowSet.
     * @throws SQLException if a JdbcRowSet cannot
     *                      be created.
     * @since 1.7
     */
    @Override
    public JdbcRowSet createJdbcRowSet() throws SQLException {
        throw new NotImplementedException();
    }

    /**
     * <p>Creates a new instance of a JoinRowSet.</p>
     *
     * @return A new instance of a JoinRowSet.
     * @throws SQLException if a JoinRowSet cannot
     *                      be created.
     * @since 1.7
     */
    @Override
    public JoinRowSet createJoinRowSet() throws SQLException {
        throw new NotImplementedException();
    }

    /**
     * <p>Creates a new instance of a WebRowSet.</p>
     *
     * @return A new instance of a WebRowSet.
     * @throws SQLException if a WebRowSet cannot
     *                      be created.
     * @since 1.7
     */
    @Override
    public WebRowSet createWebRowSet() throws SQLException {
        throw new NotImplementedException();
    }
}

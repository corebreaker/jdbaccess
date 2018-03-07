package com.corebreaker.dbaccess.rowset;

import javax.sql.rowset.*;
import java.sql.SQLException;

public class RowSetFactory implements javax.sql.rowset.RowSetFactory
{
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }
}

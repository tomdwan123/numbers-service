/*
 * Copyright (c) Message4U Pty Ltd 2014-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.messagemedia.numbers.repository.config;

import com.mchange.v2.c3p0.C3P0ProxyConnection;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.springframework.core.GenericTypeResolver;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public abstract class PostgresEnumSetType<T extends Enum<T>> implements UserType {

    private static final int[] SQL_TYPES = {Types.ARRAY};
    private final Class<T> enumType;
    private final String pgEnumTypeName;

    /**
     * @param pgEnumTypeName : name of enum type of postgres
     */
    @SuppressWarnings("unchecked")
    public PostgresEnumSetType(String pgEnumTypeName) {
        this.enumType = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(),
                PostgresEnumSetType.class);
        this.pgEnumTypeName = pgEnumTypeName;
    }

    @Override
    public Object assemble(final Serializable cached, final Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object deepCopy(final Object obj) throws HibernateException {
        return obj;
    }

    @Override
    public Serializable disassemble(final Object obj) throws HibernateException {
        return (Serializable) obj;
    }

    @Override
    public boolean equals(final Object x, final Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(final Object obj) throws HibernateException {
        return Objects.hashCode(obj);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        Array array = rs.getArray(names[0]);
        if (array == null) {
            return null;
        }
        String[] javaArray = (String[]) array.getArray();
        return Arrays.stream(javaArray).map(p -> Enum.valueOf(enumType, p)).collect(toSet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.ARRAY);
        } else {
            String[] strArr = ((Set<T>) value).stream().map(p -> p.toString()).toArray(String[]::new);
            try {
                Method method = Connection.class.getMethod("createArrayOf", new Class<?>[]{String.class, Object[].class});
                Array array = (Array) ((C3P0ProxyConnection) st.getConnection()).rawConnectionOperation(
                        method, C3P0ProxyConnection.RAW_CONNECTION, new Object[]{pgEnumTypeName, strArr});
                st.setArray(index, array);
            } catch (Exception e) {
                throw new HibernateException(e);
            }
        }
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
        return original;
    }

    @Override
    public Class<?> returnedClass() {
        return Array.class;
    }

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }
}

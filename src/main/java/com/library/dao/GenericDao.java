package com.library.dao;

import com.library.exception.DatabaseOperationException;
import com.library.exception.DuplicateEntityException;
import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD data access object interface.
 *
 * @param <T>  the entity type
 * @param <ID> the primary key identifier type
 */
public interface GenericDao<T, ID> {

    Optional<T> findById(ID id) throws DatabaseOperationException;

    List<T> findAll() throws DatabaseOperationException;

    T save(T entity) throws DatabaseOperationException, DuplicateEntityException;

    boolean update(T entity) throws DatabaseOperationException, DuplicateEntityException;

    boolean deleteById(ID id) throws DatabaseOperationException;
}

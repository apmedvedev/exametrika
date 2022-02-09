/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.common.rawdb.IRawDataFile;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.RawPageNotFoundException;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IFileField} represents a file-based field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFileField extends IField {
    /**
     * Returns data file of this field.
     *
     * @return data file of this field
     */
    IRawDataFile getFile();

    /**
     * Returns page.
     *
     * @param pageIndex page index
     * @return page
     * @throws RawPageNotFoundException when page does not exist and transaction is read-only
     */
    IRawPage getPage(long pageIndex);
}

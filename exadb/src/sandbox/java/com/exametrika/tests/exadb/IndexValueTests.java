/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;


/**
 * The {@link IndexValueTests} are tests for index value implementations.
 *
 * @author Medvedev-A
 */
public class IndexValueTests {
//    private RawDatabase database;
//    private RawDatabaseConfiguration parameters;
//    private IndexSchemaConfiguration configuration;
//    private IndexManager indexManager;
//    
//    @Before
//    public void setUp()
//    {
//        configuration = new IndexSchemaConfiguration("test", 1);
//        indexManager = new IndexManager(new SystemTimeService(), new TestDataFileAllocator(), new TestTransactionProvider(), 
//            new IndexProviderConfiguration(0, 0, 0, 0));
//        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
//        Files.emptyDir(tempDir);
//        RawDatabaseConfigurationBuilder builder = new RawDatabaseConfigurationBuilder();
//        parameters = builder
//            .addPath(new File(tempDir, "dir1").getPath())
//            .addPath(new File(tempDir, "dir2").getPath())
//            .setFlushPeriod(1000)
//            .setResourceAllocator(new RootResourceAllocatorConfigurationBuilder(10000000).toConfiguration())
//            .addPageType("normal", Constants.PAGE_SIZE)
//                .getDefaultPageCategory()
//                    .setMinPageCachePercentage(90)
//                    .setMaxPageIdlePeriod(10000)
//                .end()
//            .end()
//            .toConfiguration();
//
//        database = new RawDatabaseFactory().createDatabase(parameters);
//        database.start();
//    }
//    
//    @After
//    public void tearDown()
//    {
//        IOs.close(database);
//    }
//    
//    @Test
//    public void testTreeIndexValues() throws Throwable
//    {
//        testIndexValues(new ITestIndexFactory()
//        {
//            @Override
//            public IndexValueSpace create(IRawTransaction transaction, boolean open)
//            {
//                if (!open)
//                    return TreeIndexValueSpace.<String>create(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        Indexes.createStringKeyNormalizer(), true);
//                else
//                    return TreeIndexValueSpace.<String>open(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        Indexes.createStringKeyNormalizer());
//            }
//        });
//    }
//    
//    @Test
//    public void testHashIndexValues() throws Throwable
//    {
//        testIndexValues(new ITestIndexFactory()
//        {
//            @Override
//            public IndexValueSpace create(IRawTransaction transaction, boolean open)
//            {
//                if (!open)
//                    return HashIndexValueSpace.<String>create(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        Indexes.createStringKeyNormalizer());
//                else
//                    return HashIndexValueSpace.<String>open(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        Indexes.createStringKeyNormalizer());
//            }
//        });
//    }
//    
//    @Test
//    public void testBTreeIndexValues() throws Throwable
//    {
//        testIndexValues(new ITestIndexFactory()
//        {
//            @Override
//            public IndexValueSpace create(IRawTransaction transaction, boolean open)
//            {
//                if (!open)
//                    return BTreeIndexValueSpace.<String>create(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        false, 16, 64, Indexes.createStringKeyNormalizer(), true);
//                else
//                    return BTreeIndexValueSpace.<String>open(indexManager, configuration,
//                        new TestTransactionProvider(transaction), 1, "btree",  
//                        false, 16, 64, Indexes.createStringKeyNormalizer());
//            }
//        });
//    }
//    
//    @Test
//    public void testTreeIndexValueDeletion() throws Throwable
//    {
//        testIndexValueDeletion(new ITestIndexFactory()
//        {
//            @Override
//            public IndexValueSpace create(IRawTransaction transaction, boolean open)
//            {
//                if (!open)
//                    return TreeIndexValueSpace.<String>create(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        Indexes.createStringKeyNormalizer(), true);
//                else
//                    return TreeIndexValueSpace.<String>open(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        Indexes.createStringKeyNormalizer());
//            }
//        });
//    }
//    
//    @Test
//    public void testHashIndexValueDeletion() throws Throwable
//    {
//        testIndexValueDeletion(new ITestIndexFactory()
//        {
//            @Override
//            public IndexValueSpace create(IRawTransaction transaction, boolean open)
//            {
//                if (!open)
//                    return HashIndexValueSpace.<String>create(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        Indexes.createStringKeyNormalizer());
//                else
//                    return HashIndexValueSpace.<String>open(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        Indexes.createStringKeyNormalizer());
//            }
//        });
//    }
//    
//    @Test
//    public void testBTreeIndexValueDeletion() throws Throwable
//    {
//        testIndexValueDeletion(new ITestIndexFactory()
//        {
//            @Override
//            public IndexValueSpace create(IRawTransaction transaction, boolean open)
//            {
//                if (!open)
//                    return BTreeIndexValueSpace.<String>create(indexManager, configuration,
//                        new TestTransactionProvider(transaction), new TestDataFileAllocator(10), 1, "btree",  
//                        false, 16, 64, Indexes.createStringKeyNormalizer(), true);
//                else
//                    return BTreeIndexValueSpace.<String>open(indexManager, configuration,
//                        new TestTransactionProvider(transaction), 1, "btree",  
//                        false, 16, 64, Indexes.createStringKeyNormalizer());
//            }
//        });
//    }
//    
//    private void testIndexValues(final ITestIndexFactory factory) throws Throwable
//    {
//        final long[] areaBlockIndex = new long[2];
//        final int[] areaOffset = new int[2];
//        database.transactionSync(new RawOperation()
//        {
//            @Override
//            public void run(IRawTransaction transaction)
//            {
//                IndexValueSpace<String> space = factory.create(transaction, false);
//                
//                space.getIndex().add("key11", space.createValue(32));
//                space.getIndex().add("key21", space.createValue(64));
//                space.getIndex().add("key22", space.createValue(48));
//                space.getIndex().add("key31", space.createValue(32));
//                space.getIndex().add("key32", space.createValue(64));
//                space.getIndex().add("key33", space.createValue(32));
//                
//                ((AbstractIndexSpace)space.getIndex()).assertValid();
//                
//                IIndexValue value11 = space.find("key11", false);
//                IIndexValue value21 = space.find("key21", false);
//                IIndexValue value22 = space.find("key22", false);
//                IIndexValue value31 = space.find("key31", false);
//                IIndexValue value32 = space.find("key32", false);
//                IIndexValue value33 = space.find("key33", false);
//                        
//                try
//                {
//                    IIndexValueSerialization serialization11 = value11.createSerialization();
//                    IIndexValueSerialization serialization21 = value21.createSerialization();
//                    IIndexValueSerialization serialization22 = value22.createSerialization();
//                    IIndexValueSerialization serialization31 = value31.createSerialization();
//                    IIndexValueSerialization serialization32 = value32.createSerialization();
//                    IIndexValueSerialization serialization33 = value33.createSerialization();
//                    
//                    ((AbstractIndexSpace)space.getIndex()).assertValid();
//                    
//                    for (int i = 0; i < 10000; i++)
//                    {
//                        if (i == 5000)
//                        {
//                            areaBlockIndex[0] = serialization11.getAreaId();
//                            areaOffset[0] = serialization11.getAreaOffset();
//                        }
//                        serialization11.writeInt(i);
//                        serialization21.writeInt(i);
//                        serialization22.writeInt(i);
//                        serialization31.writeInt(i);
//                        serialization32.writeInt(i);
//                        serialization33.writeInt(i);
//                        
//                        ((AbstractIndexSpace)space.getIndex()).assertValid();
//                    }
//                    
//                    ((AbstractIndexSpace)space.getIndex()).assertValid();
//                    
//                    areaBlockIndex[1] = serialization11.getAreaId();
//                    areaOffset[1] = serialization11.getAreaOffset();
//                    serialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
//                    serialization11.writeInt(5000);
//                    serialization11.setPosition(areaBlockIndex[1], areaOffset[1]);
//                    
//                    ByteArray buf = createBuffer(23456, 123);
//                    String str = createString(34567, 9876);
//                    
//                    serialization11.writeByteArray(buf);
//                    serialization21.writeByteArray(buf);
//                    serialization22.writeByteArray(buf);
//                    serialization31.writeByteArray(buf);
//                    serialization32.writeByteArray(buf);
//                    serialization33.writeByteArray(buf);
//                    
//                    serialization11.writeString(str);
//                    serialization21.writeString(str);
//                    serialization22.writeString(str);
//                    serialization31.writeString(str);
//                    serialization32.writeString(str);
//                    serialization33.writeString(str);
//                    
//                    IIndexValueDeserialization deserialization11 = value11.createDeserialization();
//                    IIndexValueDeserialization deserialization21 = value21.createDeserialization();
//                    IIndexValueDeserialization deserialization22 = value22.createDeserialization();
//                    IIndexValueDeserialization deserialization31 = value31.createDeserialization();
//                    IIndexValueDeserialization deserialization32 = value32.createDeserialization();
//                    IIndexValueDeserialization deserialization33 = value33.createDeserialization();
//            
//                    for (int i = 0; i < 10000; i++)
//                    {
//                        assertThat(deserialization33.readInt(), is(i));
//                        assertThat(deserialization32.readInt(), is(i));
//                        assertThat(deserialization31.readInt(), is(i));
//                        assertThat(deserialization22.readInt(), is(i));
//                        assertThat(deserialization21.readInt(), is(i));
//                        assertThat(deserialization11.readInt(), is(i));
//                    }
//                    
//                    assertThat(deserialization33.readByteArray(), is(buf));
//                    assertThat(deserialization33.readString(), is(str));
//                    assertThat(deserialization32.readByteArray(), is(buf));
//                    assertThat(deserialization32.readString(), is(str));
//                    assertThat(deserialization31.readByteArray(), is(buf));
//                    assertThat(deserialization31.readString(), is(str));
//                    assertThat(deserialization22.readByteArray(), is(buf));
//                    assertThat(deserialization22.readString(), is(str));
//                    assertThat(deserialization21.readByteArray(), is(buf));
//                    assertThat(deserialization21.readString(), is(str));
//                    assertThat(deserialization11.readByteArray(), is(buf));
//                    assertThat(deserialization11.readString(), is(str));
//                    
//                    areaBlockIndex[1] = deserialization11.getAreaId();
//                    areaOffset[1] = deserialization11.getAreaOffset();
//                    
//                    deserialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
//                    assertThat(deserialization11.readInt(), is(5000));
//                    
//                    deserialization11.setPosition(areaBlockIndex[1], areaOffset[1]);
//                    final IIndexValueDeserialization d1 = deserialization11;
//                    new Expected(EndOfStreamException.class, new Runnable()
//                    {
//                        @Override
//                        public void run()
//                        {
//                            for (int i = 0; i < 100; i++)
//                                assertThat(d1.readInt(), is(0));
//                        }
//                    });
//                }
//                catch (Throwable e)
//                {
//                    Exceptions.wrapAndThrow(e);
//                }
//                
//                ((AbstractIndexSpace)space.getIndex()).onTransactionCommitted();
//            }
//        });
//
//        database.stop();
//        
//        database = new RawDatabaseFactory().createDatabase(parameters);
//        database.start();
//        
//        database.transactionSync(new RawOperation(true)
//        {
//            @Override
//            public void run(IRawTransaction transaction)
//            {
//                IndexValueSpace<String> space = factory.create(transaction, true);
//                
//                IIndexValue value11 = space.find("key11", true);
//                IIndexValue value21 = space.find("key21", true);
//                IIndexValue value22 = space.find("key22", true);
//                IIndexValue value31 = space.find("key31", true);
//                IIndexValue value32 = space.find("key32", true);
//                IIndexValue value33 = space.find("key33", true);
//                
//                try
//                {
//                    IIndexValueDeserialization deserialization11 = value11.createDeserialization();
//                    IIndexValueDeserialization deserialization21 = value21.createDeserialization();
//                    IIndexValueDeserialization deserialization22 = value22.createDeserialization();
//                    IIndexValueDeserialization deserialization31 = value31.createDeserialization();
//                    IIndexValueDeserialization deserialization32 = value32.createDeserialization();
//                    IIndexValueDeserialization deserialization33 = value33.createDeserialization();
//                    
//                    for (int i = 0; i < 10000; i++)
//                    {
//                        assertThat(deserialization11.readInt(), is(i));
//                        assertThat(deserialization21.readInt(), is(i));
//                        assertThat(deserialization22.readInt(), is(i));
//                        assertThat(deserialization31.readInt(), is(i));
//                        assertThat(deserialization32.readInt(), is(i));
//                        assertThat(deserialization33.readInt(), is(i));
//                    }
//                    
//                    ByteArray buf = createBuffer(23456, 123);
//                    String str = createString(34567, 9876);
//                    
//                    assertThat(deserialization33.readByteArray(), is(buf));
//                    assertThat(deserialization33.readString(), is(str));
//                    assertThat(deserialization32.readByteArray(), is(buf));
//                    assertThat(deserialization32.readString(), is(str));
//                    assertThat(deserialization31.readByteArray(), is(buf));
//                    assertThat(deserialization31.readString(), is(str));
//                    assertThat(deserialization22.readByteArray(), is(buf));
//                    assertThat(deserialization22.readString(), is(str));
//                    assertThat(deserialization21.readByteArray(), is(buf));
//                    assertThat(deserialization21.readString(), is(str));
//                    assertThat(deserialization11.readByteArray(), is(buf));
//                    assertThat(deserialization11.readString(), is(str));
//                    
//                    deserialization11.setPosition(areaBlockIndex[0], areaOffset[0]);
//                    assertThat(deserialization11.readInt(), is(5000));
//                    
//                    deserialization11.setPosition(areaBlockIndex[1], areaOffset[1]);
//                    final IIndexValueDeserialization d2 = deserialization11;
//                    new Expected(EndOfStreamException.class, new Runnable()
//                    {
//                        @Override
//                        public void run()
//                        {
//                            for (int i = 0; i < 100; i++)
//                                assertThat(d2.readInt(), is(0));
//                        }
//                    });
//                }
//                catch (Throwable e)
//                {
//                    Exceptions.wrapAndThrow(e);
//                }
//            }
//        });
//    }
//    
//    private void testIndexValueDeletion(final ITestIndexFactory factory) throws Throwable
//    {
//        final int COUNT = 1000;
//        final long[] n = new long[1];
//        database.transactionSync(new RawOperation()
//        {
//            @Override
//            public void run(IRawTransaction transaction)
//            {
//                IndexValueSpace<String> space = factory.create(transaction, false);
//                
//                try
//                {
//                    for (int i = 0; i < COUNT; i++)
//                    {
//                        ByteArray buf = createBuffer(200, i);
//
//                        space.getIndex().add("key" + i, space.createValue(64));
//                        IIndexValue value11 = space.find("key" + i, false);
//                        IIndexValueSerialization serialization = value11.createSerialization();
//                        serialization.writeByteArray(buf);
//                    }
//                    
//                    n[0] = space.allocateBlocks(1);
//                    
//                    for (int i = 0; i < COUNT; i++)
//                    {
//                        ByteArray buf = createBuffer(200, i);
//                        
//                        IIndexValue value11 = space.find("key" + i, false);
//                        IIndexValueDeserialization deserialization = value11.createDeserialization();
//                        assertThat(deserialization.readByteArray(), is(buf));
//                        
//                        IIndexValueSerialization serialization = value11.createSerialization();
//                        buf = createBuffer(100, i + 1);
//                        serialization.writeByteArray(buf);
//                        serialization.removeRest();
//                    }
//                    
//                    assertThat(space.allocateBlocks(1), is(n[0] + 1));
//                    
//                    for (int i = 0; i < COUNT; i++)
//                    {
//                        ByteArray buf = createBuffer(100, i + 1);
//                        
//                        IIndexValue value11 = space.getIndex().find("key" + i);
//                        final IIndexValueDeserialization deserialization = value11.createDeserialization();
//                        assertThat(deserialization.readByteArray(), is(buf));
//                        
//                        new Expected(EndOfStreamException.class, new Runnable()
//                        {
//                            @Override
//                            public void run()
//                            {
//                                for (int i = 0; i < 100; i++)
//                                    deserialization.readLong();
//                            }
//                        });
//                    }
//                    
//                    assertThat(space.allocateBlocks(1), is(n[0] + 2));
//                    
//                    for (int i = 0; i < COUNT; i++)
//                        space.getIndex().remove("key" + i);
//                }
//                catch (Throwable e)
//                {
//                    Exceptions.wrapAndThrow(e);
//                }
//                
//                ((AbstractIndexSpace)space.getIndex()).onTransactionCommitted();
//            }
//        });
//        
//        database.stop();
//        
//        database = new RawDatabaseFactory().createDatabase(parameters);
//        database.start();
//        
//        database.transactionSync(new RawOperation()
//        {
//            @Override
//            public void run(IRawTransaction transaction)
//            {
//                IndexValueSpace<String> space = factory.create(transaction, true);
//                
//                for (int i = 0; i < COUNT; i++)
//                {
//                    ByteArray buf = createBuffer(200, i);
//
//                    space.getIndex().add("key" + i, space.createValue(64));
//                    IIndexValue value11 = space.find("key" + i, false);
//                    IIndexValueSerialization serialization = value11.createSerialization();
//                    assertThat(serialization.readLong(), is(0l));
//                    assertThat(serialization.readLong(), is(0l));
//                    assertThat(serialization.readLong(), is(0l));
//                    serialization = value11.createSerialization();
//                    serialization.writeLong(0);
//                    serialization.writeLong(0);
//                    serialization.writeLong(0);
//                    serialization.writeLong(0);
//                    serialization.writeLong(0);
//                    
//                    serialization = value11.createSerialization();
//                    serialization.writeByteArray(buf);
//                }
//        
//                assertThat(space.allocateBlocks(1), is(n[0]));
//                
//                space.find("key1", false).createSerialization().removeRest();
//                
//                space.getIndex().add("key", space.createValue(32));
//                IIndexValue value = space.find("key", false);
//                IIndexValueSerialization serialization = value.createSerialization();
//                serialization.writeByteArray(new ByteArray(new byte[Constants.COMPLEX_FIELD_AREA_DATA_SIZE]));
//                
//                IIndexValue value1 = space.find("key0", false);
//                IIndexValueSerialization serialization1 = value1.createSerialization();
//                serialization1.removeRest();
//                
//                IIndexValue value2 = space.find("key" + (COUNT - 1), false);
//                IIndexValueSerialization serialization2 = value2.createSerialization();
//                serialization2.removeRest();
//                
//                serialization1.writeByteArray(new ByteArray(new byte[Constants.COMPLEX_FIELD_AREA_DATA_SIZE]));
//                serialization2.writeByteArray(new ByteArray(new byte[Constants.COMPLEX_FIELD_AREA_DATA_SIZE]));
//            }
//        });
//    }
//    
//    private ByteArray createBuffer(int size, int base)
//    {
//        byte[] b = new byte[size];
//        for (int i = 0; i < size; i++)
//            b[i] = (byte)(i + base);
//        
//        return new ByteArray(b);
//    }
//    
//    private String createString(int size, int base)
//    {
//        char[] b = new char[size];
//        for (int i = 0; i < size; i++)
//            b[i] = (char)(i + base);
//        
//        return new String(b);
//    }
//    
//    public interface ITestIndexFactory
//    {
//        <T extends IndexValueSpace> T create(IRawTransaction transaction, boolean open);
//    }
}

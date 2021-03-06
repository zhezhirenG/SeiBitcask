package file.cache;

import sun.nio.ch.FileChannelImpl;
import util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BucketBuffer {

    private BucketBuffer(){}

    public static BucketBuffer newInstance(){
        return new BucketBuffer();
    }

    //<editor-fold desc="local var">
    private volatile MappedByteBuffer bucketBuffer;

    private volatile MappedByteBuffer simpleBuffer;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock writeLock = lock.writeLock();

    private final Lock readLock = lock.readLock();
    //</editor-fold>

    //<editor-fold desc="mappedByteBuffer operation">
    public MappedByteBuffer getBucketBuffer(File file, int size){
        if(bucketBuffer == null){
            writeLock.lock();
            try{
                if (bucketBuffer == null) {
                    try(RandomAccessFile raf = new RandomAccessFile(file, "rw")){
                        bucketBuffer = raf
                                .getChannel()
                                .map(FileChannel.MapMode.READ_WRITE, 0, size);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            finally {
                writeLock.unlock();
            }
        }
        return bucketBuffer;
    }

    private MappedByteBuffer getSimpleBuffer() {
        if (simpleBuffer == null) {
            writeLock.lock();
            try {
                if (simpleBuffer == null) {
                    try(RandomAccessFile raf = new RandomAccessFile(FileUtil.getSimpleBufferPath().toFile(), "rw")){
                        simpleBuffer = raf
                                .getChannel()
                                .map(FileChannel.MapMode.READ_WRITE, 0, Integer.BYTES);
                        simpleBuffer.putInt(0, 0);
                        simpleBuffer.force();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            finally {
                writeLock.unlock();
            }
        }
        return simpleBuffer;
    }

    public void closeBucketBuffer(){
        if(bucketBuffer != null){
            writeLock.lock();
            try{
                if(bucketBuffer != null){
                    releaseBuffer(bucketBuffer);
                    bucketBuffer = null;
                }
            }
            finally {
                writeLock.unlock();
            }
        }
    }

    public void closeSimpleBuffer(){
        if(simpleBuffer != null){
            writeLock.lock();
            try{
                if(simpleBuffer != null){
                    releaseBuffer(simpleBuffer);
                    simpleBuffer = null;
                }
            }
            finally {
                writeLock.unlock();
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="getter and setter">
    public int getActiveBucketId(){
        int res;
        getSimpleBuffer();
        readLock.lock();
        try {
            res = simpleBuffer.getInt(0);
        } finally {
            readLock.unlock();
        }
        return res;
    }

    public void setActiveBucketId(int id){
        writeLock.lock();
        try {
            getSimpleBuffer().putInt(0, id);
            getSimpleBuffer().force();
        } finally {
            writeLock.unlock();
        }
    }

    public int idIncrementAndGet(){
        int id;
        writeLock.lock();
        try {
            id = getActiveBucketId();
            getSimpleBuffer().putInt(0, ++id);
            getSimpleBuffer().force();
            return id;
        } finally {
            writeLock.unlock();
        }
    }
    //</editor-fold>

    //<editor-fold desc="private static methods">
    private static void releaseBuffer(MappedByteBuffer buffer){
        Method m;
        try {
            m = FileChannelImpl.class.getDeclaredMethod("unmap", MappedByteBuffer.class);
            m.setAccessible(true);
            m.invoke(FileChannelImpl.class, buffer);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }
    //</editor-fold>


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeBucketBuffer();
        closeSimpleBuffer();
    }
}

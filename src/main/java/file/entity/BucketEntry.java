package file.entity;

import util.ConvertUtil;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author sei
 * @description Bucket存储的条目实体类，建造者模式
 * @date 17:13 2020/3/28
 */
public class BucketEntry {
    /**
     * 时间戳
     */
    private final long tstamp;
    /**
     * 键大小
     */
    private final int keySize;
    /**
     * 值大小
     */
    private final int valueSize;
    /**
     * 键
     */
    private final byte[] key;
    /**
     * 值
     */
    private final byte[] value;

    private BucketEntry(BucketEntryBuilder builder){
        tstamp = builder.tstamp;
        keySize = builder.keySize;
        valueSize = builder.valueSize;
        key = builder.key;
        value = builder.value;
    }

    public static class BucketEntryBuilder {
        private final long tstamp;

        private int keySize;

        private int valueSize;

        private byte[] key;

        private byte[] value;

        public BucketEntryBuilder(){
            tstamp = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
        }

        public BucketEntryBuilder setKey(byte[] key) {
            this.key = key;
            this.keySize = key.length;
            return this;
        }

        public BucketEntryBuilder setValue(byte[] value) {
            this.value = value;
            this.valueSize = value.length;
            return this;
        }

        public BucketEntry build(){
            return new BucketEntry(this);
        }
    }

    public static BucketEntryBuilder builder(){
        return new BucketEntryBuilder();
    }

    public long getTstamp() {
        return tstamp;
    }

    public int getKeySize() {
        return keySize;
    }

    public int getValueSize() {
        return valueSize;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    public int size(){
        return 8 + 4 + 4 + this.getKeySize() + this.getValueSize();
    }

    public byte[] toBytes(){
        byte[] result = new byte[size()];
        int copyOffset = 0;

        byte[] tstampBytes = ConvertUtil.long2Bytes(tstamp);
        System.arraycopy(tstampBytes, 0, result, copyOffset, 8);
        copyOffset += 8;

        byte[] keySizeBytes = ConvertUtil.int2Bytes(keySize);
        System.arraycopy(keySizeBytes, 0, result, copyOffset, 4);
        copyOffset += 4;

        byte[] valueSizeBytes = ConvertUtil.int2Bytes(valueSize);
        System.arraycopy(valueSizeBytes, 0, result, copyOffset, 4);
        copyOffset += 4;

        System.arraycopy(key, 0, result, copyOffset, keySize);
        copyOffset += keySize;

        System.arraycopy(value, 0, result, copyOffset, valueSize);

        return result;
    }
}
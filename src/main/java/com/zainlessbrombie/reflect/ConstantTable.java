package com.zainlessbrombie.reflect;

import com.zainlessbrombie.mc.crossclass.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by mathis on 03.03.18 12:33.
 */
public class ConstantTable {


    private ConstantTable() {}
    private List<Constant> constants;
    private byte[] original;
    private int tableEndIndex; //exclusive




    public static ConstantTable readFrom(byte[] wholeClass) {
        ConstantTable ret = new ConstantTable();
        ret.original = wholeClass;
        short numberOfConstants = (short) (0x100 * (wholeClass[8] & 0xff) + (wholeClass[9] & 0xff));

        List<Constant> constants = new ArrayList<>(numberOfConstants);
        int ptr = 10;
        for(int i = 0; i < numberOfConstants - 1; i++) {
            Constant read = Constant.readConstantAtPos(ptr, wholeClass, ret);
            read.setTableIndex(i + 1);
            ptr += read.totalLength();
            constants.add(read);
            if(read instanceof Constant.LongConstant || read instanceof Constant.DoubleConstant) // they take up two spaces in the pool.
                i++;
        }
        ret.tableEndIndex = ptr;
        ret.constants = constants;

        return ret;
    }


    public List<Constant> getConstants() {
        return constants;
    }

    @SuppressWarnings("unchecked")
    public <T extends Constant> Stream<T> getConstantsOfType(Class<T> c) {
        return constants.stream().filter(constant -> c.isAssignableFrom(constant.getClass())).map(o -> (T)o);
    }

    public byte[] recompile() {
        List<byte[]> constantsCode = new ArrayList<>(constants.size());
        for (Constant constant : constants) {
            constantsCode.add(constant.reAssemble());
        }
        int constantsLength = constantsCode.stream().mapToInt(b -> b.length).sum();
        byte[] ret = new byte[original.length + (constantsLength - (tableEndIndex - 10))];
        // 4 byte CAFEBABE + 2 bytes minVer + 2 bytes maxVer + 2 bytes content size
        System.arraycopy(original,0,ret,0,10);
        int tableSize = constantsCode.size() + 1;
        tableSize += constants.stream() //they count double
                .filter(o -> o instanceof Constant.DoubleConstant || o instanceof Constant.LongConstant)
                .count();
        ret[8] = (byte) (tableSize >> 8);
        ret[9] = (byte) (tableSize);
        int ptr = 10;
        for(byte[] constantCode : constantsCode) {
            System.arraycopy(constantCode,0,ret,ptr,constantCode.length);
            ptr += constantCode.length;
        }
        System.arraycopy(original,tableEndIndex,ret,ptr,original.length - tableEndIndex);
        return ret;
    }



}

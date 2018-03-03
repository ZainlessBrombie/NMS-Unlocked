package com.zainlessbrombie.reflect;

import com.zainlessbrombie.mc.crossclass.Main;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by mathis on 03.03.18 14:03.
 */
public abstract class Constant {

    private static ConstantConverter[] converters = new ConstantConverter[19];

    static { //init both placeholders and method calls
        ConstantConverter error = (table1, raw1, startPos1) -> {throw new UnsupportedOperationException("Type "+raw1[startPos1]+" on pos "+startPos1+" not supported!");};
        for (int i = 0; i < converters.length; i++) {
            converters[i] = error;
        }
        for (Class<?> c : Constant.class.getClasses()) {
            try {
                Method m = c.getMethod("initClass");
                m.invoke(null); // static block activation. todo rewrite to init converters directly
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
    }

    protected ConstantTable table;
    protected byte type;
    protected byte[] raw;
    protected int startPos;
    protected int tableIndex = -1;

    private Constant(ConstantTable table,byte[] raw, int startPos) {
        this.table = table;
        this.raw = raw;
        this.startPos = startPos;
    }

    public byte getType() {
        return type;
    }

    /**
     * Get the table index, the lowest index being one.
     */
    public int getTableIndex() {
        return tableIndex;
    }

    void setTableIndex(int tableIndex) {
        this.tableIndex = tableIndex;
    }

    public abstract int totalLength();

    public abstract int contentLength();

    public abstract byte[] reAssemble();

    public static Constant readConstantAtPos(int pos, byte[] raw, ConstantTable table) {
        return converters[raw[pos]].compile(table, raw, pos);
    }


    public static class Utf8Constant extends Constant {

        private int length; //whole length - 3

        static {
            converters[1] = Utf8Constant::new;
        }

        public Utf8Constant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
            if(raw[startPos] != 1)
                throw new IllegalArgumentException(raw[startPos]+" is not a utf constant");
            type = raw[startPos];
            length = (raw[startPos + 1] & 0xff) * 0x100 + (raw[startPos + 2] & 0xff);
        }

        @Override
        public int totalLength() {
            return length + 3;
        }

        @Override
        public int contentLength() {
            return length;
        }

        public byte[] getContent() {
            byte[] ret = new byte[length];
            System.arraycopy(raw,startPos + 3,ret,0,length);
            return ret;
        }

        /**
         * Set the byte array content, of this constant. Does not include the tag and length information.
         */
        public void setContent(byte[] newContent) {
            raw = new byte[newContent.length + 3];
            raw[0] = type;
            raw[1] = (byte) (newContent.length >> 8);
            raw[2] = (byte) newContent.length;
            System.arraycopy(newContent,0,raw,3,newContent.length);
            startPos = 0;
            length = newContent.length;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[length + 3];
            System.arraycopy(raw,startPos,ret,0,length + 3);
            return ret;
        }

        public static void initClass() {

        }

    }


    public static class IntegerConstant extends Constant {

        static {
            converters[3] = IntegerConstant::new;
        }

        public IntegerConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 5;
        }

        @Override
        public int contentLength() {
            return 4;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[5];
            System.arraycopy(raw,startPos,ret,0,5);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class FloatConstant extends Constant {

        static {
            converters[4] = FloatConstant::new;
        }

        public FloatConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 5;
        }

        @Override
        public int contentLength() {
            return 4;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[5];
            System.arraycopy(raw,startPos,ret,0,5);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class LongConstant extends Constant {

        static {
            converters[5] = LongConstant::new;
        }

        public LongConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 9;
        }

        @Override
        public int contentLength() {
            return 8;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[9];
            System.arraycopy(raw,startPos,ret,0,9);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class DoubleConstant extends Constant {

        static {
            converters[6] = DoubleConstant::new;
        }

        public DoubleConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 9;
        }

        @Override
        public int contentLength() {
            return 8;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[9];
            System.arraycopy(raw,startPos,ret,0,9);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class ClassConstant extends Constant {

        static {
            converters[7] = ClassConstant::new;
        }

        public ClassConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 3;
        }

        @Override
        public int contentLength() {
            return 2;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[3];
            System.arraycopy(raw,startPos,ret,0,3);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class StringConstant extends Constant {

        static {
            converters[8] = StringConstant::new;
        }

        public StringConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 3;
        }

        @Override
        public int contentLength() {
            return 2;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[3];
            System.arraycopy(raw,startPos,ret,0,3);
            return ret;
        }

        /**
         * The reference id as encoded in the two bytes. Minimum is 1, meaning you can get the corresponding entry from the list with .get(getReferenceId() - 1)
         */
        public int getReferenceId() {
            return (raw[startPos + 1] & 0xff) * 0x100 + (raw[startPos + 2] & 0xff);
        }

        public String getReferencedContent() {
            try {
                Constant c = table.getConstants().get(getReferenceId() - 1);
                if(!(c instanceof Utf8Constant))
                    throw new RuntimeException("Expected constant with id "+getReferenceId()+" to be UTF8Constant, but got "+c);
                return new String(((Utf8Constant) c).getContent());
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException("Data integrity error: "+getReferenceId()+" not in table of length "+table.getConstants().parallelStream());
            }
        }

        public static void initClass() {

        }
    }

    public static class FieldRefConstant extends Constant {

        static {
            converters[9] = FieldRefConstant::new;
        }

        public FieldRefConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 5;
        }

        @Override
        public int contentLength() {
            return 4;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[5];
            System.arraycopy(raw,startPos,ret,0,5);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class MethodRefConstant extends Constant {

        static {
            converters[10] = MethodRefConstant::new;
        }

        public MethodRefConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 5;
        }

        @Override
        public int contentLength() {
            return 4;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[5];
            System.arraycopy(raw,startPos,ret,0,5);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class InterfaceMethodRefConstant extends Constant {

        static {
            converters[11] = InterfaceMethodRefConstant::new;
        }

        public InterfaceMethodRefConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 5;
        }

        @Override
        public int contentLength() {
            return 4;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[5];
            System.arraycopy(raw,startPos,ret,0,5);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class NameAndTypeConstant extends Constant {

        static {
            converters[12] = NameAndTypeConstant::new;
        }

        public NameAndTypeConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 5;
        }

        @Override
        public int contentLength() {
            return 4;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[5];
            System.arraycopy(raw,startPos,ret,0,5);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class MethodHandleConstant extends Constant {

        static {
            converters[15] = MethodHandleConstant::new;
        }

        public MethodHandleConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 4;
        }

        @Override
        public int contentLength() {
            return 3;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[4];
            System.arraycopy(raw,startPos,ret,0,4);
            return ret;
        }

        public static void initClass() {

        }
    }

    public static class MethodTypeConstant extends Constant {

        static {
            converters[16] = MethodTypeConstant::new;
        }

        public MethodTypeConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 3;
        }

        @Override
        public int contentLength() {
            return 2;
        }

        @Override
        public byte[] reAssemble() {
            return new byte[]{raw[startPos],raw[startPos+1],raw[startPos+2]}; //todo find out at what point arraycopy is cheaper
        }

        public static void initClass() {

        }
    }

    public static class InvokeDynamicConstant extends Constant {

        static {
            converters[18] = InvokeDynamicConstant::new;
        }

        public InvokeDynamicConstant(ConstantTable table, byte[] raw, int startPos) {
            super(table, raw, startPos);
        }

        @Override
        public int totalLength() {
            return 5;
        }

        @Override
        public int contentLength() {
            return 4;
        }

        @Override
        public byte[] reAssemble() {
            byte[] ret = new byte[5];
            System.arraycopy(raw,startPos,ret,0,5);
            return ret;
        }

        public static void initClass() {

        }
    }

    private interface ConstantConverter {
        Constant compile(ConstantTable table, byte[] raw, int startPos); //startPos being the tag index
    }
}

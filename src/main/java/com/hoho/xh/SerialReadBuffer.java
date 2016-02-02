package com.hoho.xh;

/**
 * Created by pc on 2016/1/30.
 * 缓冲接受到的数据，直到收到“回车”（结束）
 */
public class SerialReadBuffer {
    private StringBuffer readBuffer = new StringBuffer();
    private int counter = 0;

    public void append(byte[] inputByte) {
        for (byte b : inputByte) {
            if (b == 0x0A) {//回车
                counter++;
            }
            readBuffer.append((char) b);
        }
    }

    public int size() {
        return counter;
    }

    public String read() {
        int index = readBuffer.indexOf("\n");
        if (index != -1) {
            String s = readBuffer.substring(0, index);
            readBuffer.delete(0, index + 1);
            counter--;
            return s;
        } else return null;
    }
    public void clean(){
        readBuffer.setLength(0);
        counter=0;
    }
}

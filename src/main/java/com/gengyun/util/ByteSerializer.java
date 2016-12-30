package com.gengyun.util;

import java.io.*;

/**
 * <类详细说明：序列化和反序列化方法>
 *
 * @Author： Huanghai
 * @Version: 2016-09-29
 **/
public class ByteSerializer {
    public static byte[] serialize(Object o) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        byte [] buf = baos.toByteArray();
        oos.flush();
        return buf;
    }

    public static Object deserialize(byte[] bytes) throws IOException,ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object obj = ois.readObject();
        return obj;
    }
}

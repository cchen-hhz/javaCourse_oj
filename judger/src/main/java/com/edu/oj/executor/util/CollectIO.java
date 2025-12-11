package com.edu.oj.executor.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CollectIO {
    public static Thread collectIO(Process p, ByteArrayOutputStream outBuf, ByteArrayOutputStream errBuf) {
        Thread t = new Thread(() -> {
            try (InputStream out = p.getInputStream();
                 InputStream err = p.getErrorStream()) {
                out.transferTo(outBuf);
                err.transferTo(errBuf);
            } catch (IOException ignored) {}
        });
        t.start();
        return t;
    }
}

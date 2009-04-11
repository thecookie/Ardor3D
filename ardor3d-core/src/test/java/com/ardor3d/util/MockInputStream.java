/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.io.InputStream;
import java.io.IOException;

public class MockInputStream extends InputStream  {
    private int bytesAvailable = 0;
    private boolean eof = false;
    

    public int read() throws IOException {
        while (true) {
            int result = returnSomething();

            if (result != 0) {
                return result;
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized int returnSomething() {
        if (eof) {
            return -1;
        }

        if (bytesAvailable > 0) {
            bytesAvailable--;
            return 1;
        }
        
        return 0;
    }

    @Override
    public synchronized int available() throws IOException {
        return bytesAvailable;
    }

    public synchronized void addBytesAvailable(int bytesAvailable) {
        this.bytesAvailable += bytesAvailable;
    }

    public synchronized void setEof(boolean eof) {
        this.eof = eof;
    }
}
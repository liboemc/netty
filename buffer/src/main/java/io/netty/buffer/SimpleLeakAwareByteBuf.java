/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.buffer;

import io.netty.util.ResourceLeak;
import io.netty.util.ResourceLeakDetector;

import java.nio.ByteOrder;

final class SimpleLeakAwareByteBuf extends WrappedByteBuf {

    private final ResourceLeak leak;

    SimpleLeakAwareByteBuf(ByteBuf buf, ResourceLeak leak) {
        super(buf);
        assert !(buf instanceof AbstractPooledDerivedByteBuf);
        this.leak = leak;
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        // Call unwrap() before just in case that super.release() will change the ByteBuf instance that is returned
        // by ByteBuf.
        ByteBuf unwrapped = unwrap();
        if (super.release()) {
            boolean closed = ResourceLeakDetector.close(leak, unwrapped);
            assert closed;
            return true;
        }
        return false;
    }

    @Override
    public boolean release(int decrement) {
        // Call unwrap() before just in case that super.release() will change the ByteBuf instance that is returned
        // by ByteBuf.
        ByteBuf unwrapped = unwrap();
        if (super.release(decrement)) {
            boolean closed = ResourceLeakDetector.close(leak, unwrapped);
            assert closed;
            return true;
        }
        return false;
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        leak.record();
        if (order() == endianness) {
            return this;
        } else {
            return new SimpleLeakAwareByteBuf(super.order(endianness), leak);
        }
    }

    @Override
    public ByteBuf slice() {
        return new SimpleLeakAwareByteBuf(super.slice(), leak);
    }

    // These methods that retain as well are not delegating to their super methods. This is because keeping
    // track of the ResourceLeak is very tricky as AbstractPooledDerivedByteBuf implementations also need to keep
    // track of their parent to release it. The problem is that instances of AbstractPooledDerivedByteBuf have no
    // idea about our wrapping and will not release the ResourceLeak directly as "this" is captured.
    //
    // Wrapping the buffers creates some overhead anyway its considered the easiest and safest way to just
    // not make use of AbstractPooleDerivedByteBuf when leak detection for the buffer is ongoing.
    @Override
    public ByteBuf retainedSlice() {
        return slice().retain();
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {
        return slice(index, length).retain();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return duplicate().retain();
    }

    @Override
    public ByteBuf readRetainedSlice(int length) {
        return readSlice(length).retain();
    }

    // The above comment not apply to the following methods anymore.

    @Override
    public ByteBuf slice(int index, int length) {
        return new SimpleLeakAwareByteBuf(super.slice(index, length), leak);
    }

    @Override
    public ByteBuf duplicate() {
        return new SimpleLeakAwareByteBuf(super.duplicate(), leak);
    }

    @Override
    public ByteBuf readSlice(int length) {
        return new SimpleLeakAwareByteBuf(super.readSlice(length), leak);
    }

    @Override
    public ByteBuf asReadOnly() {
        return new SimpleLeakAwareByteBuf(super.asReadOnly(), leak);
    }
}

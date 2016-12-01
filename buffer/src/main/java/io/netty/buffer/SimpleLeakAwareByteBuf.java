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

    @Override
    public ByteBuf retainedSlice() {
        return LeakAwareByteBufUtil.unwrappedDerived(this, leak, super.retainedSlice());
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {
        return LeakAwareByteBufUtil.unwrappedDerived(this, leak, super.retainedSlice(index, length));
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return LeakAwareByteBufUtil.unwrappedDerived(this, leak, super.retainedDuplicate());
    }

    @Override
    public ByteBuf readRetainedSlice(int length) {
        return LeakAwareByteBufUtil.unwrappedDerived(this, leak, super.readRetainedSlice(length));
    }

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

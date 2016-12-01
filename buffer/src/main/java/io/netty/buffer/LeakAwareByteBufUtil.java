/*
 * Copyright 2016 The Netty Project
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

final class LeakAwareByteBufUtil {

    private LeakAwareByteBufUtil() { }

    static ByteBuf unwrappedDerived(ByteBuf leakAware, ResourceLeak parentLeak, ByteBuf derived) {
        ByteBuf unwrappedDerived = unwrapSwapped(derived);

        if (unwrappedDerived instanceof AbstractPooledDerivedByteBuf) {
            ResourceLeak leak = AbstractByteBuf.leakDetector.open(derived);
            if (leak == null) {
                // No leak detection, just return the derived buffer.
                return derived;
            }
            ByteBuf leakAwareBuf = leakAware instanceof SimpleLeakAwareByteBuf ?
                    new SimpleLeakAwareByteBuf(derived, leak) : new AdvancedLeakAwareByteBuf(derived, leak);
            ((AbstractPooledDerivedByteBuf) unwrappedDerived).parent(leakAware);
            return leakAwareBuf;
        }
        return leakAware instanceof SimpleLeakAwareByteBuf ?
                new SimpleLeakAwareByteBuf(derived, parentLeak) : new AdvancedLeakAwareByteBuf(derived, parentLeak);
    }

    @SuppressWarnings("deprecation")
    private static ByteBuf unwrapSwapped(ByteBuf buf) {
        if (buf instanceof SwappedByteBuf) {
            do {
                // TODO: Just use unwrap() once https://github.com/netty/netty/pull/6081 is fixed.
                buf = ((SwappedByteBuf) buf).buf;
            } while (buf instanceof SwappedByteBuf);

            return buf;
        }
        return buf;
    }
}

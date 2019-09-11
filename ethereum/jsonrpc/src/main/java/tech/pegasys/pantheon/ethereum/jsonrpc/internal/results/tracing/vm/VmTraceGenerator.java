/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.vm;

import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.debug.TraceFrame;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTrace;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.Trace;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.bytes.BytesValues;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class VmTraceGenerator {

  public static Stream<Trace> generateTraceStream(final TransactionTrace transactionTrace) {
    return Stream.of(generateTrace(transactionTrace));
  }

  public static Trace generateTrace(final TransactionTrace transactionTrace) {
    final VmTrace vmTrace = new VmTrace();
    if (transactionTrace != null && !transactionTrace.getTraceFrames().isEmpty()) {
      // TODO: set smart contract code
      transactionTrace
          .getTransaction()
          .getInit()
          .map(BytesValue::getHexString)
          .ifPresent(vmTrace::setCode);
      final AtomicInteger index = new AtomicInteger(0);
      transactionTrace
          .getTraceFrames()
          .forEach(traceFrame -> addFrame(index, transactionTrace, vmTrace, traceFrame));
    }
    return vmTrace;
  }

  private static void addFrame(
      final AtomicInteger index,
      final TransactionTrace transactionTrace,
      final VmTrace vmTrace,
      final TraceFrame traceFrame) {
    if ("STOP".equals(traceFrame.getOpcode())) {
      return;
    }
    traceFrame.getMaybeCode().ifPresent(code -> vmTrace.setCode(code.getBytes().getHexString()));
    final int nextFrameIndex = index.get() + 1;
    final Optional<TraceFrame> maybeNextFrame =
        transactionTrace.getTraceFrames().size() > nextFrameIndex
            ? Optional.of(transactionTrace.getTraceFrames().get(nextFrameIndex))
            : Optional.empty();
    final Op op = new Op();
    op.setCost(traceFrame.getGasCost().orElse(Gas.ZERO).toLong());
    op.setPc(traceFrame.getPc());
    final Ex ex = new Ex();
    ex.setUsed(
        traceFrame.getGasRemaining().toLong() - traceFrame.getGasCost().orElse(Gas.ZERO).toLong());
    if (traceFrame.isMemoryWritten()) {
      maybeNextFrame
          .flatMap(TraceFrame::getMemory)
          .ifPresent(
              memory -> {
                if (memory.length > 0) {
                  ex.setMem(new Mem(BytesValues.trimTrailingZeros(memory[0]).getHexString(), 0));
                }
              });
    }

    if ((traceFrame.getOpcode().startsWith("PUSH") || "CALLDATALOAD".equals(traceFrame.getOpcode()))
        && maybeNextFrame.isPresent()) {
      maybeNextFrame
          .get()
          .getStack()
          .map(stack -> stack[stack.length - 1])
          .map(BytesValues::trimLeadingZeros)
          .ifPresent(push -> ex.addPush(push.isZero() ? "0x0" : push.toShortHexString()));
    }

    if ("SSTORE".equals(traceFrame.getOpcode())) {
      handleSstore(traceFrame, ex);
    }

    op.setEx(ex);

    vmTrace.add(op);
    index.incrementAndGet();
  }

  private static void handleSstore(final TraceFrame traceFrame, final Ex ex) {
    ex.setStore(
        traceFrame
            .getStack()
            .map(stack -> new Store(stack[0].toShortHexString(), stack[1].toShortHexString()))
            .orElseThrow());
  }
}

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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.debug.TraceFrame;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTrace;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FlatTraceGenerator {

  public static Stream<Trace> generateFromTransactionTrace(
      final TransactionTrace trace, final AtomicInteger traceCounter) {
    final FlatTrace.Builder firstFlatTraceBuilder =
        FlatTrace.builder().resultBuilder(Result.builder());
    String lastContractAddress = trace.getTransaction().getTo().orElse(Address.ZERO).getHexString();
    final Action.Builder firstFlatTraceActionBuilder =
        Action.builder()
            .from(trace.getTransaction().getSender().getHexString())
            .gas(
                trace
                    .getTransaction()
                    .getUpfrontGasCost()
                    .minus(Wei.of(trace.getResult().getGasRemaining()))
                    .toShortHexString())
            .value(trace.getTransaction().getValue().toShortHexString());

    final Optional<String> smartContractCode =
        trace.getTransaction().getInit().isPresent()
            ? Optional.of(trace.getResult().getOutput().toString())
            : Optional.empty();
    final Optional<String> smartContractAddress =
        smartContractCode.isPresent()
            ? Optional.of(
                Address.contractAddress(
                        trace.getTransaction().getSender(), trace.getTransaction().getNonce())
                    .getHexString())
            : Optional.empty();
    // set code field in result node
    smartContractCode.ifPresent(firstFlatTraceBuilder.getResultBuilder().orElseThrow()::code);
    // set init field if transaction is a smart contract deployment
    trace
        .getTransaction()
        .getInit()
        .ifPresent(init -> firstFlatTraceActionBuilder.init(init.getHexString()));
    // set to, input and callType fields if not a smart contract
    trace
        .getTransaction()
        .getTo()
        .ifPresent(
            to ->
                firstFlatTraceActionBuilder
                    .to(to.toString())
                    .callType("call")
                    .input(
                        trace
                            .getTransaction()
                            .getData()
                            .orElse(trace.getTransaction().getInit().orElse(BytesValue.EMPTY))
                            .getHexString()));
    firstFlatTraceBuilder.actionBuilder(firstFlatTraceActionBuilder);
    // declare a queue of trace contexts
    final Deque<FlatTrace.Context> tracesContexts = new ArrayDeque<>();
    // add the first trace context to the queue of trace contexts
    tracesContexts.addLast(new FlatTrace.Context(firstFlatTraceBuilder));
    // declare the first trace context as the previous trace context
    // FlatTrace.Context previousTraceContext = tracesContexts.peekLast();
    final List<Integer> currentTraceAddressVector = new ArrayList<>();
    currentTraceAddressVector.add(traceCounter.get());
    final AtomicInteger subTracesCounter = new AtomicInteger(0);
    long cumulativeGasCost = 0;
    for (TraceFrame traceFrame : trace.getTraceFrames()) {
      cumulativeGasCost += traceFrame.getGasCost().orElse(Gas.ZERO).toLong();
      if ("CALL".equals(traceFrame.getOpcode())) {
        final Bytes32[] stack = traceFrame.getStack().orElseThrow();
        final Address contractCallAddress = toAddress(stack[stack.length - 2]);
        final Bytes32[] memory = traceFrame.getMemory().orElseThrow();
        final Bytes32 contractCallInput = memory[0];
        final FlatTrace.Builder subTraceBuilder =
            FlatTrace.builder()
                .traceAddress(currentTraceAddressVector.toArray(new Integer[0]))
                .resultBuilder(Result.builder());
        final Action.Builder subTraceActionBuilder =
            Action.builder()
                .from(lastContractAddress)
                .to(contractCallAddress.toString())
                .input(contractCallInput.getHexString())
                .gas(traceFrame.getGasRemaining().toHexString())
                .callType("call")
                .value(trace.getTransaction().getValue().toShortHexString());
        final long gasCost = cumulativeGasCost;
        // retrieve the previous trace context
        Optional.ofNullable(tracesContexts.peekLast())
            .ifPresent(
                previousContext -> {
                  // increment sub traces counter of previous trace
                  previousContext.getBuilder().incSubTraces();
                  // set gas cost of previous trace
                  previousContext
                      .getBuilder()
                      .getResultBuilder()
                      .orElse(Result.builder())
                      .gasUsed(Gas.of(gasCost).toHexString());
                });
        tracesContexts.addLast(
            new FlatTrace.Context(subTraceBuilder.action(subTraceActionBuilder.build())));
        // compute trace addresses
        IntStream.of(subTracesCounter.incrementAndGet()).forEach(currentTraceAddressVector::add);
        cumulativeGasCost = 0;
      }
      if ("RETURN".equals(traceFrame.getOpcode())) {
        final Deque<FlatTrace.Context> polledContexts = new ArrayDeque<>();
        FlatTrace.Context ctx;
        boolean continueToPollContexts = true;
        // find last non returned trace
        while (continueToPollContexts && (ctx = tracesContexts.pollLast()) != null) {
          polledContexts.addFirst(ctx);
          if (!ctx.isReturned()) {
            final FlatTrace.Builder flatTraceBuilder = ctx.getBuilder();
            final Result.Builder resultBuilder =
                flatTraceBuilder.getResultBuilder().orElse(Result.builder());
            final Bytes32[] memory = traceFrame.getMemory().orElseThrow();
            resultBuilder.gasUsed(Gas.of(cumulativeGasCost).toHexString());
            smartContractAddress.ifPresentOrElse(
                address -> {
                  resultBuilder.address(address);
                  flatTraceBuilder.type("create");
                },
                () -> resultBuilder.output(memory[0].toString()));
            ctx.markAsReturned();
            continueToPollContexts = false;
          }
        }
        // reinsert polled contexts add the end of the queue
        polledContexts.forEach(tracesContexts::addLast);
        cumulativeGasCost = 0;
      }
    }
    final List<Trace> flatTraces = new ArrayList<>();
    tracesContexts.forEach(context -> flatTraces.add(context.getBuilder().build()));
    traceCounter.incrementAndGet();
    return flatTraces.stream();
  }

  private static Address toAddress(final Bytes32 value) {
    return Address.wrap(
        BytesValue.of(
            Arrays.copyOfRange(value.extractArray(), Bytes32.SIZE - Address.SIZE, Bytes32.SIZE)));
  }
}

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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.flat;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.debug.TraceFrame;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTrace;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.Trace;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FlatTraceGenerator {

  /**
   * Generates a stream of {@link Trace} from the passed {@link TransactionTrace} data.
   *
   * @param transactionTrace the {@link TransactionTrace} to use
   * @param traceCounter the current trace counter value
   * @return a stream of generated traces {@link Trace}
   */
  public static Stream<Trace> generateFromTransactionTrace(
      final TransactionTrace transactionTrace, final AtomicInteger traceCounter) {
    final FlatTrace.Builder firstFlatTraceBuilder = FlatTrace.freshBuilder(transactionTrace);
    final String lastContractAddress =
        transactionTrace.getTransaction().getTo().orElse(Address.ZERO).getHexString();

    final Optional<String> smartContractCode =
        transactionTrace.getTransaction().getInit().isPresent()
            ? Optional.of(transactionTrace.getResult().getOutput().toString())
            : Optional.empty();
    final Optional<String> smartContractAddress =
        smartContractCode.isPresent()
            ? Optional.of(
                Address.contractAddress(
                        transactionTrace.getTransaction().getSender(),
                        transactionTrace.getTransaction().getNonce())
                    .getHexString())
            : Optional.empty();
    // set code field in result node
    smartContractCode.ifPresent(firstFlatTraceBuilder.getResultBuilder().orElseThrow()::code);
    // set init field if transaction is a smart contract deployment
    transactionTrace
        .getTransaction()
        .getInit()
        .ifPresent(
            init ->
                firstFlatTraceBuilder.getActionBuilder().orElseThrow().init(init.getHexString()));
    // set to, input and callType fields if not a smart contract
    transactionTrace
        .getTransaction()
        .getTo()
        .ifPresent(
            to ->
                firstFlatTraceBuilder
                    .getActionBuilder()
                    .orElseThrow()
                    .to(to.toString())
                    .callType("call")
                    .input(
                        transactionTrace
                            .getTransaction()
                            .getData()
                            .orElse(
                                transactionTrace
                                    .getTransaction()
                                    .getInit()
                                    .orElse(BytesValue.EMPTY))
                            .getHexString()));
    // declare a queue of transactionTrace contexts
    final Deque<FlatTrace.Context> tracesContexts = new ArrayDeque<>();
    // add the first transactionTrace context to the queue of transactionTrace contexts
    tracesContexts.addLast(new FlatTrace.Context(firstFlatTraceBuilder));
    // declare the first transactionTrace context as the previous transactionTrace context
    // FlatTrace.Context previousTraceContext = tracesContexts.peekLast();
    final List<Integer> addressVector = new ArrayList<>();
    addressVector.add(traceCounter.get());
    final AtomicInteger subTracesCounter = new AtomicInteger(0);
    final AtomicLong cumulativeGasCost = new AtomicLong(0);
    for (TraceFrame traceFrame : transactionTrace.getTraceFrames()) {
      cumulativeGasCost.addAndGet(traceFrame.getGasCost().orElse(Gas.ZERO).toLong());
      if ("CALL".equals(traceFrame.getOpcode())) {
        handleCall(
            transactionTrace.getTransaction(),
            traceFrame,
            lastContractAddress,
            cumulativeGasCost,
            addressVector,
            tracesContexts,
            subTracesCounter);
      } else if ("RETURN".equals(traceFrame.getOpcode()) || "STOP".equals(traceFrame.getOpcode())) {
        handleReturn(traceFrame, smartContractAddress, cumulativeGasCost, tracesContexts);
      } else if ("SELFDESTRUCT".equals(traceFrame.getOpcode())) {
        handleSelfDestruct(
            transactionTrace.getTransaction(),
            traceFrame,
            lastContractAddress,
            cumulativeGasCost,
            addressVector,
            tracesContexts,
            subTracesCounter);
      }
    }
    final List<Trace> flatTraces = new ArrayList<>();
    tracesContexts.forEach(context -> flatTraces.add(context.getBuilder().build()));
    traceCounter.incrementAndGet();
    return flatTraces.stream();
  }

  private static void handleCall(
      final Transaction transaction,
      final TraceFrame traceFrame,
      final String lastContractAddress,
      final AtomicLong cumulativeGasCost,
      final List<Integer> addressVector,
      final Deque<FlatTrace.Context> tracesContexts,
      final AtomicInteger subTracesCounter) {
    final Bytes32[] stack = traceFrame.getStack().orElseThrow();
    final Address contractCallAddress = toAddress(stack[stack.length - 2]);
    final FlatTrace.Builder subTraceBuilder =
        FlatTrace.builder()
            .traceAddress(addressVector.toArray(new Integer[0]))
            .resultBuilder(Result.builder());
    final Action.Builder subTraceActionBuilder =
        Action.createCallAction(transaction, lastContractAddress, contractCallAddress, traceFrame);

    final long gasCost = cumulativeGasCost.longValue();
    // retrieve the previous transactionTrace context
    Optional.ofNullable(tracesContexts.peekLast())
        .ifPresent(
            previousContext -> {
              // increment sub traces counter of previous transactionTrace
              previousContext.getBuilder().incSubTraces();
              // set gas cost of previous transactionTrace
              previousContext
                  .getBuilder()
                  .getResultBuilder()
                  .orElse(Result.builder())
                  .gasUsed(Gas.of(gasCost).toHexString());
            });
    tracesContexts.addLast(
        new FlatTrace.Context(subTraceBuilder.action(subTraceActionBuilder.build())));
    // compute transactionTrace addresses
    IntStream.of(subTracesCounter.incrementAndGet()).forEach(addressVector::add);
    cumulativeGasCost.set(0);
  }

  private static void handleReturn(
      final TraceFrame traceFrame,
      final Optional<String> smartContractAddress,
      final AtomicLong cumulativeGasCost,
      final Deque<FlatTrace.Context> tracesContexts) {
    final Deque<FlatTrace.Context> polledContexts = new ArrayDeque<>();
    FlatTrace.Context ctx;
    boolean continueToPollContexts = true;
    // find last non returned transactionTrace
    while (continueToPollContexts && (ctx = tracesContexts.pollLast()) != null) {
      polledContexts.addFirst(ctx);
      if (!ctx.isReturned()) {
        final FlatTrace.Builder flatTraceBuilder = ctx.getBuilder();
        final Result.Builder resultBuilder =
            flatTraceBuilder.getResultBuilder().orElse(Result.builder());
        resultBuilder.gasUsed(Gas.of(cumulativeGasCost.longValue()).toHexString());
        // set address and type to create if smart contract deployment
        smartContractAddress.ifPresentOrElse(
            address -> {
              resultBuilder.address(address);
              flatTraceBuilder.type("create");
            },
            // set output otherwise
            () ->
                resultBuilder.output(
                    traceFrame.getMemory().isPresent() && traceFrame.getMemory().get().length > 0
                        ? traceFrame.getMemory().get()[0].toString()
                        : "0x"));
        ctx.markAsReturned();
        continueToPollContexts = false;
      }
    }
    // reinsert polled contexts add the end of the queue
    polledContexts.forEach(tracesContexts::addLast);
    tracesContexts
        .getFirst()
        .getBuilder()
        .getActionBuilder()
        .ifPresent(actionBuilder -> actionBuilder.incrementGas(cumulativeGasCost.longValue()));
    cumulativeGasCost.set(0);
  }

  private static void handleSelfDestruct(
      final Transaction transaction,
      final TraceFrame traceFrame,
      final String lastContractAddress,
      final AtomicLong cumulativeGasCost,
      final List<Integer> addressVector,
      final Deque<FlatTrace.Context> tracesContexts,
      final AtomicInteger subTracesCounter) {
    final Bytes32[] stack = traceFrame.getStack().orElseThrow();
    final Address refundAddress = toAddress(stack[0]);
    final FlatTrace.Builder subTraceBuilder =
        FlatTrace.builder().type("suicide").traceAddress(addressVector.toArray(new Integer[0]));
    final Action.Builder subTraceActionBuilder =
        Action.createSelfDestructAction(
            transaction, lastContractAddress, refundAddress, traceFrame);

    final long gasCost = cumulativeGasCost.longValue();
    // retrieve the previous transactionTrace context
    Optional.ofNullable(tracesContexts.peekLast())
        .ifPresent(
            previousContext -> {
              // increment sub traces counter of previous transactionTrace
              previousContext.getBuilder().incSubTraces();
              // set gas cost of previous transactionTrace
              previousContext
                  .getBuilder()
                  .getResultBuilder()
                  .orElse(Result.builder())
                  .gasUsed(Gas.of(gasCost).toHexString());
            });
    tracesContexts.addLast(
        new FlatTrace.Context(subTraceBuilder.action(subTraceActionBuilder.build())));
    // compute transactionTrace addresses
    IntStream.of(subTracesCounter.incrementAndGet()).forEach(addressVector::add);
    cumulativeGasCost.set(0);
  }

  private static Address toAddress(final Bytes32 value) {
    return Address.wrap(
        BytesValue.of(
            Arrays.copyOfRange(value.extractArray(), Bytes32.SIZE - Address.SIZE, Bytes32.SIZE)));
  }
}

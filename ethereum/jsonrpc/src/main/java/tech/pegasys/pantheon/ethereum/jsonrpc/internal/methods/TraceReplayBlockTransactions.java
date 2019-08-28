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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.debug.TraceFrame;
import tech.pegasys.pantheon.ethereum.debug.TraceOptions;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.BlockParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.TraceTypeParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.BlockTrace;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.BlockTracer;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTrace;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.Action;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.FlatTrace;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.Result;
import tech.pegasys.pantheon.ethereum.vm.DebugOperationTracer;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TraceReplayBlockTransactions extends AbstractBlockParameterMethod {
  private static final Logger LOG = LogManager.getLogger();

  private final BlockTracer blockTracer;

  public TraceReplayBlockTransactions(
      final JsonRpcParameter parameters,
      final BlockTracer blockTracer,
      final BlockchainQueries queries) {
    super(queries, parameters);
    this.blockTracer = blockTracer;
  }

  @Override
  public String getName() {
    return RpcMethod.TRACE_REPLAY_BLOCK_TRANSACTIONS.getMethodName();
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequest request) {
    return getParameters().required(request.getParams(), 0, BlockParameter.class);
  }

  @Override
  protected Object resultByBlockNumber(final JsonRpcRequest request, final long blockNumber) {
    final TraceTypeParameter traceTypeParameter =
        getParameters().required(request.getParams(), 1, TraceTypeParameter.class);

    // TODO : method returns an error if any option other than “trace” is supplied.
    // remove when others options are implemented
    if (traceTypeParameter.getTraceTypes().contains(TraceTypeParameter.TraceType.STATE_DIFF)
        || traceTypeParameter.getTraceTypes().contains(TraceTypeParameter.TraceType.VM_TRACE)) {
      LOG.warn("Unsupported trace option");
      throw new InvalidJsonRpcParameters("Invalid trace types supplied.");
    }

    if (blockNumber == BlockHeader.GENESIS_BLOCK_NUMBER) {
      // Nothing to trace for the genesis block
      return emptyResult();
    }

    return getBlockchainQueries()
        .getBlockchain()
        .getBlockByNumber(blockNumber)
        .map((block) -> traceBlock(block, traceTypeParameter))
        .orElse(null);
  }

  private Object traceBlock(final Block block, final TraceTypeParameter traceTypeParameter) {
    // TODO: generate options based on traceTypeParameter
    final TraceOptions traceOptions = TraceOptions.DEFAULT;

    return blockTracer
        .trace(block, new DebugOperationTracer(traceOptions))
        .map(BlockTrace::getTransactionTraces)
        .map((traces) -> formatTraces(traces, traceTypeParameter))
        .orElse(null);
  }

  private JsonNode formatTraces(
      final List<TransactionTrace> traces, final TraceTypeParameter traceTypeParameter) {
    final Set<TraceTypeParameter.TraceType> traceTypes = traceTypeParameter.getTraceTypes();
    final ObjectMapper mapper = new ObjectMapper();
    final ArrayNode resultArrayNode = mapper.createArrayNode();
    final ObjectNode resultNode = mapper.createObjectNode();
    final AtomicInteger traceCounter = new AtomicInteger(0);
    traces.stream()
        .findFirst()
        .ifPresent(
            transactionTrace -> {
              resultNode.put(
                  "transactionHash", transactionTrace.getTransaction().hash().getHexString());
              resultNode.put("output", transactionTrace.getResult().getOutput().toString());
            });
    resultNode.put("stateDiff", (String) null);
    resultNode.put("vmTrace", (String) null);

    if (traceTypes.contains(TraceTypeParameter.TraceType.TRACE)) {
      final ArrayNode tracesNode = resultNode.putArray("trace");
      traces.forEach((trace) -> formatWithTraceOption(trace, traceCounter, mapper, tracesNode));
    }

    resultArrayNode.add(resultNode);
    return resultArrayNode;
  }

  @SuppressWarnings("unused")
  private void formatWithTraceOption(
      final TransactionTrace trace,
      final AtomicInteger traceCounter,
      final ObjectMapper mapper,
      final ArrayNode tracesNode) {
    final FlatTrace.Builder firstFlatTraceBuilder =
        FlatTrace.builder().resultBuilder(Result.builder());
    String lastContractAddress = trace.getTransaction().getTo().orElse(Address.ZERO).getHexString();
    final Action.Builder firstFlatTraceActionBuilder =
        Action.builder()
            .from(trace.getTransaction().getSender().getHexString())
            .to(trace.getTransaction().getTo().orElse(Address.ZERO).toString())
            .input(
                trace
                    .getTransaction()
                    .getData()
                    .orElse(trace.getTransaction().getInit().orElse(BytesValue.EMPTY))
                    .getHexString())
            .gas(
                trace
                    .getTransaction()
                    .getUpfrontGasCost()
                    .minus(Wei.of(trace.getResult().getGasRemaining()))
                    .toShortHexString())
            .callType("call")
            .value(trace.getTransaction().getValue().toShortHexString());
    firstFlatTraceBuilder.actionBuilder(firstFlatTraceActionBuilder);
    final Deque<FlatTrace.Context> tracesContexts = new ArrayDeque<>();
    tracesContexts.addLast(new FlatTrace.Context(firstFlatTraceBuilder));
    FlatTrace.Builder previousTraceBuilder = firstFlatTraceBuilder;
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
        tracesContexts.addLast(
            new FlatTrace.Context(subTraceBuilder.action(subTraceActionBuilder.build())));
        previousTraceBuilder.incSubTraces();
        previousTraceBuilder
            .getResultBuilder()
            .orElse(Result.builder())
            .gasUsed(Gas.of(cumulativeGasCost).toHexString());
        previousTraceBuilder = subTraceBuilder;
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
            final Bytes32[] memory = traceFrame.getMemory().orElseThrow();
            ctx.getBuilder()
                .getResultBuilder()
                .orElse(Result.builder())
                .gasUsed(Gas.of(cumulativeGasCost).toHexString())
                .output(memory[0].toString());
            previousTraceBuilder = ctx.getBuilder();
            ctx.markAsReturned();
            continueToPollContexts = false;
          }
        }
        // reinsert polled contexts add the end of the queue
        polledContexts.forEach(tracesContexts::addLast);
        cumulativeGasCost = 0;
      }
    }
    tracesContexts.forEach(context -> tracesNode.addPOJO(context.getBuilder().build()));
    traceCounter.incrementAndGet();
  }

  private Object emptyResult() {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.createArrayNode();
  }

  private static Address toAddress(final Bytes32 value) {
    return Address.wrap(
        BytesValue.of(
            Arrays.copyOfRange(value.extractArray(), Bytes32.SIZE - Address.SIZE, Bytes32.SIZE)));
  }
}

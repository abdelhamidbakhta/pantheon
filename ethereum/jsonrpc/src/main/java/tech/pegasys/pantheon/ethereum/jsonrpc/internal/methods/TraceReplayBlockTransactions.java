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

import java.util.ArrayList;
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
    final ObjectNode currentTraceNode = mapper.createObjectNode();
    final FlatTrace.Builder firstFlatTraceBuilder = FlatTrace.builder();
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
            .gas(trace.getTransaction().getUpfrontGasCost().toShortHexString())
            .callType("call")
            .value(trace.getTransaction().getValue().toShortHexString());
    final Result.Builder firstFlatTraceResultBuilder = Result.builder();
    final List<FlatTrace.Builder> subTracesBuilders = new ArrayList<>();
    FlatTrace.Builder previousTraceBuilder = firstFlatTraceBuilder;
    final List<Integer> currentTraceAddressVector = new ArrayList<>();
    currentTraceAddressVector.add(traceCounter.get());
    final AtomicInteger subTracesCounter = new AtomicInteger(0);
    for (TraceFrame traceFrame : trace.getTraceFrames()) {
      if ("CALL".equals(traceFrame.getOpcode())) {
        final Bytes32[] stack = traceFrame.getStack().orElseThrow();
        final Bytes32 contractCallAddress = stack[stack.length - 2];
        LOG.info("Call detected to contract: {}", contractCallAddress.toString());
        final Bytes32[] memory = traceFrame.getMemory().orElseThrow();
        final Bytes32 contractCallInput = memory[0];
        final FlatTrace.Builder subTraceBuilder =
            FlatTrace.builder().traceAddress(currentTraceAddressVector.toArray(new Integer[0]));
        final Action.Builder subTraceActionBuilder =
            Action.builder()
                .from(lastContractAddress)
                .to(contractCallAddress.toString())
                .input(contractCallInput.getHexString())
                .callType("call")
                .value(trace.getTransaction().getValue().toShortHexString());
        subTracesBuilders.add(subTraceBuilder.action(subTraceActionBuilder.build()));
        previousTraceBuilder.incSubTraces();
        previousTraceBuilder = subTraceBuilder;
        IntStream.of(subTracesCounter.incrementAndGet())
            .forEach(value -> currentTraceAddressVector.add(value));
      }
      if ("RETURN".equals(traceFrame.getOpcode())) {}
    }
    tracesNode.addPOJO(
        firstFlatTraceBuilder
            .action(firstFlatTraceActionBuilder.build())
            .result(firstFlatTraceResultBuilder.build())
            .build());
    subTracesBuilders.forEach(flatTraceBuilder -> tracesNode.addPOJO(flatTraceBuilder.build()));
    traceCounter.incrementAndGet();
  }

  private void generateActionNode(final ObjectNode traceNode, final TransactionTrace trace) {}

  /*
  private void generateResultNode(final ObjectNode traceNode, final TransactionTrace trace) {
    final ObjectNode traceResultNode = traceNode.putObject("result");
    traceResultNode.put("output", "0x");
    traceResultNode.put("gasUsed", Gas.of(trace.getGas()).toHexString());
  }

  private void generateActionNode(final ObjectNode traceNode, final TransactionTrace trace) {
    final ObjectNode actionResultNode = traceNode.putObject("action");
    actionResultNode.put("callType", "call");
    actionResultNode.put("from", trace.getTransaction().getSender().toString());
    actionResultNode.put(
        "gas", trace.getTransaction().getUpfrontGasCost().toStrictShortHexString());
    actionResultNode.put(
        "input",
        trace
            .getTransaction()
            .getData()
            .orElse(trace.getTransaction().getInit().orElse(BytesValue.EMPTY))
            .toString());
    trace
        .getTransaction()
        .getTo()
        .ifPresent(address -> actionResultNode.put("to", address.toString()));
    actionResultNode.put("value", trace.getTransaction().getValue().toStrictShortHexString());
  }*/

  private Object emptyResult() {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.createArrayNode();
  }
}

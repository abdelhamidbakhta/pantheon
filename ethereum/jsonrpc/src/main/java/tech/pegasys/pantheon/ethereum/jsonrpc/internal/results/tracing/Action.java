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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.debug.TraceFrame;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTrace;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(NON_NULL)
public class Action {

  private String callType;
  private String from;
  private String gas;
  private String input;
  private String to;
  private String init;
  private String value;
  private String address;
  private String balance;
  private String refundAddress;

  public static Builder builder() {
    return new Builder();
  }

  public static Builder createCallAction(
      final Transaction transaction,
      final String lastContractAddress,
      final Address contractCallAddress,
      final TraceFrame traceFrame) {
    return builder()
        .from(lastContractAddress)
        .to(contractCallAddress.toString())
        .input(traceFrame.getMemory().orElseThrow()[0].getHexString())
        .gas(traceFrame.getGasRemaining().toHexString())
        .callType("call")
        .value(transaction.getValue().toShortHexString());
  }

  public static Builder createSelfDestructAction(
      final Transaction transaction,
      final String lastContractAddress,
      final Address contractCallAddress,
      final TraceFrame traceFrame) {
    return builder().address(lastContractAddress).refundAddress(contractCallAddress.toString());
  }

  public String getCallType() {
    return callType;
  }

  public void setCallType(final String callType) {
    this.callType = callType;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(final String from) {
    this.from = from;
  }

  public String getGas() {
    return gas;
  }

  public void setGas(final String gas) {
    this.gas = gas;
  }

  public String getInput() {
    return input;
  }

  public void setInput(final String input) {
    this.input = input;
  }

  public String getTo() {
    return to;
  }

  public void setTo(final String to) {
    this.to = to;
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

  public String getInit() {
    return init;
  }

  public void setInit(final String init) {
    this.init = init;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(final String address) {
    this.address = address;
  }

  public String getBalance() {
    return balance;
  }

  public void setBalance(final String balance) {
    this.balance = balance;
  }

  public String getRefundAddress() {
    return refundAddress;
  }

  public void setRefundAddress(final String refundAddress) {
    this.refundAddress = refundAddress;
  }

  public static final class Builder {
    private String callType;
    private String from;
    private String gas;
    private String input;
    private String to;
    private String init;
    private String value;
    private String address;
    private String balance;
    private String refundAddress;

    private Builder() {}

    public static Builder of(final Action action) {
      final Builder builder = new Builder();
      builder.callType = action.callType;
      builder.from = action.from;
      builder.gas = action.gas;
      builder.input = action.input;
      builder.to = action.to;
      builder.init = action.init;
      builder.value = action.value;
      builder.address = action.address;
      builder.refundAddress = action.refundAddress;
      builder.balance = action.balance;
      return builder;
    }

    public static Builder from(final TransactionTrace trace) {
      return new Builder()
          .from(trace.getTransaction().getSender().getHexString())
          .gas(Wei.of(trace.getResult().getGasRemaining()).toStrictShortHexString())
          .value(trace.getTransaction().getValue().toShortHexString());
    }

    public Builder incrementGas(final long value) {
      this.gas = Gas.fromHexString(gas).plus(Gas.of(value)).toHexString();
      return this;
    }

    public Builder callType(final String callType) {
      this.callType = callType;
      return this;
    }

    public Builder from(final String from) {
      this.from = from;
      return this;
    }

    public Builder gas(final String gas) {
      this.gas = gas;
      return this;
    }

    public Builder input(final String input) {
      this.input = input;
      return this;
    }

    public Builder to(final String to) {
      this.to = to;
      return this;
    }

    public Builder init(final String init) {
      this.init = init;
      return this;
    }

    public Builder value(final String value) {
      this.value = value;
      return this;
    }

    public Builder address(final String address) {
      this.address = address;
      return this;
    }

    public Builder balance(final String balance) {
      this.balance = balance;
      return this;
    }

    public Builder refundAddress(final String refundAddress) {
      this.refundAddress = refundAddress;
      return this;
    }

    public Action build() {
      final Action action = new Action();
      action.setCallType(callType);
      action.setFrom(from);
      action.setGas(gas);
      action.setInput(input);
      action.setTo(to);
      action.setInit(init);
      action.setValue(value);
      action.setAddress(address);
      action.setRefundAddress(refundAddress);
      action.setBalance(balance);
      return action;
    }
  }
}

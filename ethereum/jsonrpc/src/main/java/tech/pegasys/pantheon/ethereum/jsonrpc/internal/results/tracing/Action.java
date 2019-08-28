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

public class Action {

  private String callType;
  private String from;
  private String gas;
  private String input;
  private String to;
  private String value;

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

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String callType = "call";
    private String from;
    private String gas;
    private String input;
    private String to;
    private String value;

    private Builder() {}

    public static Builder of(final Action action) {
      final Builder builder = new Builder();
      builder.callType = action.callType;
      builder.from = action.from;
      builder.gas = action.gas;
      builder.input = action.input;
      builder.to = action.to;
      builder.value = action.value;
      return builder;
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

    public Builder value(final String value) {
      this.value = value;
      return this;
    }

    public Action build() {
      final Action action = new Action();
      action.setCallType(callType);
      action.setFrom(from);
      action.setGas(gas);
      action.setInput(input);
      action.setTo(to);
      action.setValue(value);
      return action;
    }
  }
}

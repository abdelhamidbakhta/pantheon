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

public class FlatTrace {
  private Action action;
  private Result result;
  private int subtraces;
  private Integer[] traceAddress = new Integer[0];
  private String type;

  public Action getAction() {
    return action;
  }

  public void setAction(final Action action) {
    this.action = action;
  }

  public Result getResult() {
    return result;
  }

  public void setResult(final Result result) {
    this.result = result;
  }

  public int getSubtraces() {
    return subtraces;
  }

  public void setSubtraces(final int subtraces) {
    this.subtraces = subtraces;
  }

  public Integer[] getTraceAddress() {
    return traceAddress;
  }

  public void setTraceAddress(final Integer[] traceAddress) {
    this.traceAddress = traceAddress;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Action action;
    private Result result;
    private int subtraces;
    private Integer[] traceAddress = new Integer[0];
    private String type = "call";

    private Builder() {}

    public Builder action(final Action action) {
      this.action = action;
      return this;
    }

    public Builder result(final Result result) {
      this.result = result;
      return this;
    }

    public Builder subtraces(int subtraces) {
      this.subtraces = subtraces;
      return this;
    }

    public Builder traceAddress(Integer[] traceAddress) {
      this.traceAddress = traceAddress;
      return this;
    }

    public Builder type(final String type) {
      this.type = type;
      return this;
    }

    public Builder incSubTraces() {
      return incSubTraces(1);
    }

    public Builder incSubTraces(final int n) {
      this.subtraces += n;
      return this;
    }

    public FlatTrace build() {
      FlatTrace flatTrace = new FlatTrace();
      flatTrace.setAction(action);
      flatTrace.setResult(result);
      flatTrace.setSubtraces(subtraces);
      flatTrace.setTraceAddress(traceAddress);
      flatTrace.setType(type);
      return flatTrace;
    }
  }
}

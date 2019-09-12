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

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VmTrace implements Trace {

  private String code;
  private final List<Op> ops;

  public VmTrace() {
    this("0x");
  }

  public VmTrace(final String code) {
    this(code, new ArrayList<>());
  }

  private VmTrace(final String code, final List<Op> ops) {
    this.code = code;
    this.ops = ops;
  }

  public void add(final Op op) {
    ops.add(op);
  }

  public String getCode() {
    return code;
  }

  public void setCode(final String code) {
    this.code = code;
  }

  public List<Op> getOps() {
    return ops;
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, ops);
  }
}

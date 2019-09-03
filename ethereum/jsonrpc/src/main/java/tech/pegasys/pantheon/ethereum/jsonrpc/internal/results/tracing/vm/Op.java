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

public class Op {
  private long cost;
  private Ex ex;
  private long pc;
  private String sub;

  public Op() {}

  public Op(final long cost, final Ex ex, final long pc, final String sub) {
    this.cost = cost;
    this.ex = ex;
    this.pc = pc;
    this.sub = sub;
  }

  public long getCost() {
    return cost;
  }

  public void setCost(final long cost) {
    this.cost = cost;
  }

  public Ex getEx() {
    return ex;
  }

  public void setEx(final Ex ex) {
    this.ex = ex;
  }

  public long getPc() {
    return pc;
  }

  public void setPc(final long pc) {
    this.pc = pc;
  }

  public String getSub() {
    return sub;
  }

  public void setSub(final String sub) {
    this.sub = sub;
  }
}

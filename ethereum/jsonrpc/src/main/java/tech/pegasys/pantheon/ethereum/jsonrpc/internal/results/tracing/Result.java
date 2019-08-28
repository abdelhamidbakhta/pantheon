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

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(NON_NULL)
public class Result {
  private String gasUsed;
  private String output;
  private String code;
  private String address;

  public String getGasUsed() {
    return gasUsed;
  }

  public void setGasUsed(final String gasUsed) {
    this.gasUsed = gasUsed;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(final String output) {
    this.output = output;
  }

  public String getCode() {
    return code;
  }

  public void setCode(final String code) {
    this.code = code;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(final String address) {
    this.address = address;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String gasUsed;
    private String output;
    private String code;
    private String address;

    private Builder() {}

    public Builder gasUsed(final String gasUsed) {
      this.gasUsed = gasUsed;
      return this;
    }

    public Builder output(final String output) {
      this.output = output;
      return this;
    }

    public Builder code(final String code) {
      this.code = code;
      return this;
    }

    public Builder address(final String address) {
      this.address = address;
      return this;
    }

    public static Builder of(final Result result) {
      final Builder builder = new Builder();
      builder.output = result.output;
      builder.gasUsed = result.gasUsed;
      builder.code = result.code;
      builder.address = result.address;
      return builder;
    }

    public Result build() {
      Result result = new Result();
      result.setGasUsed(gasUsed);
      result.setOutput(output);
      result.setCode(code);
      result.setAddress(address);
      return result;
    }
  }
}

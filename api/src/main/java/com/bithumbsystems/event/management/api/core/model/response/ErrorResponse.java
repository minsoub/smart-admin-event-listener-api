package com.bithumbsystems.event.management.api.core.model.response;


import com.bithumbsystems.event.management.api.core.model.enums.ReturnCode;
import com.bithumbsystems.event.management.api.core.exception.ErrorData;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse<T> {

  private final ReturnCode result;
  private final ErrorData error;

  private final Object data;

  public ErrorResponse(ErrorData error) {
    this.result = ReturnCode.FAIL;
    this.error = error;
    this.data = null;
  }
}